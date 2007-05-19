package de.fuberlin.wiwiss.pubby;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;

public interface DataSource {

	String getEndpointURL();
	
	String getResourceDescriptionURL(String resourceURI);
	
	Model getResourceDescription(String resourceURI);
	
	Model getAnonymousPropertyValues(String resourceURI, Property property, boolean isInverse);
}
