package gaia.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.core.CoreContainer;

public final class LWELoadAdminUiServlet extends HttpServlet {
	private static final long serialVersionUID = 8735160818403563759L;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");

		PrintWriter out = response.getWriter();

		InputStream in = getServletContext().getResourceAsStream("/solr/admin.html");

		if (in != null)
			try {
				CoreContainer cores = (CoreContainer) request.getAttribute("org.apache.solr.CoreContainer");

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
