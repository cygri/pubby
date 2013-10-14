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
import de.fuberlin.wiwiss.pubby.ResourceDescription;

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
		Model descriptions = listPropertyValues(resources, property, isInverse, true);
		if (descriptions.isEmpty()) return false;

		Resource r = descriptions.getResource(controller.getAbsoluteIRI());
		List<ResourceDescription> resourceDescriptions = new ArrayList<ResourceDescription>();
		StmtIterator it = isInverse
				? descriptions.listStatements(null, property, r)
				: r.listProperties(property);
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			RDFNode value = isInverse ? stmt.getSubject() : stmt.getObject();
			resourceDescriptions.add(new ResourceDescription(
					value.asResource(), descriptions, config));
		}
		
		ResourceDescription description = getResourceDescription(controller, resources);

		// TODO: This is 100% duplicated with ValuesURLServlet
		String propertyTitle = null;
		boolean showAsInverse = isInverse;
		if (config.showLabels()) {
			if (showAsInverse) {
				propertyTitle = description.toTitleCase(
						config.getVocabularyStore().getInverseLabel(property.getURI(), true), null);
				if (propertyTitle != null) {
					showAsInverse = false;
				}
			}
			if (propertyTitle == null) {
				propertyTitle = description.toTitleCase(
						config.getVocabularyStore().getLabel(property.getURI(), true), null);
			}
		}
		if (propertyTitle == null) {
			propertyTitle = config.getPrefixes().getNsURIPrefix(property.getNameSpace()) + 
					":" + property.getLocalName();
		}
		String title = description.getTitle() + 
				(showAsInverse ? " \u00AB " : " \u00BB ") +
				propertyTitle;
		
		VelocityHelper template = new VelocityHelper(getServletContext(), response);
		Context context = template.getVelocityContext();
		context.put("project_name", config.getProjectName());
		context.put("project_link", config.getProjectLink());
		context.put("title", title);
		context.put("server_base", config.getWebApplicationBaseURI());
		context.put("sparql_endpoint", getFirstSPARQLEndpoint(resources));
		context.put("back_uri", controller.getAbsoluteIRI());
		context.put("back_label", description.getTitle());
		context.put("rdf_link", isInverse ? controller.getInversePathDataURL(property) : controller.getPathDataURL(property));
		context.put("resources", resourceDescriptions);
		context.put("showLabels", new Boolean(config.showLabels()));
		template.renderXHTML("pathpage.vm");
		return true;
	}
	
	private static final long serialVersionUID = -2597664961896022667L;
}
