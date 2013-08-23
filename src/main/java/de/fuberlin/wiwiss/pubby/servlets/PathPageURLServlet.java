package de.fuberlin.wiwiss.pubby.servlets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.HypermediaResource;
import de.fuberlin.wiwiss.pubby.MappedResource;
import de.fuberlin.wiwiss.pubby.AbsResourceDescription;
import de.fuberlin.wiwiss.pubby.PageResourceDescription;

/**
 * A servlet for rendering an HTML page describing the blank nodes
 * related to a given resource via a given property.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class PathPageURLServlet extends BasePathServlet {

	public boolean doGet(HypermediaResource controller,
			Collection<MappedResource> resources, 
			Property property, boolean isInverse, 
			HttpServletRequest request,
			HttpServletResponse response,
			Configuration config) throws IOException {		
		Model descriptions = getAnonymousPropertyValues(resources, property, isInverse);
		if (descriptions.size() == 0) {
			return false;
		}

		Resource r = descriptions.getResource(controller.getAbsoluteIRI());
		List<AbsResourceDescription> resourceDescriptions = new ArrayList<AbsResourceDescription>();
		for (Resource value: getPropertyValues(r, property, isInverse) ) {
		    resourceDescriptions.add(new PageResourceDescription( value, descriptions, config));
		}
		for (Resource value: getPropertyValues(r, descriptions.getProperty(config.mapResource(property.getURI()).getController().getAbsoluteIRI()), isInverse) ) {
		    resourceDescriptions.add(new PageResourceDescription( value, descriptions, config));
		}
		
		Model description = getResourceDescription(resources);
		AbsResourceDescription resourceDescription = new PageResourceDescription(
				controller, description, config);

		String title = resourceDescription.getLabel() + (isInverse ? " « " : " » ") +
				config.getPrefixes().getNsURIPrefix(property.getNameSpace()) + ":" + 
				property.getLocalName();
		VelocityHelper template = new VelocityHelper(getServletContext(), response);
		Context context = template.getVelocityContext();
		context.put("project_name", config.getProjectName());
		context.put("project_link", config.getProjectLink());
		context.put("title", title);
		context.put("server_base", config.getWebApplicationBaseURI());
		context.put("sparql_endpoint", getFirstSPARQLEndpoint(resources));
		context.put("back_uri", controller.getAbsoluteIRI());
		context.put("back_label", resourceDescription.getLabel());
		context.put("rdf_link", isInverse ? controller.getInversePathDataURL(property) : controller.getPathDataURL(property));
		context.put("resources", resourceDescriptions);
		template.renderXHTML("pathpage.vm");
		return true;
	}

	Iterable<Resource> getPropertyValues(Resource r, Property property, boolean isInverse) {
		List<Resource> result = new ArrayList<Resource>();
		StmtIterator it = isInverse
				? r.getModel().listStatements(null, property, r)
				: r.listProperties(property);
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			RDFNode value = isInverse ? stmt.getSubject() : stmt.getObject();
			if (!value.isAnon()) continue;
			result.add(value.asResource());
		}
		return result;
	}
	
	private static final long serialVersionUID = -2597664961896022667L;
}
