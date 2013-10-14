package de.fuberlin.wiwiss.pubby;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.WebContent;

import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.engine.http.HttpQuery;
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
	private final String defaultGraphURI;
	private final boolean supportsSPARQL11;

	private final List<String> resourceQueries;
	private final List<String> propertyQueries;
	private final List<String> inversePropertyQueries;
	private final List<String> anonPropertyQueries;
	private final List<String> anonInversePropertyQueries;
	
	private final Collection<Property> highIndegreeProperties;
	private final Collection<Property> highOutdegreeProperties;
	
	private String previousDescribeQuery;
	private String contentType = null;
	
	public RemoteSPARQLDataSource(String endpointURL, String defaultGraphURI) {
		this(endpointURL, defaultGraphURI, false, null, null, null, null, null, null, null);
	}
	
	public RemoteSPARQLDataSource(String endpointURL, String defaultGraphURI,
			boolean supportsSPARQL11,
			List<String> resourceQueries, 
			List<String> propertyQueries, List<String> inversePropertyQueries,
			List<String> anonPropertyQueries, List<String> anonInversePropertyQueries,
			Collection<Property> highIndegreeProperties, Collection<Property> highOutdegreeProperties) {
		this.endpointURL = endpointURL;
		this.defaultGraphURI = defaultGraphURI;
		this.supportsSPARQL11 = supportsSPARQL11;
		if (resourceQueries == null || resourceQueries.isEmpty()) {
			resourceQueries = supportsSPARQL11 ?
				Arrays.asList(new String[]{
					"CONSTRUCT {?__this__ ?p ?o} WHERE {?__this__ ?p ?o. FILTER (?p NOT IN ?__high_outdegree_properties__)}",
					"CONSTRUCT {?s ?p ?__this__} WHERE {?s ?p ?__this__. FILTER (?p NOT IN ?__high_indegree_properties__)}"
				}) :
				Collections.singletonList("DESCRIBE ?__this__");
		}
		if (propertyQueries == null || propertyQueries.isEmpty()) {
			propertyQueries = Collections.singletonList(
					"CONSTRUCT {?__this__ ?__property__ ?x} WHERE {?__this__ ?__property__ ?x}");
		}
		if (inversePropertyQueries == null || inversePropertyQueries.isEmpty()) {
			inversePropertyQueries = Collections.singletonList(
					"CONSTRUCT {?x ?__property__ ?__this__} WHERE {?x ?__property__ ?__this__}");
		}
		if (anonPropertyQueries == null || anonPropertyQueries.isEmpty()) {
			anonPropertyQueries = Collections.singletonList(
					"DESCRIBE ?x WHERE {?__this__ ?__property__ ?x. FILTER (isBlank(?x))}");
		}
		if (anonInversePropertyQueries == null || anonInversePropertyQueries.isEmpty()) {
			anonInversePropertyQueries = Collections.singletonList(
					"DESCRIBE ?x WHERE {?x ?__property__ ?__this__. FILTER (isBlank(?x))}");
		}
		this.resourceQueries = resourceQueries;
		this.propertyQueries = propertyQueries;
		this.inversePropertyQueries = inversePropertyQueries;
		this.anonPropertyQueries = anonPropertyQueries;
		this.anonInversePropertyQueries = anonInversePropertyQueries;
		
		this.highIndegreeProperties = highIndegreeProperties == null ? 
				Collections.<Property>emptySet() : highIndegreeProperties;
		this.highOutdegreeProperties = highOutdegreeProperties == null ? 
				Collections.<Property>emptySet() : highOutdegreeProperties;
	}
	
	/**
	 * Sets the content type to ask for in the request to the remote
	 * SPARQL endpoint.
	 */
	public void setContentType(String mediaType) {
		this.contentType = mediaType;
	}

	@Override
	public String getEndpointURL() {
		return endpointURL;
	}

	public String getResourceDescriptionURL(String resourceURI) {
		try {
			StringBuffer result = new StringBuffer();
			result.append(endpointURL);
			result.append("?");
			if (defaultGraphURI != null) {
				result.append("default-graph-uri=");
				result.append(URLEncoder.encode(defaultGraphURI, "utf-8"));
				result.append("&");
			}
			result.append("query=");
			result.append(URLEncoder.encode(preProcessQuery(resourceQueries.get(0), resourceURI), "utf-8"));
			return result.toString();
		} catch (UnsupportedEncodingException ex) {
			// can't happen, utf-8 is always supported
			throw new RuntimeException(ex);
		}
	}

	@Override
	public Model getResourceDescription(String resourceURI) {
		// Loop over resource description queries, join results in a single model.
		// Process each query to replace place-holders of the given resource.
		Model model = ModelFactory.createDefaultModel();
		for (String query: resourceQueries) {
			Model result = executeQuery(preProcessQuery(query, resourceURI));
			model.add(result);
			model.setNsPrefixes(result);
		}
		return model;
	}

	@Override
	public Map<Property, Integer> getHighIndegreeProperties(String resourceURI) {
		return getHighDegreeProperties(
				"SELECT ?p (COUNT(?s) AS ?count) " +
				"WHERE { " +
				"  ?s ?p ?__this__. " +
				"  FILTER (?p IN ?__high_indegree_properties__)" +
				"}" +
				"GROUP BY ?p",
				resourceURI);
	}

	@Override
	public Map<Property, Integer> getHighOutdegreeProperties(String resourceURI) {
		return getHighDegreeProperties(
				"SELECT ?p (COUNT(?o) AS ?count) " +
				"WHERE { " +
				"  ?__this__ ?p ?o. " +
				"  FILTER (?p IN ?__high_outdegree_properties__)" +
				"}" +
				"GROUP BY ?p", 
				resourceURI);
	}

	private Map<Property, Integer> getHighDegreeProperties(String query, 
			String resourceURI) {
		if (!supportsSPARQL11) return null;
		query = preProcessQuery(query, resourceURI);
		ResultSet rs = execSelectQuery(query);
		Map<Property, Integer> results = new HashMap<Property, Integer>();
		while (rs.hasNext()) {
			QuerySolution solution = rs.next();
			Resource p = solution.get("p").asResource();
			int count = solution.get("count").asLiteral().getInt();
			results.put(ResourceFactory.createProperty(p.getURI()), count);
		}
		return results;
	}

	@Override
	public Model listPropertyValues(String resourceURI, Property property, 
			boolean isInverse, boolean describeAnonymous) {
		// Loop over the queries, join results in a single model.
		// Process each query to replace place-holders of the given resource and property.
		List<String> queries = describeAnonymous
				? (isInverse ? anonInversePropertyQueries : anonPropertyQueries)
				: (isInverse ? inversePropertyQueries : propertyQueries);
		Model model = ModelFactory.createDefaultModel();
		for (String query: queries) {
			String preprocessed = preProcessQuery(query, resourceURI, property);
			Model result = executeQuery(preprocessed);
			model.add(result);
			model.setNsPrefixes(result);
		}
		return model;
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
	
	private Model executeQuery(String queryString) {
		Model model = ModelFactory.createDefaultModel();
		previousDescribeQuery = queryString;

		// Since we don't know the exact query type (e.g. DESCRIBE or CONSTRUCT),
		// and com.hp.hpl.jena.query.QueryFactory could throw exceptions on
		// vendor-specific sections of the query, we use the lower-level
		// com.hp.hpl.jena.sparql.engine.http.HttpQuery to execute the query and
		// read the results into model.
		
		HttpQuery httpQuery = new HttpQuery(endpointURL);
		httpQuery.addParam("query", queryString);
		if (defaultGraphURI != null) {
			httpQuery.addParam("default-graph-uri", defaultGraphURI);
		}
		
		// The rest is more or less a copy of QueryEngineHTTP.execModel()
		httpQuery.setAccept(contentType);
		InputStream in = httpQuery.exec();

		// Don't assume the endpoint actually gives back the content type we
		// asked for
		String actualContentType = httpQuery.getContentType();

		// If the server fails to return a Content-Type then we will assume
		// the server returned the type we asked for
		if (actualContentType == null || actualContentType.equals("")) {
			actualContentType = contentType;
		}

		// Try to select language appropriately here based on the model content
		// type
		Lang lang = WebContent.contentTypeToLang(actualContentType);
		if (!RDFLanguages.isTriples(lang))
			throw new QueryException("Endpoint returned Content Type: " + actualContentType
					+ " which is not a valid RDF Graph syntax");
		RDFDataMgr.read(model, in, lang);
		return model;
	}
	
	private ResultSet execSelectQuery(String query) {
		QueryEngineHTTP endpoint = new QueryEngineHTTP(endpointURL, query);
		if (defaultGraphURI != null) {
			endpoint.setDefaultGraphURIs(Collections.singletonList(defaultGraphURI));
		}
		return endpoint.execSelect();
	}
	
	private String preProcessQuery(String query, String resourceURI) {
		return preProcessQuery(query, resourceURI, null);
	}
	
	private String preProcessQuery(String query, String resourceURI, Property property) {
		String result = replaceString(query, "?__this__", "<" + resourceURI + ">");
		if (property != null) {
			result = replaceString(result, "?__property__", "<" + property.getURI() + ">");
		}
		result = replaceString(result, "?__high_indegree_properties__", 
				toSPARQLArgumentList(highIndegreeProperties));
		result = replaceString(result, "?__high_outdegree_properties__", 
				toSPARQLArgumentList(highOutdegreeProperties));
		return result;
	}
	
	private String replaceString(String text, String searchString, String replacement) {
		int start = 0;
		int end = text.indexOf(searchString, start);
		if (end == -1) {
			return text;
		}

		int replacementLength = searchString.length();
		StringBuffer buf = new StringBuffer();
		while (end != -1) {
			buf.append(text.substring(start, end)).append(replacement);
			start = end + replacementLength;
			end = text.indexOf(searchString, start);
		}
		buf.append(text.substring(start));
		return buf.toString();
	}
	
	private String toSPARQLArgumentList(Collection<? extends RDFNode> values) {
		StringBuilder result = new StringBuilder();
		result.append('(');
		boolean isFirst = true;
		for (RDFNode term: values) {
			if (!isFirst) {
				result.append(", ");
			}
			if (term.isURIResource()) {
				result.append('<');
				result.append(term.asResource().getURI());
				result.append('>');
			} else {
				throw new IllegalArgumentException(
						"toSPARQLArgumentList is only implemented for URIs; " + 
						"called with term " + term);
			}
			isFirst = false;
		}
		result.append(')');
		return result.toString();
	}
}
