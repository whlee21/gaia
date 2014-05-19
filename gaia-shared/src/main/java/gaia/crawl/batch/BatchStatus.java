package gaia.crawl.batch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import gaia.utils.StringUtils;

public class BatchStatus {
	public String collection;
	public String batchId;
	public String dsId;
	public String crawlerType;
	public String descr;
	public long startTime;
	public long finishTime;
	public long numDocs;
	public boolean parsed = false;
	public long parsedDocs;
	public static final String BID = "batch_id";
	public static final String COL = "collection";
	public static final String DSID = "ds_id";
	public static final String CRAWLER_TYPE = "crawler";
	public static final String DESCR = "description";
	public static final String START = "start_time";
	public static final String FINISH = "finish_time";
	public static final String NUMDOCS = "num_docs";
	public static final String PARSED = "parsed";
	public static final String PARSEDDOCS = "parsed_docs";

	protected BatchStatus() {
	}

	public BatchStatus(String crawlerType, String collection, String dsId, String batchId) {
		this.crawlerType = crawlerType;
		this.collection = collection;
		this.dsId = dsId;
		this.batchId = batchId;
		this.startTime = System.currentTimeMillis();
	}

	public void write(OutputStream out) throws IOException {
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
		pw.println("batch_id\t" + batchId);
		if (collection != null)
			pw.println("collection\t" + collection);
		if (crawlerType != null)
			pw.println("crawler\t" + crawlerType);
		if (dsId != null)
			pw.println("ds_id\t" + dsId);
		if (descr != null)
			pw.println("description\t" + descr);
		pw.println("start_time\t" + startTime);
		pw.println("finish_time\t" + finishTime);
		pw.println("num_docs\t" + numDocs);
		pw.println("parsed\t" + parsed);
		pw.println("parsed_docs\t" + parsedDocs);
		pw.flush();
		pw.close();
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("batch_id", batchId);
		map.put("collection", collection);
		map.put("crawler", crawlerType);
		map.put("description", descr);
		map.put("ds_id", dsId);
		map.put("finish_time", Long.valueOf(finishTime));
		map.put("num_docs", Long.valueOf(numDocs));
		map.put("parsed", Boolean.valueOf(parsed));
		map.put("parsed_docs", Long.valueOf(parsedDocs));
		map.put("start_time", Long.valueOf(startTime));
		return map;
	}

	public static BatchStatus fromMap(Map<String, Object> m) {
		if (m == null) {
			return null;
		}
		BatchStatus bs = new BatchStatus();
		bs.batchId = ((String) m.get("batch_id"));
		bs.collection = ((String) m.get("collection"));
		bs.crawlerType = ((String) m.get("crawler"));
		bs.descr = ((String) m.get("description"));
		bs.dsId = ((String) m.get("ds_id"));
		if (m.get("finish_time") != null) {
			bs.finishTime = Long.parseLong(m.get("finish_time").toString());
		}
		if (m.get("start_time") != null) {
			bs.startTime = Long.parseLong(m.get("start_time").toString());
		}
		if (m.get("num_docs") != null) {
			bs.numDocs = Long.parseLong(m.get("num_docs").toString());
		}
		if (m.get("parsed_docs") != null) {
			bs.parsedDocs = Long.parseLong(m.get("parsed_docs").toString());
		}
		bs.parsed = StringUtils.getBoolean(m.get("parsed")).booleanValue();
		return bs;
	}

	public static BatchStatus read(InputStream in) throws IOException {
		BatchStatus res = new BatchStatus();
		BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		String line;
		while ((line = br.readLine()) != null) {
			if ((line.length() != 0) && (line.charAt(0) != '#')) {
				int pos = line.indexOf(9);
				if (pos == -1) {
					throw new IOException("Invalid line: '" + line + "'");
				}
				String field = line.substring(0, pos);
				String val = null;
				if (pos < line.length() - 1)
					val = line.substring(pos + 1);
				if (field.equals("batch_id"))
					res.batchId = val;
				else if (field.equals("ds_id"))
					res.dsId = val;
				else if (field.equals("collection"))
					res.collection = val;
				else if (field.equals("crawler"))
					res.crawlerType = val;
				else if (field.equals("description"))
					res.descr = val;
				else if (field.equals("start_time"))
					res.startTime = Long.parseLong(val);
				else if (field.equals("finish_time"))
					res.finishTime = Long.parseLong(val);
				else if (field.equals("num_docs"))
					res.numDocs = Long.parseLong(val);
				else if (field.equals("parsed_docs"))
					res.parsedDocs = Long.parseLong(val);
				else if (field.equals("parsed"))
					res.parsed = Boolean.parseBoolean(val);
			}
		}
		br.close();
		if (res.batchId == null) {
			throw new IOException("Missing ID");
		}
		return res;
	}

	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + (batchId == null ? 0 : batchId.hashCode());
		result = prime * result + (collection == null ? 0 : collection.hashCode());

		result = prime * result + (crawlerType == null ? 0 : crawlerType.hashCode());

		result = prime * result + (descr == null ? 0 : descr.hashCode());
		result = prime * result + (dsId == null ? 0 : dsId.hashCode());
		result = prime * result + (int) (finishTime ^ finishTime >>> 32);
		result = prime * result + (int) (numDocs ^ numDocs >>> 32);
		result = prime * result + (parsed ? 1231 : 1237);
		result = prime * result + (int) (parsedDocs ^ parsedDocs >>> 32);
		result = prime * result + (int) (startTime ^ startTime >>> 32);
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BatchStatus other = (BatchStatus) obj;
		if (batchId == null) {
			if (other.batchId != null)
				return false;
		} else if (!batchId.equals(other.batchId))
			return false;
		if (collection == null) {
			if (other.collection != null)
				return false;
		} else if (!collection.equals(other.collection))
			return false;
		if (crawlerType == null) {
			if (other.crawlerType != null)
				return false;
		} else if (!crawlerType.equals(other.crawlerType))
			return false;
		if (descr == null) {
			if (other.descr != null)
				return false;
		} else if (!descr.equals(other.descr))
			return false;
		if (dsId == null) {
			if (other.dsId != null)
				return false;
		} else if (!dsId.equals(other.dsId))
			return false;
		if (finishTime != other.finishTime)
			return false;
		if (numDocs != other.numDocs)
			return false;
		if (parsed != other.parsed)
			return false;
		if (parsedDocs != other.parsedDocs)
			return false;
		if (startTime != other.startTime)
			return false;
		return true;
	}

	public String toString() {
		return "BatchStatus [collection=" + collection + ", batchId=" + batchId + ", dsId=" + dsId + ", crawlerType="
				+ crawlerType + ", descr=" + descr + ", startTime=" + startTime + ", finishTime=" + finishTime + ", numDocs="
				+ numDocs + ", parsed=" + parsed + ", parsedDocs=" + parsedDocs + "]";
	}
}
