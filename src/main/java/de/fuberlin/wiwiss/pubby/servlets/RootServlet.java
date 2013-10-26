package de.fuberlin.wiwiss.pubby.servlets;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.HypermediaControls;
import de.fuberlin.wiwiss.pubby.IRIEncoder;

/**
 * A catch-all servlet managing the URI space of the web application.
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

		// Older versions of Pubby used to have /pathpage and /pathdata,
		// more limited versions of what is now /values and /values.data.
		// 301-redirect the old locations to the new ones.
		Matcher m = Pattern.compile("^(pathpage|pathdata)(/.*)$").matcher(relativeURI);
		if (m.matches()) {
			String valuesURL = config.getWebApplicationBaseURI() + 
					("pathpage".equals(m.group(1)) ? "values" : "values.data")
					+ m.group(2);
			response.setStatus(301);
			response.sendRedirect(IRIEncoder.toURI(valuesURL));
			return true;
		}

		// Homepage.
		if ("".equals(relativeURI)) {
			// Get the URL where a description of the index resource may be found
			String indexURL = HypermediaControls.createFromIRI(config.getIndexIRI(), config).getBrowsableURL();
			if (!config.getWebApplicationBaseURI().equals(indexURL)) {
				// We need to redirect to the URL
				response.sendRedirect(IRIEncoder.toURI(indexURL));
				return true;
			}
		}
		
		// Assume it's a resource URI -- will produce 404 if not
		getServletContext().getNamedDispatcher("WebURIServlet").forward(request, response);
		return true;
	}
}
