package de.fuberlin.wiwiss.pubby;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

/**
 * A data source backed by a SPARQL endpoint accessed through
 * the SPARQL protocol.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author Sergio Fernandez (sergio.fernandez@fundacionctic.org)
 * @version $Id$
 */
public class RemoteSPARQLDataSource implements DataSource {
	private final String endpointURL;
	private final String defaultGraphName;
	private String previousDescribeQuery;
	private String askContentType = null;
	
	public RemoteSPARQLDataSource(String endpointURL, String graphName) {
		this.endpointURL = endpointURL;
		this.defaultGraphName = graphName;
	}

	/**
	 * Sets the content type to ask for in the request to the remote
	 * SPARQL endpoint.
	 */
	public void setAskContentType(String mediaType) {
		this.askContentType = mediaType;
	}
	
	public String getEndpointURL() {
		return endpointURL;
	}

	private String buildDescribeQuery(String resourceURI) {
		return "DESCRIBE <" + resourceURI + ">";
	}

	public Model getResourceDescription(String resourceURI) {
		return execDescribeQuery(buildDescribeQuery(resourceURI));
	}
	
	public Model getAnonymousPropertyValues(String resourceURI, Property property, boolean isInverse) {
		String query = "DESCRIBE ?x WHERE { "
			+ (isInverse 
					? "?x <" + property.getURI() + "> <" + resourceURI + "> . "
					: "<" + resourceURI + "> <" + property.getURI() + "> ?x . ")
			+ "FILTER (isBlank(?x)) }";
		return execDescribeQuery(query);
	}
	
	
	@Override
	public List<Resource> getIndex() {
		List<Resource> result = new ArrayList<Resource>();
		ResultSet rs = execSelectQuery(
				"SELECT DISTINCT ?s { " +
				"?s ?p ?o " +
				"FILTER (isURI(?s)) " +
				"} LIMIT " + DataSource.MAX_INDEX_SIZE);
		while (rs.hasNext()) {
			result.add(rs.next().getResource("s"));
		}
		return result;
	}

	public String getPreviousDescribeQuery() {
		return previousDescribeQuery;
	}
	
	private Model execDescribeQuery(String query) {
		previousDescribeQuery = query;
		QueryEngineHTTP endpoint = new QueryEngineHTTP(endpointURL, query);
		if (defaultGraphName != null) {
			endpoint.setDefaultGraphURIs(Collections.singletonList(defaultGraphName));
		}
		return endpoint.execDescribe();
	}
	
	private ResultSet execSelectQuery(String query) {
		QueryEngineHTTP endpoint = new QueryEngineHTTP(endpointURL, query);
		if (defaultGraphName != null) {
			endpoint.setDefaultGraphURIs(Collections.singletonList(defaultGraphName));
		}
		if (askContentType != null) {
			endpoint.setAskContentType(askContentType);
		}
		return endpoint.execSelect();
	}
}
