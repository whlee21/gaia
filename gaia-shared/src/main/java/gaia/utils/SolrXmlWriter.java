package gaia.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.XML;

public class SolrXmlWriter {
	private OutputStreamWriter writer;
	private File file;

	public static void main(String[] args) {
		SolrXmlWriter writer = new SolrXmlWriter(new File(args[0]));

		List<Field> doc = new ArrayList<Field>();
		doc.add(new Field("field", "one"));
		writer.addDocument(doc);

		doc = new ArrayList<Field>();
		doc.add(new Field("field2", "two"));
		writer.addDocument(doc);

		doc = new ArrayList<Field>();
		doc.add(new Field("field2", "three"));
		writer.addDocument(doc);

		writer.close();
	}

	public SolrXmlWriter(File file) {
		this.file = file;
		try {
			writer = new OutputStreamWriter(new FileOutputStream(file), StringUtils.UTF_8);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		try {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.write("<add>");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void addDocument(List<Field> fields) {
		try {
			writer.write(StringUtils.LINE_SEPARATOR + "<doc>");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		for (Field field : fields) {
			try {
				writer.write(StringUtils.LINE_SEPARATOR);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			Map<String, String> attribs = new HashMap<String, String>();
			attribs.put("name", field.name);
			try {
				XML.writeXML(writer, "field", field.value, attribs);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		try {
			writer.write(StringUtils.LINE_SEPARATOR + "</doc>");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void addDocument(SolrInputDocument doc) {
		Set<String> keySet = (Set<String>) doc.getFieldNames();
		Object[] fieldNamesArray = keySet.toArray();
		int n = fieldNamesArray.length;
		List<Field> docFields = new ArrayList<Field>();
		for (int i = 0; i < n; i++) {
			String fieldName = (String) fieldNamesArray[i];
			Collection<Object> fieldValues = doc.getFieldValues(fieldName);
			for (Iterator<Object> iter = fieldValues.iterator(); iter.hasNext();) {
				Object fieldValue = iter.next();
				String fieldValueString = null;
				if (fieldValue != null)
					fieldValueString = fieldValue.toString();
				Field field = new Field(fieldName, fieldValueString);
				docFields.add(field);
			}
		}
		addDocument(docFields);
	}

	public void close() {
		try {
			writer.write("</add>");
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static class Field {
		String name;
		String value;
		float boost = 1.0F;

		public Field(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public Field(String name, String value, float boost) {
			this.name = name;
			this.value = value;
			this.boost = boost;
		}
	}
}
