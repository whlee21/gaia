package gaia.update;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.CommitUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;

class DistributedUpdateProcessor extends UpdateRequestProcessor {
	static Executor commExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS,
			new SynchronousQueue<Runnable>());

	static ThreadSafeClientConnManager mgr;
	static HttpClient client;// = new DefaultHttpClient(mgr);
	CompletionService<Request> completionService;
	Set<Future<Request>> pending;
	private final SolrQueryRequest req;
	private final SolrQueryResponse rsp;
	private final UpdateRequestProcessor next;
	private final DistributedUpdateProcessorFactory factory;
	private final SchemaField idField;
	private List<String> shards;
	private final List<AddUpdateCommand>[] adds;
	private final List<DeleteUpdateCommand>[] deletes;
	String selfStr;
	int self;
	int maxBufferedAddsPerServer = 10;
	int maxBufferedDeletesPerServer = 100;

	public DistributedUpdateProcessor(String shardStr, SolrQueryRequest req, SolrQueryResponse rsp,
			DistributedUpdateProcessorFactory factory, UpdateRequestProcessor next) {
		super(next);
		this.req = req;
		this.rsp = rsp;
		this.next = next;
		this.factory = factory;
		this.idField = req.getSchema().getUniqueKeyField();

		this.shards = factory.shards;

		String selfStr = req.getParams().get("self", factory.selfStr);
		if (shardStr != null) {
			shards = StrUtils.splitSmart(shardStr, ",", true);
		}

		self = -1;
		if (shards != null) {
			for (int i = 0; i < shards.size(); i++) {
				if (((String) shards.get(i)).equals(selfStr)) {
					self = i;
					break;
				}
			}
		}

		if (shards == null) {
			shards = new ArrayList<String>(1);
			shards.add("self");
			self = 0;
		}

		adds = new List[shards.size()];
		deletes = new List[shards.size()];
	}

	private int getSlot(String id) {
		return (id.hashCode() >>> 1) % shards.size();
	}

	public void processAdd(AddUpdateCommand cmd) throws IOException {
		checkResponses(false);

		SolrInputDocument doc = cmd.getSolrInputDocument();
		SolrInputField field = doc.getField(idField.getName());
		if (field == null) {
			if (next != null)
				next.processAdd(cmd);
			return;
		}
		String id = field.getFirstValue().toString();
		int slot = getSlot(id);
		if (slot == self) {
			if (next != null)
				next.processAdd(cmd);
			return;
		}

		flushDeletes(slot, 1, null);

		AddUpdateCommand clone = new AddUpdateCommand(req);

		clone.solrDoc = cmd.solrDoc;
		clone.commitWithin = cmd.commitWithin;
		clone.overwrite = cmd.overwrite;

		List<AddUpdateCommand> alist = adds[slot];
		if (alist == null) {
			alist = new ArrayList<AddUpdateCommand>(2);
			adds[slot] = alist;
		}
		alist.add(clone);

		flushAdds(slot, maxBufferedAddsPerServer, null);
	}

	private DeleteUpdateCommand clone(DeleteUpdateCommand cmd) {
		DeleteUpdateCommand c = new DeleteUpdateCommand(req);
		c.id = cmd.id;
		c.query = cmd.query;
		return c;
	}

	private void doDelete(int slot, DeleteUpdateCommand cmd) throws IOException {
		if (slot == self) {
			if (self >= 0)
				next.processDelete(cmd);
			return;
		}

		flushAdds(slot, 1, null);

		List<DeleteUpdateCommand> dlist = deletes[slot];
		if (dlist == null) {
			dlist = new ArrayList<DeleteUpdateCommand>(2);
			deletes[slot] = dlist;
		}
		dlist.add(clone(cmd));

		flushDeletes(slot, maxBufferedDeletesPerServer, null);
	}

	public void processDelete(DeleteUpdateCommand cmd) throws IOException {
		checkResponses(false);

		if (cmd.id != null) {
			doDelete(getSlot(cmd.id), cmd);
		} else if (cmd.query != null) {
			for (int slot = 0; slot < deletes.length; slot++) {
				if (slot != self)
					doDelete(slot, cmd);
			}
			doDelete(self, cmd);
		}
	}

	public void processCommit(CommitUpdateCommand cmd) throws IOException {
		checkResponses(true);

		for (int slot = 0; slot < shards.size(); slot++)
			if (slot != self) {
				if ((!flushAdds(slot, 1, cmd)) && (!flushDeletes(slot, 1, cmd))) {
					GaiaUpdateRequest ureq = new GaiaUpdateRequest();
					addCommit(ureq, cmd);
					submit(slot, ureq);
				}
			}
		if ((next != null) && (self >= 0))
			next.processCommit(cmd);

		if (cmd.waitSearcher)
			checkResponses(true);
	}

	public void finish() throws IOException {
		for (int slot = 0; slot < shards.size(); slot++)
			if (slot != self) {
				flushAdds(slot, 1, null);
				flushDeletes(slot, 1, null);
			}
		checkResponses(true);
		if ((next != null) && (self >= 0))
			next.finish();
	}

	void checkResponses(boolean block) {
		while ((pending != null) && (pending.size() > 0))
			try {
				Future<Request> future = block ? completionService.take() : completionService.poll();
				if (future == null)
					return;
				pending.remove(future);
				try {
					Request sreq = (Request) future.get();
					if (sreq.rspCode != 0) {
						if (rsp.getException() == null) {
							Exception e = sreq.exception;
							String newMsg = "shard update error (" + sreq.shard + "):" + e.getMessage();
							if ((e instanceof SolrException)) {
								SolrException se = (SolrException) e;
								e = new SolrException(SolrException.ErrorCode.getErrorCode(se.code()), newMsg, se.getCause());
							} else {
								e = new SolrException(SolrException.ErrorCode.SERVER_ERROR, "newMsg", e);
							}
							rsp.setException(e);
						}

						SolrException.log(SolrCore.log, "shard update error (" + sreq.shard + ")", sreq.exception);
					}
				} catch (ExecutionException e) {
					SolrException.log(SolrCore.log, "error sending update request to shard", e);
				}
			} catch (InterruptedException e) {
				throw new SolrException(SolrException.ErrorCode.SERVICE_UNAVAILABLE,
						"interrupted waiting for shard update response", e);
			}
	}

	void addCommit(GaiaUpdateRequest ureq, CommitUpdateCommand cmd) {
		if (cmd == null)
			return;

		ureq.setAction(cmd.optimize ? AbstractUpdateRequest.ACTION.OPTIMIZE : AbstractUpdateRequest.ACTION.COMMIT, false,
				cmd.waitSearcher);
	}

	boolean flushAdds(int slot, int limit, CommitUpdateCommand ccmd) {
		List<AddUpdateCommand> alist = adds[slot];
		if ((alist == null) || (alist.size() < limit))
			return false;

		GaiaUpdateRequest ureq = new GaiaUpdateRequest();
		addCommit(ureq, ccmd);

		for (AddUpdateCommand cmd : alist) {
			ureq.add(cmd.solrDoc, cmd.commitWithin, cmd.overwrite);
		}

		adds[slot] = null;
		submit(slot, ureq);
		return true;
	}

	boolean flushDeletes(int slot, int limit, CommitUpdateCommand ccmd) {
		List<DeleteUpdateCommand> dlist = deletes[slot];
		if ((dlist == null) || (dlist.size() < limit))
			return false;

		GaiaUpdateRequest ureq = new GaiaUpdateRequest();
		addCommit(ureq, ccmd);
		for (DeleteUpdateCommand cmd : dlist) {
			if (cmd.id != null) {
				ureq.deleteById(cmd.id);
			}
			if (cmd.query != null) {
				ureq.deleteByQuery(cmd.query);
			}
		}

		deletes[slot] = null;
		submit(slot, ureq);
		return true;
	}

	void submit(int slot, GaiaUpdateRequest ureq) {
		Request sreq = new Request();
		sreq.shard = ((String) shards.get(slot));
		sreq.ureq = ureq;
		submit(sreq);
	}

	void submit(final Request sreq) {
		if (completionService == null) {
			completionService = new ExecutorCompletionService<Request>(commExecutor);
			pending = new HashSet<Future<Request>>();
		}

		Callable<Request> task = new Callable<Request>() {
			public DistributedUpdateProcessor.Request call() throws Exception {
				try {
					String url = "http://" + sreq.shard;
					SolrServer server = new HttpSolrServer(url, DistributedUpdateProcessor.client);
					sreq.ursp = server.request(sreq.ureq);
				} catch (Exception e) {
					sreq.exception = e;
					if ((e instanceof SolrException))
						sreq.rspCode = ((SolrException) e).code();
					else {
						sreq.rspCode = -1;
					}
				}
				return sreq;
			}
		};
		pending.add(completionService.submit(task));
	}

	static {
		mgr = new ThreadSafeClientConnManager();
		mgr.setMaxTotal(200);
		mgr.setDefaultMaxPerRoute(8);
		client = new DefaultHttpClient(mgr);
	}

	static class Request {
		String shard;
		GaiaUpdateRequest ureq;
		NamedList<Object> ursp;
		int rspCode;
		Exception exception;
	}
}
