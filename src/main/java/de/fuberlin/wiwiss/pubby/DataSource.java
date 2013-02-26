package de.fuberlin.wiwiss.pubby;

import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A source of RDF data intended for publication through
 * the server.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public interface DataSource {
	static final int MAX_INDEX_SIZE = 1000;
	
	String getEndpointURL();
	
	Model getResourceDescription(String resourceURI);
	
	Model getAnonymousPropertyValues(String resourceURI, Property property, boolean isInverse);
	
	/**
	 * A list of resources to be displayed as the contents of this data source.
	 * Usually the first (for some order) n subjects in the data source,
	 * where n is some data source defined value.
	 */
	List<Resource> getIndex();
}
