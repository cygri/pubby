package de.fuberlin.wiwiss.pubby.servlets;
import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.HypermediaResource;
import de.fuberlin.wiwiss.pubby.MappedResource;
import de.fuberlin.wiwiss.pubby.ResourceDescription;
import de.fuberlin.wiwiss.pubby.ResourceDescription.ResourceProperty;

/**
 * A servlet for rendering an HTML page listing resources
 * related to a given resource via a given property.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class ValuesURLServlet extends BasePathServlet {

	public boolean doGet(HypermediaResource controller,
			Collection<MappedResource> resources, 
			Property property, boolean isInverse, 
			HttpServletRequest request,
			HttpServletResponse response,
			Configuration config) throws IOException {		
		Model descriptions = listPropertyValues(resources, property, isInverse, false);
		if (descriptions.isEmpty()) return false;

		ResourceProperty prop = new ResourceDescription(
				controller, descriptions, config).getProperty(property, isInverse);

		// Description of the main resource, for title etc.
		ResourceDescription description = getResourceDescription(controller, resources);

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
		context.put("rdf_link", isInverse ? controller.getInverseValuesDataURL(property) : controller.getValuesDataURL(property));
		context.put("property", prop);
		context.put("showLabels", new Boolean(config.showLabels()));
		template.renderXHTML("valuespage.vm");
		return true;
	}
	
	private static final long serialVersionUID = -2597664961896022667L;
}
