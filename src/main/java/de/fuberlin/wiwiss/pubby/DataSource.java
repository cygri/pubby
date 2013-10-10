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
	
	/**
	 * Returns a subgraph of the data source. It lists the values of a
	 * particular property of a particular resource.
	 * @param resourceURI The resource to be examined
	 * @param property The property we're interested in
	 * @param isInverse Are we interested in outgoing arcs (<tt>false</tt>) or incoming (<tt>true</tt>)?
	 * @param describeAnonymous If <tt>true</tt>, list only blank nodes, and include complete descriptions
	 * @return Model A subgraph of the data source. 
	 */
	Model listPropertyValues(String resourceURI, Property property, 
			boolean isInverse, boolean describeAnonymous);
	
	/**
	 * A list of URI resources to be displayed as the contents of this data source.
	 * Usually the first (for some order) n subjects in the data source,
	 * where n is some data source defined value.
	 */
	List<Resource> getIndex();
}
