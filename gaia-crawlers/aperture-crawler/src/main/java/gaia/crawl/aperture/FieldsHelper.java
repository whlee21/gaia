package gaia.crawl.aperture;

import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.datasource.FieldMappingUtil;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.DateUtil;
import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.DatatypeLiteral;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.vocabulary.RDF;
import org.ontoware.rdf2go.vocabulary.XSD;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.semanticdesktop.aperture.vocabulary.NAO;
import org.semanticdesktop.aperture.vocabulary.NCO;
import org.semanticdesktop.aperture.vocabulary.NIE;
import org.semanticdesktop.aperture.vocabulary.NMO;
import org.semanticdesktop.aperture.vocabulary.xmp.DC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldsHelper {
	private static transient Logger LOG = LoggerFactory.getLogger(FieldsHelper.class);

	private static ThreadLocal<DateFormat> dateTimeFormatTL = new ThreadLocal<DateFormat>();

	private static ThreadLocal<DateFormat> dateFormatTL = new ThreadLocal<DateFormat>();

	public static boolean addFieldsToDocument(SolrInputDocument doc, String defaultField, RDFContainer metadata,
			FieldMapping fieldMapping) throws IOException {
		boolean seenUniqueFieldName = false;

		boolean sawDublinCoreTitle = false;
		boolean sawApertureTitle = false;

		boolean sawDublinCoreCreator = false;
		boolean sawApertureCreator = false;

		for (ClosableIterator<Statement> i = metadata.getModel().iterator(); i.hasNext();) {
			Statement s = i.next();
			LOG.debug(new StringBuilder().append("s: ").append(s.toString()).toString());
			org.ontoware.rdf2go.model.node.URI predicate = s.getPredicate();
			Node object = s.getObject();

			if (predicate.equals(DC.title)) {
				sawDublinCoreTitle = true;
				if (sawApertureTitle) {
					LOG.debug("Ignoring Dublin Core title since Aperture title already seen");
				}
			} else if (predicate.equals(NIE.title)) {
				sawApertureTitle = true;
				if (sawDublinCoreTitle) {
					LOG.debug("Ignoring Aperture title since Dublin Core title already seen");
				}
			} else {
				String value = null;

				if (predicate.equals(DC.creator)) {
					sawDublinCoreCreator = true;
					if (sawApertureCreator) {
						LOG.debug("Ignoring Dublin Core creator since Aperture creator already seen");
					}

				} else if (predicate.equals(NCO.creator)) {
					sawApertureCreator = true;
					if (sawDublinCoreCreator) {
						LOG.debug("Ignoring Aperture creator since Dublin Core creator already seen");
					} else {
						Node object2 = null;
						ClosableIterator<Statement> j = metadata.getModel().findStatements(object.asResource(), NCO.fullname,
								Variable.ANY);
						while (j.hasNext()) {
							Statement s2 = j.next();
							object2 = s2.getObject();
						}
						if (object2 == null) {
							LOG.warn(new StringBuilder().append("Unable to access fullname for Contact at URI ")
									.append(object.toSPARQL()).toString());
						} else
							object = object2;
					}
				} else {
					String name = extractName(predicate, defaultField);
					if (name != null) {
						String newName = FieldMappingUtil.map(name, fieldMapping, null);
						FieldMapping.FType fType = FieldMapping.FType.STRING;
						if (fieldMapping != null) {
							fType = fieldMapping.checkType(newName);
						}
						String uniqueKey = fieldMapping != null ? fieldMapping.getUniqueKey() : "id";
						if ((!seenUniqueFieldName) && (name.equals(uniqueKey))) {
							seenUniqueFieldName = true;
						}

						if ((object instanceof DatatypeLiteral)) {
							DatatypeLiteral l = (DatatypeLiteral) object;

							if ((l.getDatatype().equals(XSD._dateTime))
									|| (predicate.toString().equals("http://purl.org/dc/elements/1.1/date"))
									|| (fType == FieldMapping.FType.DATE)) {
								Date d = string2Date(l.getValue());
								if (d != null)
									value = DateUtil.getThreadLocalDateFormat().format(d);
							} else {
								value = l.getValue();
							}
						} else if ((predicate.toString().equals("http://purl.org/dc/elements/1.1/date"))
								|| (fType == FieldMapping.FType.DATE)) {
							value = DateUtil.getThreadLocalDateFormat().format(string2Date(object.toString()));
						} else {
							if (predicate.equals(NCO.contributor)) {
								if (!(object instanceof org.ontoware.rdf2go.model.node.URI))
									break;
								continue;
							}
							if ((predicate.equals(NMO.from)) || (predicate.equals(NMO.to)) || (predicate.equals(NMO.cc))
									|| (predicate.equals(NMO.bcc))) {
								String fullname = null;
								String email = null;
								ClosableIterator<Statement> j = metadata.getModel().findStatements(object.asResource(), Variable.ANY,
										Variable.ANY);
								while (j.hasNext()) {
									Statement s2 = j.next();
									if (!s2.getPredicate().equals(RDF.type)) {
										if (s2.getPredicate().equals(NCO.fullname)) {
											fullname = s2.getObject().toString();
										} else if (s2.getPredicate().equals(NCO.hasEmailAddress)) {
											email = s2.getObject().toString();
											if (email.startsWith("mailto:"))
												email = email.substring(7);
										}
									}
								}
								if ((fullname == null) && (email == null)) {
									value = object.toString();
								} else {
									StringBuilder sb = new StringBuilder();
									if (fullname != null)
										sb.append(fullname);
									if (email != null) {
										if (fullname != null)
											sb.append(" <");
										sb.append(email);
										if (fullname != null)
											sb.append(">");
									}
									value = sb.toString();
								}
							} else {
								if (predicate.equals(NAO.Tag)) {
									String val = object.asLiteral().getValue();
									int idx = val.indexOf('=');
									if (idx == -1) {
										continue;
									}
									String k = val.substring(0, idx);
									String v = val.substring(idx + 1);
									doc.addField(new StringBuilder().append("meta_").append(k).toString(), v);
									continue;
								}
								value = object.toString();
							}
						}

						if ((value != null) && (!"".equals(value))) {
							doc.addField(name, value);
						}
					}
				}
			}
		}
		return seenUniqueFieldName;
	}

	public static Date string2Date(String theDate) {
		try {
			return getDateTimeFormat().parse(theDate);
		} catch (ParseException e) {
			try {
				return getDateFormat().parse(theDate);
			} catch (ParseException e1) {
			}
		}
		return null;
	}

	public static DateFormat getDateFormat() {
		DateFormat dateFormat = (DateFormat) dateFormatTL.get();
		if (dateFormat == null) {
			dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(true);
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			dateFormatTL.set(dateFormat);
		}
		return dateFormat;
	}

	public static DateFormat getDateTimeFormat() {
		DateFormat dateTimeFormat = (DateFormat) dateTimeFormatTL.get();
		if (dateTimeFormat == null) {
			dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			dateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			dateTimeFormat.setLenient(true);
			dateTimeFormatTL.set(dateTimeFormat);
		}
		return dateTimeFormat;
	}

	public static String extractName(org.ontoware.rdf2go.model.node.URI predicate, String def) {
		String name = null;
		java.net.URI uri = predicate.asJavaURI();
		if (uri.getFragment() != null) {
			name = uri.getFragment();
		} else if (uri.getPath() != null) {
			String path = uri.getPath();

			int index = path.lastIndexOf("/");
			if (index != -1)
				name = path.substring(index + 1);
			else {
				name = path;
			}
		} else {
			name = def;
		}
		return name;
	}

	public static boolean addFieldValues(FieldValues fieldValues, SolrInputDocument doc, String uniqueFieldName,
			String uriStr) {
		boolean seenUniqueFieldName = false;
		Set<Object> docValues = new HashSet<Object>();
		for (Set<FieldValue> values : fieldValues.values()) {
			for (FieldValue val : values) {
				if (uriStr.endsWith(val.getPrefix())) {
					if ((!seenUniqueFieldName) && (val.getSuffix().equals(uniqueFieldName) == true)) {
						seenUniqueFieldName = true;
					}

					boolean add = true;
					Collection<Object> docField = doc.getFieldValues(val.getSuffix());
					if ((docField != null) && (!docField.isEmpty())) {
						docValues.clear();
						docValues.addAll(docField);
						if (docValues.contains(val.getValue())) {
							add = false;
						}
					}
					if (add) {
						doc.addField(val.getSuffix(), val.getValue(), val.getBoost());
					}
				}
			}
		}
		return seenUniqueFieldName;
	}
}
