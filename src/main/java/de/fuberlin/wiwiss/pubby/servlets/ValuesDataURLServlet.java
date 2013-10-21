package de.fuberlin.wiwiss.pubby.servlets;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.HypermediaResource;
import de.fuberlin.wiwiss.pubby.ModelResponse;
import de.fuberlin.wiwiss.pubby.ResourceDescription;
import de.fuberlin.wiwiss.pubby.ResourceDescription.ResourceProperty;

/**
 * A servlet for serving an RDF document describing resources
 * related to a given resource via a given property. The property
 * can be a forward or backward arc.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class ValuesDataURLServlet extends ValuesBaseServlet {

	public boolean doGet(HypermediaResource controller,
			Property predicate, boolean isInverse, 
			HttpServletRequest request,
			HttpServletResponse response,
			Configuration config) throws IOException {
		// TODO: If no data ("return false"), respond with plain text like the PageURLServlet, not with HTML
		
		// TODO: Good bit of duplication with ValuesURLServlet here
		ResourceDescription resource = controller.getResourceDescription();
		if (resource == null) return false;

		Model descriptions = config.getDataSource().listPropertyValues(
				controller.getAbsoluteIRI(), predicate, isInverse);
		if (descriptions.isEmpty()) return false;
		ResourceProperty property = new ResourceDescription(
				controller, descriptions, config).getProperty(predicate, isInverse);
		if (property == null) return false;	// Can happen if prefix is declared in URI space of a data source rather than in web space

		addDocumentMetadata(descriptions, controller, 
				addQueryString(
						isInverse 
								? controller.getInverseValuesDataURL(predicate) 
								: controller.getValuesDataURL(predicate),
						request),
				getDocumentTitle(
						resource.getTitle(), property.getCompleteLabel(), isInverse));
		
		new ModelResponse(descriptions, request, response).serve();
		return true;
	}

	public String getDocumentTitle(String resourceLabel, String propertyLabel,
			boolean isInverse) {
		if (isInverse) {
			return "RDF description of all resources whose " + propertyLabel + " is " + resourceLabel;
		} else { 
			return "RDF description of all values that are " + propertyLabel + " of " + resourceLabel;
		}
	}

	private static final long serialVersionUID = -7927775670218866340L;
}