package de.fuberlin.wiwiss.pubby.servlets;
import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.DataResourceDescription;
import de.fuberlin.wiwiss.pubby.HypermediaResource;
import de.fuberlin.wiwiss.pubby.MappedResource;
import de.fuberlin.wiwiss.pubby.ModelResponse;
import de.fuberlin.wiwiss.pubby.AbsResourceDescription;
import de.fuberlin.wiwiss.pubby.vocab.FOAF;

/**
 * A servlet for serving an RDF document describing the blank nodes
 * related to a given resource via a given property.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class PathDataURLServlet extends BasePathServlet {
		
	public boolean doGet(HypermediaResource controller,
			Collection<MappedResource> resources, Property property, boolean isInverse, 
			HttpServletRequest request,
			HttpServletResponse response,
			Configuration config) throws IOException {

		Model descriptions = getAnonymousPropertyValues(resources, property, isInverse);
		if (descriptions.size() == 0) {
			return false;
		}
		
		// Add document metadata
		if (descriptions.qnameFor(FOAF.primaryTopic.getURI()) == null
				&& descriptions.getNsPrefixURI("foaf") == null) {
			descriptions.setNsPrefix("foaf", FOAF.NS);
		}
		if (descriptions.qnameFor(RDFS.label.getURI()) == null
				&& descriptions.getNsPrefixURI("rdfs") == null) {
			descriptions.setNsPrefix("rdfs", RDFS.getURI());
		}
		Resource r = descriptions.getResource(controller.getAbsoluteIRI());
		Resource document = descriptions.getResource(
				addQueryString(
						isInverse 
								? controller.getInversePathDataURL(property) 
								: controller.getPathDataURL(property), request));
		document.addProperty(FOAF.primaryTopic, r);
		String resourceLabel = new DataResourceDescription(controller, descriptions, config).getLabel();
		String propertyLabel = config.getPrefixes().qnameFor(property.getURI());
		if (isInverse) {
			document.addProperty(RDFS.label, 
					"RDF description of resources whose " + propertyLabel + " is " + resourceLabel);
		} else { 
			document.addProperty(RDFS.label, 
					"RDF description of resources that are " + propertyLabel + " of " + resourceLabel);
		}
		for (MappedResource resource: resources) {
			resource.getDataset().addDocumentMetadata(descriptions, document);
		}

		new ModelResponse(descriptions, request, response).serve();
		return true;
	}

	private static final long serialVersionUID = -7927775670218866340L;
}