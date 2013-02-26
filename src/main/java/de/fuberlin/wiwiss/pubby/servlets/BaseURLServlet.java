package de.fuberlin.wiwiss.pubby.servlets;
import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.HypermediaResource;
import de.fuberlin.wiwiss.pubby.IRIEncoder;
import de.fuberlin.wiwiss.pubby.MappedResource;

/**
 * An abstract base servlet for servlets that manage a namespace
 * of documents related to a set of resources. This class handles
 * preprocessing of the request to extract the resource URI.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public abstract class BaseURLServlet extends BaseServlet {

	protected abstract boolean doGet(
			HypermediaResource controller,
			Collection<MappedResource> resources,
			HttpServletRequest request,
			HttpServletResponse response,
			Configuration config) throws IOException, ServletException;
	
	public boolean doGet(String relativeURI, HttpServletRequest request,
			HttpServletResponse response, Configuration config) 
	throws IOException, ServletException {
		HypermediaResource controller = config.getController(IRIEncoder.toIRI(relativeURI), false);
		Collection<MappedResource> resources = config.getMappedResourcesFromRelativeWebURI(
				relativeURI, false);
		if (resources.isEmpty()) return false;
		if (!doGet(controller, resources, request, response, config)) {
			send404(response, config.getWebApplicationBaseURI() + relativeURI);
		}
		return true;
	}
	
	private static final long serialVersionUID = -9003417732598023676L;
}