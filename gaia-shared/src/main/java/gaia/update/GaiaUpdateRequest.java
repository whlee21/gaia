package gaia.update;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.XML;

public class GaiaUpdateRequest extends AbstractUpdateRequest {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6427320289170746687L;
	private List<SolrDoc> documents = null;
	private List<String> deleteById = null;
	private List<String> deleteQuery = null;

	public GaiaUpdateRequest() {
		super(SolrRequest.METHOD.POST, "/update");
	}

	public GaiaUpdateRequest(String url) {
		super(SolrRequest.METHOD.POST, url);
	}

	public void clear() {
		if (documents != null) {
			documents.clear();
		}
		if (deleteById != null) {
			deleteById.clear();
		}
		if (deleteQuery != null)
			deleteQuery.clear();
	}

	public GaiaUpdateRequest add(SolrInputDocument doc) {
		if (documents == null) {
			documents = new ArrayList<SolrDoc>(2);
		}
		SolrDoc solrDoc = new SolrDoc();
		solrDoc.document = doc;
		solrDoc.commitWithin = -1;
		solrDoc.overwrite = true;
		documents.add(solrDoc);

		return this;
	}

	public GaiaUpdateRequest add(SolrInputDocument doc, int commitWithin, boolean overwrite) {
		if (documents == null) {
			documents = new ArrayList<SolrDoc>(2);
		}
		SolrDoc solrDoc = new SolrDoc();
		solrDoc.document = doc;
		solrDoc.commitWithin = commitWithin;
		solrDoc.overwrite = overwrite;
		documents.add(solrDoc);

		return this;
	}

	public GaiaUpdateRequest deleteById(String id) {
		if (deleteById == null) {
			deleteById = new ArrayList<String>();
		}
		deleteById.add(id);
		return this;
	}

	public GaiaUpdateRequest deleteById(List<String> ids) {
		if (deleteById == null)
			deleteById = new ArrayList<String>(ids);
		else {
			deleteById.addAll(ids);
		}
		return this;
	}

	public GaiaUpdateRequest deleteByQuery(String q) {
		if (deleteQuery == null) {
			deleteQuery = new ArrayList<String>();
		}
		deleteQuery.add(q);
		return this;
	}

	public Collection<ContentStream> getContentStreams() throws IOException {
		return ClientUtils.toContentStreams(getXML(), "application/xml; charset=UTF-8");
	}

	public String getXML() throws IOException {
		StringWriter writer = new StringWriter();
		writeXML(writer);
		writer.flush();

		String xml = writer.toString();

		return xml.length() > 0 ? xml : null;
	}

	public void writeXML(Writer writer) throws IOException {
		List<List<SolrDoc>> getDocLists = getDocLists(documents);

		for (List<SolrDoc> docs : getDocLists) {
			if ((docs != null) && (docs.size() > 0)) {
				SolrDoc firstDoc = (SolrDoc) docs.get(0);
				int commitWithin = firstDoc.commitWithin != -1 ? firstDoc.commitWithin : this.commitWithin;
				boolean overwrite = firstDoc.overwrite;
				if ((commitWithin > -1) || (overwrite != true))
					writer.write("<add commitWithin=\"" + commitWithin + "\" " + "overwrite=\"" + overwrite + "\">");
				else {
					writer.write("<add>");
				}
				if (documents != null) {
					for (SolrDoc doc : documents) {
						if (doc != null) {
							ClientUtils.writeXML(doc.document, writer);
						}
					}
				}

				writer.write("</add>");
			}

		}

		boolean deleteI = (deleteById != null) && (deleteById.size() > 0);
		boolean deleteQ = (deleteQuery != null) && (deleteQuery.size() > 0);
		if ((deleteI) || (deleteQ)) {
			writer.append("<delete>");
			if (deleteI) {
				for (String id : deleteById) {
					writer.append("<id>");
					XML.escapeCharData(id, writer);
					writer.append("</id>");
				}
			}
			if (deleteQ) {
				for (String q : deleteQuery) {
					writer.append("<query>");
					XML.escapeCharData(q, writer);
					writer.append("</query>");
				}
			}
			writer.append("</delete>");
		}
	}

	private List<List<SolrDoc>> getDocLists(List<SolrDoc> documents) {
		List<List<SolrDoc>> docLists = new ArrayList<List<SolrDoc>>();
		if (documents == null) {
			return docLists;
		}
		boolean lastOverwrite = true;
		int lastCommitWithin = -1;
		List<SolrDoc> docList = null;
		for (SolrDoc doc : documents) {
			if ((doc.overwrite != lastOverwrite) || (doc.commitWithin != lastCommitWithin) || (docLists.size() == 0)) {
				docList = new ArrayList<SolrDoc>();
				docLists.add(docList);
			}
			docList.add(doc);
			lastCommitWithin = doc.commitWithin;
			lastOverwrite = doc.overwrite;
		}

		return docLists;
	}

	public List<String> getDeleteById() {
		return deleteById;
	}

	public List<String> getDeleteQuery() {
		return deleteQuery;
	}

	private class SolrDoc {
		SolrInputDocument document;
		int commitWithin;
		boolean overwrite;

		private SolrDoc() {
		}

		public String toString() {
			return "SolrDoc [document=" + document + ", commitWithin=" + commitWithin + ", overwrite=" + overwrite + "]";
		}
	}
}
