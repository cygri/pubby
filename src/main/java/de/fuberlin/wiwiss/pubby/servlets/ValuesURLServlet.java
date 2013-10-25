package de.fuberlin.wiwiss.pubby.servlets;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.HypermediaControls;
import de.fuberlin.wiwiss.pubby.ResourceDescription;
import de.fuberlin.wiwiss.pubby.ResourceDescription.ResourceProperty;

/**
 * A servlet for rendering an HTML page listing resources
 * related to a given resource via a given property. URIs and
 * literals are displayed as simple values. Blank nodes are
 * displayed as complete resource descriptions.
 */
public class ValuesURLServlet extends ValuesBaseServlet {

	public boolean doGet(HypermediaControls controller,
			Property predicate, boolean isInverse, 
			HttpServletRequest request,
			HttpServletResponse response,
			Configuration config) throws IOException {		
		ResourceDescription resource = controller.getResourceDescription();
		if (resource == null) return false;

		Model descriptions = config.getDataSource().listPropertyValues(
				controller.getAbsoluteIRI(), predicate, isInverse);
		if (descriptions.isEmpty()) return false;
		ResourceProperty property = new ResourceDescription(
				controller, descriptions, config).getProperty(predicate, isInverse);
		if (property == null) return false;	// Can happen if prefix is declared in URI space of a data source rather than in web space
		
		VelocityHelper template = new VelocityHelper(getServletContext(), response);
		Context context = template.getVelocityContext();
		context.put("project_name", config.getProjectName());
		context.put("project_link", config.getProjectLink());
		context.put("uri", resource.getURI());
		context.put("server_base", config.getWebApplicationBaseURI());
		context.put("title", resource.getTitle());
		context.put("head_title", resource.getTitle() + " \u00BB " + property.getCompleteLabel());
		context.put("property", property);
		context.put("back_uri", controller.getBrowsableURL());
		context.put("back_label", resource.getTitle());
		context.put("rdf_link", isInverse ? controller.getInverseValuesDataURL(predicate) : controller.getValuesDataURL(predicate));
		context.put("showLabels", config.showLabels());

		addPageMetadata(context, controller, resource.getModel());
		
		template.renderXHTML("valuespage.vm");
		return true;
	}
	
	private static final long serialVersionUID = -2597664961896022667L;
}
