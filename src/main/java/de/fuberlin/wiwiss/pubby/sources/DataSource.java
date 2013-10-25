package de.fuberlin.wiwiss.pubby.sources;

import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A source of RDF data intended for publication through
 * the server.
 * 
 * TODO: describeResource and getHighXxxdegreeProperties should be combined into a single method with a complex result so that implementations can better mess with the high-degree stuff while keeping responses consistent between methods
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public interface DataSource {
	static final int MAX_INDEX_SIZE = 100;
	
	/**
	 * Indicates whether this data source may have some information about
	 * a given IRI.
	 * If this is <code>false</code>, a client should not bother to call
	 * {@link #describeResource(String)}. This method is to allow for
	 * optimizations and should respond very fast.
	 * 
	 * It is also used by
	 * the UI to decide whether a click on a resource should go to a Pubby
	 * page (if <code>true</code>) or out to the Web (if <code>false</code>).
	 * 
	 * @param absoluteIRI The IRI of a resource to be described
	 * @return <code>true</code> if this data source might have something about it
	 */
	boolean canDescribe(String absoluteIRI);
	
	/**
	 * Returns a subgraph of the data source describing one resource.
	 * This should include both incoming and outgoing triples. However,
	 * it should exclude outgoing arcs where the property is a
	 * high-outdegree property, and it should exclude incoming arcs where
	 * the property is a high-indegree property. If labels for other
	 * resources are included in the result, then they will be used.
	 * @param absoluteIRI The IRI of the resource to be described
	 * @return A subgraph of the data source describing the resource.
	 */
	Model describeResource(String absoluteIRI);

	/**
	 * If {@link #describeResource(String)} omits properties of
	 * high indegree, then those properties must be returned here with
	 * the count of arcs. If high-indegree properties are not omitted,
	 * or the resource doesn't have any, then an empty map or null may be
	 * returned. Entries with value 0 will be ignored.
	 * @param resourceIRI The IRI of the resource to be described
	 * @return A map containing high-indegree properties with number of arcs
	 * 		for the resource
	 */
	Map<Property, Integer> getHighIndegreeProperties(String resourceIRI);
	
	/**
	 * If {@link #describeResource(String)} omits properties of
	 * high outdegree, then those properties must be returned here with
	 * the count of arcs. If high-outdegree properties are not omitted,
	 * or the resource doesn't have any, then an empty map or null may be
	 * returned. Entries with value 0 will be ignored.
	 * @param resourceIRI The IRI of the resource to be described
	 * @return A map containing high-outdegree properties with number of arcs
	 * 		for the resource
	 */
	Map<Property, Integer> getHighOutdegreeProperties(String resourceIRI);
	
	/**
	 * Returns a subgraph of the data source. It lists the values of a
	 * particular property of a particular resource. Where values are blank
	 * nodes, a complete description of these anonymous resources must be
	 * included.
	 * @param resourceIRI The resource to be examined
	 * @param property The property we're interested in
	 * @param isInverse Are we interested in outgoing arcs (<tt>false</tt>) or incoming (<tt>true</tt>)?
	 * @return A subgraph of the data source. 
	 */
	Model listPropertyValues(String resourceIRI, Property property, 
			boolean isInverse);
	
	/**
	 * A list of IRI resources described in this data source. Ordering is
	 * implementation-defined. Usually a reasonable limit should be applied
	 * to the number of resources returned.
	 */
	List<Resource> getIndex();
}
