package gaia.servlet;


//import org.eclipse.jetty.servlet.DefaultServlet;
//import org.eclipse.jetty.util.resource.Resource;

//public class JettyRelativeDirListingServlet extends DefaultServlet {
//	private static final String RELATIVE_BASE = "";
//	private boolean _dirAllowed = true;
//
//	private String _stripPath = null;
//	private int _stripPathLength = 0;
//
//	public void init() throws UnavailableException {
//		super.init();
//		_dirAllowed = getInitBoolean("dirAllowed", _dirAllowed);
//		_stripPath = getInitParameter("stripPath");
//		_stripPathLength = (null == _stripPath ? 0 : _stripPath.length());
//	}
//
//	private boolean getInitBoolean(String name, boolean dft) {
//		String value = getInitParameter(name);
//		if ((value == null) || (value.length() == 0))
//			return dft;
//		return (value.startsWith("t")) || (value.startsWith("T")) || (value.startsWith("y")) || (value.startsWith("Y"))
//				|| (value.startsWith("1"));
//	}
//
//	public Resource getResource(String pathInContext) {
//		String path = (null != _stripPath) && (pathInContext.startsWith(_stripPath)) ? pathInContext
//				.substring(_stripPathLength) : pathInContext;
//
//		return super.getResource(path);
//	}
//
//	protected void sendDirectory(HttpServletRequest request, HttpServletResponse response, Resource resource,
//			String pathInContext) throws IOException {
//		if (!_dirAllowed) {
//			response.sendError(403);
//			return;
//		}
//
//		String dir = resource.getListHTML("", pathInContext.length() > 1);
//
//		if (dir == null) {
//			response.sendError(403, "No directory");
//			return;
//		}
//
//		byte[] data = dir.getBytes("UTF-8");
//		response.setContentType("text/html; charset=UTF-8");
//		response.setContentLength(data.length);
//		response.getOutputStream().write(data);
//	}
//}

public class JettyRelativeDirListingServlet {
	
}