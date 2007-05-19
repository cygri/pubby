package de.fuberlin.wiwiss.pubby.servlets;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.MappedResource;

/**
 * An abstract base servlet for servlets that manage a namespace of resources.
 * This class handles preprocessing of the request to extract the
 * resource URI relative to the namespace root, and manages the
 * {@link Configuration} instance shared by all servlets.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public abstract class BaseResourceServlet extends BaseServlet {
	
	protected abstract boolean doGet(
			MappedResource resource,
			HttpServletRequest request,
			HttpServletResponse response,
			Configuration config) throws IOException, ServletException;
	
	public boolean doGet(String relativeURI, HttpServletRequest request,
			HttpServletResponse response, Configuration config) 
	throws IOException, ServletException {
		MappedResource resource = config.getMappedResourceFromRelativeWebURI(
				relativeURI);
		return resource != null && doGet(resource, request, response, config);
	}
}