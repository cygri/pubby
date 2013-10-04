package de.fuberlin.wiwiss.pubby.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.IRIEncoder;

/**
 * A catch-all servlet managing the URI space of the web application.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author Sergio Fernández (sergio.fernandez@fundacionctic.org)
 * @version $Id$
 */
public class RootServlet extends BaseServlet {

	private static final long serialVersionUID = -4812044304620174504L;

	protected boolean doGet(String relativeURI,
			HttpServletRequest request, HttpServletResponse response,
			Configuration config)
			throws IOException, ServletException {
		
		// static/ directory handled by default servlet
		if (relativeURI.startsWith("static/")) {
			getServletContext().getNamedDispatcher("default").forward(request, response);
			return true;
		}

		// Homepage. If index resource is defined, and different from the current URI, redirect to it.
		if ("".equals(relativeURI) && config.getIndexResource() != null 
				&& !"".equals(config.getIndexResource().getRelativeIRI())) {
			response.sendRedirect(IRIEncoder.toURI(
					config.getIndexResource().getAbsoluteIRI()));
			return true;
		}
		
		// Assume it's a resource URI -- will produce 404 if not
		getServletContext().getNamedDispatcher("WebURIServlet").forward(request, response);
		return true;
	}
}
