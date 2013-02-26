package de.fuberlin.wiwiss.pubby.servlets;
import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.HypermediaResource;
import de.fuberlin.wiwiss.pubby.IRIEncoder;
import de.fuberlin.wiwiss.pubby.MappedResource;
import de.fuberlin.wiwiss.pubby.negotiation.ContentTypeNegotiator;
import de.fuberlin.wiwiss.pubby.negotiation.MediaRangeSpec;
import de.fuberlin.wiwiss.pubby.negotiation.PubbyNegotiator;

/**
 * Servlet that handles the public URIs of mapped resources.
 * It redirects either to the page URL or to the data URL,
 * based on content negotiation.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class WebURIServlet extends BaseServlet {

	public boolean doGet(String relativeURI, HttpServletRequest request,
			HttpServletResponse response, Configuration config) throws IOException {
		Collection<MappedResource> resources = 
				config.getMappedResourcesFromRelativeWebURI(relativeURI, true);
		if (resources.isEmpty()) return false;

		response.addHeader("Vary", "Accept, User-Agent");
		ContentTypeNegotiator negotiator = PubbyNegotiator.getPubbyNegotiator();
		MediaRangeSpec bestMatch = negotiator.getBestMatch(
				request.getHeader("Accept"), request.getHeader("User-Agent"));
		if (bestMatch == null) {
			response.setStatus(406);
			response.setContentType("text/plain");
			response.getOutputStream().println(
					"406 Not Acceptable: The requested data format is not supported. " +
					"Only HTML and RDF are available.");
			return true;
		}
		
		response.setStatus(303);
		response.setContentType("text/plain");
		String location;
		HypermediaResource hypermedia = config.getController(IRIEncoder.toIRI(relativeURI), true);
		if ("text/html".equals(bestMatch.getMediaType())) {
			location = hypermedia.getPageURL();
		} else {
			location = hypermedia.getDataURL();
		}
		response.addHeader("Location", IRIEncoder.toURI(location));
		response.getOutputStream().println(
				"303 See Other: For a description of this item, see " + location);
		return true;
	}

	private static final long serialVersionUID = 3797268342314917283L;
}