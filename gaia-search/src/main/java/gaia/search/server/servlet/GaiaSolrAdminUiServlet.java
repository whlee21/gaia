package gaia.search.server.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GaiaSolrAdminUiServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8010370268524639133L;

	private static final Logger LOG = LoggerFactory.getLogger(GaiaSolrAdminUiServlet.class);

	// private CoreContainer cores;
	//
	// @Inject
	// public GaiaSolrAdminUiServlet(CoreContainer cores) {
	// this.cores = cores;
	// }

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");

		PrintWriter out = response.getWriter();

		InputStream in = getServletContext().getResourceAsStream("/solr/admin.html");

		if (in != null)
			try {
				CoreContainer cores = (CoreContainer) request.getAttribute("org.apache.solr.CoreContainer");
				Enumeration<String> attrs = request.getAttributeNames();
				while (attrs.hasMoreElements()) {
					LOG.debug("whlee21 doGet attr = " + attrs.nextElement());
//					attrs.
				}
				String html = IOUtils.toString(in, "UTF-8");

				String[] search = { "${contextPath}", "${adminPath}" };
				String[] replace = { StringEscapeUtils.escapeJavaScript("/solr"),
						StringEscapeUtils.escapeJavaScript(cores.getAdminPath()) };

				out.println(StringUtils.replaceEach(html, search, replace));
			} finally {
				IOUtils.closeQuietly(in);
			}
		else
			out.println("solr");
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		doGet(request, response);
	}
}
