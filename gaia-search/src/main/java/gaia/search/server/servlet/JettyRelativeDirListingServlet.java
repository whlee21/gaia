package gaia.search.server.servlet;

import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.Resource;

public class JettyRelativeDirListingServlet extends DefaultServlet
{
  private static final String RELATIVE_BASE = "";
  private boolean _dirAllowed = true;

  private String _stripPath = null;
  private int _stripPathLength = 0;

  public void init() throws UnavailableException
  {
    super.init();
    this._dirAllowed = getInitBoolean("dirAllowed", this._dirAllowed);
    this._stripPath = getInitParameter("stripPath");
    this._stripPathLength = (null == this._stripPath ? 0 : this._stripPath.length());
  }

  private boolean getInitBoolean(String name, boolean dft)
  {
    String value = getInitParameter(name);
    if ((value == null) || (value.length() == 0)) return dft;
    return (value.startsWith("t")) || (value.startsWith("T")) || (value.startsWith("y")) || (value.startsWith("Y")) || (value.startsWith("1"));
  }

  public Resource getResource(String pathInContext)
  {
    String path = (null != this._stripPath) && (pathInContext.startsWith(this._stripPath)) ? pathInContext.substring(this._stripPathLength) : pathInContext;

    return super.getResource(path);
  }

  protected void sendDirectory(HttpServletRequest request, HttpServletResponse response, Resource resource, String pathInContext)
    throws IOException
  {
    if (!this._dirAllowed) {
      response.sendError(403);
      return;
    }

    String dir = resource.getListHTML("", pathInContext.length() > 1);

    if (dir == null) {
      response.sendError(403, "No directory");
      return;
    }

    byte[] data = dir.getBytes("UTF-8");
    response.setContentType("text/html; charset=UTF-8");
    response.setContentLength(data.length);
    response.getOutputStream().write(data);
  }
}
