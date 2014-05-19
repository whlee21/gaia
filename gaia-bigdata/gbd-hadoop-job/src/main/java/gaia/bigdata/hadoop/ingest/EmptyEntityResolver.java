package gaia.bigdata.hadoop.ingest;

import java.io.InputStream;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import org.apache.commons.io.input.ClosedInputStream;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public final class EmptyEntityResolver
{
  public static final EntityResolver SAX_INSTANCE = new EntityResolver()
  {
    public InputSource resolveEntity(String publicId, String systemId) {
      return new InputSource(ClosedInputStream.CLOSED_INPUT_STREAM);
    }
  };

  public static final XMLResolver STAX_INSTANCE = new XMLResolver()
  {
    public InputStream resolveEntity(String publicId, String systemId, String baseURI, String namespace) {
      return ClosedInputStream.CLOSED_INPUT_STREAM;
    }
  };

  private static void trySetSAXFeature(SAXParserFactory saxFactory, String feature, boolean enabled)
  {
    try
    {
      saxFactory.setFeature(feature, enabled);
    }
    catch (Exception ex)
    {
    }
  }

  public static void configureSAXParserFactory(SAXParserFactory saxFactory)
  {
    saxFactory.setValidating(false);

    trySetSAXFeature(saxFactory, "http://javax.xml.XMLConstants/feature/secure-processing", true);
  }

  private static void trySetStAXProperty(XMLInputFactory inputFactory, String key, Object value) {
    try {
      inputFactory.setProperty(key, value);
    }
    catch (Exception ex)
    {
    }
  }

  public static void configureXMLInputFactory(XMLInputFactory inputFactory)
  {
    trySetStAXProperty(inputFactory, "javax.xml.stream.isValidating", Boolean.FALSE);

    trySetStAXProperty(inputFactory, "javax.xml.stream.isSupportingExternalEntities", Boolean.TRUE);
    inputFactory.setXMLResolver(STAX_INSTANCE);
  }
}
