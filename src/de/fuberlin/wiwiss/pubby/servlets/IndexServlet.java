package de.fuberlin.wiwiss.pubby.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.fuberlin.wiwiss.pubby.Configuration;

public class IndexServlet extends BaseServlet {

	protected boolean doGet(String relativeURI,
			HttpServletRequest request, HttpServletResponse response,
			Configuration config)
			throws IOException, ServletException {
		if (relativeURI.startsWith("static/")) {
			getServletContext().getNamedDispatcher("default").forward(request, response);
			return true;
		}
		if ("".equals(relativeURI) && config.getIndexResource() != null) {
			response.sendRedirect(config.getIndexResource().getWebURI());
			return true;
		}
		getServletContext().getNamedDispatcher("ResourceServlet").forward(request, response);
		return true;
	}
}
