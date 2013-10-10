package de.fuberlin.wiwiss.pubby.servlets;
import java.util.Collection;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;

import de.fuberlin.wiwiss.pubby.MappedResource;

/**
 * A servlet for serving an RDF document listing the values of a given
 * property on a given resouce. The property can be a forward or backward arc.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class ValuesDataURLServlet extends BasePathDataServlet {

	@Override
	public Model getData(Collection<MappedResource> resources,
			Property property, boolean isInverse) {
		return listPropertyValues(resources, property, isInverse, false);
	}

	@Override
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