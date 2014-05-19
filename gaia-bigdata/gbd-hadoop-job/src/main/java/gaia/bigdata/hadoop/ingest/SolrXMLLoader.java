package gaia.bigdata.hadoop.ingest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.StrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.bigdata.hbase.documents.Document;

class SolrXMLLoader {
	private static transient Logger log = LoggerFactory
			.getLogger(SolrXMLLoader.class);
	XMLInputFactory inputFactory;
	SAXParserFactory saxFactory;
	private final String collection;
	private final String idField;

	SolrXMLLoader(String collection, String idField) {
		this.collection = collection;
		this.idField = idField;
		inputFactory = XMLInputFactory.newInstance();
		EmptyEntityResolver.configureXMLInputFactory(inputFactory);
		try {
			inputFactory.setProperty("reuse-instance", Boolean.FALSE);
		} catch (IllegalArgumentException ex) {
			log.debug(new StringBuilder()
					.append("Unable to set the 'reuse-instance' property for the input chain: ")
					.append(inputFactory).toString());
		}

		saxFactory = SAXParserFactory.newInstance();
		saxFactory.setNamespaceAware(true);
		EmptyEntityResolver.configureSAXParserFactory(saxFactory);
	}

	public Collection<Document> readDocs(InputStream input,
			String missingIdPrefix) throws XMLStreamException {
		XMLStreamReader parser = parser = inputFactory
				.createXMLStreamReader(input);
		Collection docs = Collections.emptyList();
		int event = 0;
		try {
			long docCount = 0L;
			while ((event = parser.next()) != 8) {
				switch (event) {
				case 1:
					String currTag = parser.getLocalName();
					if (currTag.equals("add")) {
						docs = new ArrayList();
					} else if (currTag.equals("doc")) {
						docs.add(readDoc(parser, missingIdPrefix, docCount));
						docCount += 1L;
					}
					break;
				}
			}

		} finally {
			parser.close();
		}
		return docs;
	}

	private Document readDoc(XMLStreamReader parser, String missingIdPrefix,
			long docCount) throws XMLStreamException {
		Document doc = new Document(collection);

		String attrName = "";
		double docBoost = 1.0D;
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			attrName = parser.getAttributeLocalName(i);
			if ("boost".equals(attrName))
				docBoost = Float.parseFloat(parser.getAttributeValue(i));
			else {
				log.warn(new StringBuilder().append("Unknown attribute doc/@")
						.append(attrName).toString());
			}
		}

		StringBuilder text = new StringBuilder();
		String name = null;
		double boost = 1.0D;
		boolean isNull = false;
		String update = null;
		Map<String, Map<String, Object>> updateMap = null;
		boolean complete = false;
		while (!complete) {
			int event = parser.next();
			switch (event) {
			case 4:
			case 6:
			case 12:
				text.append(parser.getText());
				break;
			case 2:
				if ("doc".equals(parser.getLocalName())) {
					complete = true;
				} else if ("field".equals(parser.getLocalName())) {
					Object v = isNull ? null : text.toString();
					if (update != null) {
						if (updateMap == null)
							updateMap = new HashMap();
						Map<String, Object> extendedValues = updateMap.get(name);
						if (extendedValues == null) {
							extendedValues = new HashMap(1);
							updateMap.put(name, extendedValues);
						}
						Object val = extendedValues.get(update);
						if (val == null) {
							extendedValues.put(update, v);
						} else if ((val instanceof List)) {
							List list = (List) val;
							list.add(v);
						} else {
							List values = new ArrayList();
							values.add(val);
							values.add(v);
							extendedValues.put(update, values);
						}

					} else {
						doc.fields.put(name, v);
						if (boost != 1.0D) {
							doc.boosts.put(name,
									Double.valueOf(boost * docBoost));
						}
						boost = 1.0D;
					}
				}
				break;
			case 1:
				text.setLength(0);
				String localName = parser.getLocalName();
				if (!"field".equals(localName)) {
					log.warn(new StringBuilder()
							.append("unexpected XML tag doc/")
							.append(localName).toString());
					throw new SolrException(
							SolrException.ErrorCode.BAD_REQUEST,
							new StringBuilder()
									.append("unexpected XML tag doc/")
									.append(localName).toString());
				}

				boost = 1.0D;
				update = null;
				String attrVal = "";
				for (int i = 0; i < parser.getAttributeCount(); i++) {
					attrName = parser.getAttributeLocalName(i);
					attrVal = parser.getAttributeValue(i);
					if ("name".equals(attrName))
						name = attrVal;
					else if ("boost".equals(attrName))
						boost = Float.parseFloat(attrVal);
					else if ("null".equals(attrName))
						isNull = StrUtils.parseBoolean(attrVal);
					else if ("update".equals(attrName))
						update = attrVal;
					else
						log.warn(new StringBuilder()
								.append("Unknown attribute doc/field/@")
								.append(attrName).toString());
				}
			case 3:
			case 5:
			case 7:
			case 8:
			case 9:
			case 10:
			case 11:
			}
		}
		if (updateMap != null) {
			for (Map.Entry entry : updateMap.entrySet()) {
				name = (String) entry.getKey();
				Map value = (Map) entry.getValue();

				doc.fields.put(name, value);
				doc.boosts.put(name, Double.valueOf(docBoost));
			}
		}
		Object id = doc.fields.get(idField);
		if (id != null)
			doc.id = id.toString();
		else {
			doc.id = new StringBuilder().append(missingIdPrefix)
					.append(docCount).toString();
		}
		return doc;
	}
}
