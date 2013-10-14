package de.fuberlin.wiwiss.pubby;

import java.util.List;
import java.util.Map;

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
	
	/**
	 * Returns a subgraph of the data source describing one resource.
	 * This should include both incoming and outgoing triples. However,
	 * it should exclude outgoing arcs where the property is a
	 * high-outdegree property, and it should exclude incoming arcs where
	 * the property is a high-indegree property. If labels for other
	 * resources are included in the result, then they will be used.
	 * @param resourceURI The resource to be described
	 * @return A subgraph of the data source describing the resource.
	 */
	Model getResourceDescription(String resourceURI);

	/**
	 * If {@link #getResourceDescription(String)} omits properties of
	 * high indegree, then those properties must be returned here with
	 * the count of arcs. If high-indegree properties are not omitted,
	 * or the resource doesn't have any, then an empty map or null may be
	 * returned. Entries with value 0 will be ignored.
	 * @param resourceURI The resource to be described
	 * @return A map containing high-indegree properties with number of arcs
	 * 		for the resource
	 */
	Map<Property, Integer> getHighIndegreeProperties(String resourceURI);
	
	/**
	 * If {@link #getResourceDescription(String)} omits properties of
	 * high outdegree, then those properties must be returned here with
	 * the count of arcs. If high-outdegree properties are not omitted,
	 * or the resource doesn't have any, then an empty map or null may be
	 * returned. Entries with value 0 will be ignored.
	 * @param resourceURI The resource to be described
	 * @return A map containing high-outdegree properties with number of arcs
	 * 		for the resource
	 */
	Map<Property, Integer> getHighOutdegreeProperties(String resourceURI);
	
	/**
	 * Returns a subgraph of the data source. It lists the values of a
	 * particular property of a particular resource.
	 * @param resourceURI The resource to be examined
	 * @param property The property we're interested in
	 * @param isInverse Are we interested in outgoing arcs (<tt>false</tt>) or incoming (<tt>true</tt>)?
	 * @param describeAnonymous If <tt>true</tt>, list only blank nodes, and include complete descriptions
	 * @return A subgraph of the data source. 
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
