package gaia.solr.click;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.store.Directory;
import org.apache.solr.core.IndexDeletionPolicyWrapper;
import org.apache.solr.core.SolrDeletionPolicy;

public class ClickDeletionPolicy extends SolrDeletionPolicy {
	static Class<?> commitWrapper;
	static Field delegateField;

	public void onInit(List commits) throws IOException {
		maybeModify(commits);
		super.onInit(commits);
	}

	public void onCommit(List commits) throws IOException {
		maybeModify(commits);
		super.onCommit(commits);
	}

	private void maybeModify(List commits) throws IOException {
		if (commits.size() > 0) {
			IndexCommit c = (IndexCommit) commits.get(commits.size() - 1);
			Directory d = c.getDirectory();
			String[] names = d.listAll();
			List<String> data = null;
			for (String s : names) {
				if (BoostDataFileFilter.INSTANCE.accept(null, s)) {
					if (data == null)
						data = new ArrayList<String>();
					data.add(s);
				}
			}
			if (data != null) {
				if (c.getClass().isAssignableFrom(commitWrapper))
					try {
						IndexCommit orig = (IndexCommit) delegateField.get(c);
						delegateField.set(c, new CommitWrapper(orig, data));
					} catch (Exception e) {
						e.printStackTrace();
					}
				else {
					c = new CommitWrapper(c, data);
				}
				commits.set(commits.size() - 1, c);
			}
		}
	}

	static {
		Class<?>[] classes = IndexDeletionPolicyWrapper.class.getDeclaredClasses();
		for (Class<?> c : classes)
			if (c.getSimpleName().equals("IndexCommitWrapper")) {
				commitWrapper = c;
				break;
			}
		try {
			delegateField = commitWrapper.getDeclaredField("delegate");
			delegateField.setAccessible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static class CommitWrapper extends IndexCommit {
		private IndexCommit in;
		private List<String> names;

		CommitWrapper(IndexCommit in, List<String> names) throws IOException {
			this.in = in;
			this.names = new ArrayList<String>(in.getFileNames());
			this.names.addAll(names);
		}

		public void delete() {
			in.delete();
		}

		public Directory getDirectory() {
			return in.getDirectory();
		}

		public Collection<String> getFileNames() throws IOException {
			return names;
		}

		public long getGeneration() {
			return in.getGeneration();
		}

		public String getSegmentsFileName() {
			return in.getSegmentsFileName();
		}

		public Map<String, String> getUserData() throws IOException {
			return in.getUserData();
		}

		public boolean isDeleted() {
			return in.isDeleted();
		}

		public int getSegmentCount() {
			return in.getSegmentCount();
		}
	}
}
