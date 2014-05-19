package gaia.solr.click;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClickServlet extends HttpServlet {
	private static final long serialVersionUID = 1032755502493426257L;
	private static final Logger LOG = LoggerFactory.getLogger("click.log");

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String reqId = req.getParameter("reqId");
		String docId = req.getParameter("docId");
		if ((docId == null) || (reqId == null)) {
			resp.setStatus(400);
			return;
		}
		String pos = req.getParameter("pos");
		String time = Long.toHexString(System.currentTimeMillis());
		if (pos != null)
			LOG.info("C\t" + time + "\t" + reqId + "\t" + docId + "\t" + pos);
		else {
			LOG.info("C\t" + time + "\t" + reqId + "\t" + docId + "\t-1");
		}
		resp.setStatus(200);
	}
}
