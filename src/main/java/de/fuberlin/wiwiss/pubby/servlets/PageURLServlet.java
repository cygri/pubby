package de.fuberlin.wiwiss.pubby.servlets;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.HypermediaResource;
import de.fuberlin.wiwiss.pubby.MappedResource;
import de.fuberlin.wiwiss.pubby.ResourceDescription;

/**
 * A servlet for serving the HTML page describing a resource.
 * Invokes a Velocity template.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class PageURLServlet extends BaseURLServlet {

	public boolean doGet(HypermediaResource controller,
			Collection<MappedResource> resources, 
			HttpServletRequest request,
			HttpServletResponse response,
			Configuration config) throws ServletException, IOException {

		ResourceDescription description = getResourceDescription(controller, resources);
		if (description == null) return false;
		
		VelocityHelper template = new VelocityHelper(getServletContext(), response);
		Context context = template.getVelocityContext();
		context.put("project_name", config.getProjectName());
		context.put("project_link", config.getProjectLink());
		context.put("uri", description.getURI());
		context.put("server_base", config.getWebApplicationBaseURI());
		context.put("rdf_link", controller.getDataURL());
		context.put("sparql_endpoint", getFirstSPARQLEndpoint(resources));
		context.put("title", description.getTitle());
		context.put("comment", description.getComment());
		context.put("image", description.getImageURL());
		context.put("properties", description.getProperties());
		context.put("showLabels", config.showLabels());

		try {
			Model metadata = ModelFactory.createDefaultModel();
			for (MappedResource resource: resources) {
				Resource documentRepresentation = resource.getDataset().addMetadataFromTemplate( metadata, resource, getServletContext() );
				// Replaced the commented line by the following one because the
				// RDF graph we want to talk about is a specific representation
				// of the data identified by the getDataURL() URI.
				//                                       Olaf, May 28, 2010
				// context.put("metadata", metadata.getResource(resource.getDataURL()));
				context.put("metadata", documentRepresentation);
			}

			Map<String,String> nsSet = metadata.getNsPrefixMap();
			nsSet.putAll(description.getModel().getNsPrefixMap());
			context.put("prefixes", nsSet.entrySet());
			context.put("blankNodesMap", new HashMap<Resource,String>());
		}
		catch (Exception e) {
			context.put("metadata", Boolean.FALSE);
		}
	
		template.renderXHTML("page.vm");
		return true;
	}

	private static final long serialVersionUID = 3363621132360159793L;
}
