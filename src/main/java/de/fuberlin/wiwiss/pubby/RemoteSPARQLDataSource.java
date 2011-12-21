package de.fuberlin.wiwiss.pubby;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.sparql.engine.http.HttpQuery;

/**
 * A data source backed by a SPARQL endpoint accessed through
 * the SPARQL protocol.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author Sergio Fernández (sergio.fernandez@fundacionctic.org)
 * @version $Id$
 */
public class RemoteSPARQLDataSource implements DataSource {
	
	private String endpointURL;
	private String defaultGraphURI;
	private String webResourcePrefix;
	private String webBase;
	private String previousDescribeQuery;
	
	private String[] resourceQueries;
	private String[] anonPropertyQueries;
	private String[] anonInversePropertyQueries;
	
	public RemoteSPARQLDataSource(String endpointURL, String defaultGraphURI,
			String webBase, String webResourcePrefix) {
		this(endpointURL, defaultGraphURI, webBase, webResourcePrefix, null, null, null);
	}
	
	public RemoteSPARQLDataSource(String endpointURL, String defaultGraphURI, 
			String webBase, String webResourcePrefix,
			String[] resourceQueries, String[] anonPropertyQueries, String[] anonInversePropertyQueries) {
		this.endpointURL = endpointURL;
		this.defaultGraphURI = defaultGraphURI;
		this.webBase = webBase;
		this.webResourcePrefix = webResourcePrefix;

		// If no resource description queries given, set them to the default.
		if (resourceQueries==null){
			resourceQueries = new String[1];
			resourceQueries[0] = "DESCRIBE ?__this__";
		}

		// If no anonymous property description queries given, set them to the default.
		if (anonPropertyQueries==null){
			anonPropertyQueries = new String[1];
			anonPropertyQueries[0] = "DESCRIBE ?x WHERE {?__this__ ?__property__ ?x. FILTER (isBlank(?x))}";
		}

		// If no anonymous inverse property description queries given, set them to the default.
		if (anonInversePropertyQueries==null){
			anonInversePropertyQueries = new String[1];
			anonInversePropertyQueries[0] = "DESCRIBE ?x WHERE {?__property__ ?__this__ ?x. FILTER (isBlank(?x))}";
		}

		this.resourceQueries = resourceQueries;
		this.anonPropertyQueries = anonPropertyQueries;
		this.anonInversePropertyQueries = anonInversePropertyQueries;
	}
	
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
			result.append(URLEncoder.encode(preProcessQuery(resourceQueries[0], resourceURI), "utf-8"));
			return result.toString();
		} catch (UnsupportedEncodingException ex) {
			// can't happen, utf-8 is always supported
			throw new RuntimeException(ex);
		}
	}

	private String buildIndexQuery(String resourceURI) {
		return "CONSTRUCT { ?s <" + RDF.type.getURI() + "> ?o } WHERE { ?s <" + RDF.type.getURI() + "> ?o } LIMIT 1000";
	}
	
	public Model getResourceDescription(String resourceURI) {
		String index = Configuration.buildIndexResource(this.webBase, this.webResourcePrefix);
		if (index.equals(resourceURI)) {
			return execIndexQuery(buildIndexQuery(resourceURI));
		} else {
			// Loop over resource description queries, join results in a single model.
			// Process each query to replace place-holders of the given resource.
			Model model = executeQuery(preProcessQuery(resourceQueries[0], resourceURI));
			for (int i=1; i<resourceQueries.length; i++){
				model.add(executeQuery(preProcessQuery(resourceQueries[i], resourceURI)));
			}
			return model;
		}
	}

	public Model getAnonymousPropertyValues(String resourceURI, Property property, boolean isInverse) {
		
		// Loop over anonymous property description queries, join results in a single model.
		// Process each query to replace place-holders of the given resource and property.
		String[] queries = isInverse ? anonInversePropertyQueries : anonPropertyQueries;
		Model model = executeQuery(preProcessQuery(queries[0], resourceURI, property));
		for (int i=1; i<queries.length; i++){
			model.add(executeQuery(preProcessQuery(queries[i], resourceURI, property)));
		}
		return model;
	}
	
	public String getPreviousDescribeQuery() {
		return previousDescribeQuery;
	}
	
	private Model executeQuery(String queryString) {

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
		httpQuery.setAccept("application/rdf+xml");
		
		Model model = ModelFactory.createDefaultModel();
		InputStream in = httpQuery.exec();
		model.read(in, null);
		return model;
	}
	
	private String preProcessQuery(String query, String resourceURI){
		return preProcessQuery(query, resourceURI, null);
	}
	

	private Model execIndexQuery(String query) {
		Model model = executeQuery(query);
		Property siocContainerOf = model.createProperty("http://rdfs.org/sioc/ns#container_of");
		ResIterator iter = model.listSubjects();
		Resource index = model.createResource(Configuration.buildIndexResource(this.webBase, this.webResourcePrefix));
		while (iter.hasNext()) {
			Resource o = iter.nextResource();
			if (!index.equals(o)) {
				Statement statement = model.createStatement(index, siocContainerOf, o);
				model.add(statement);
			}
		}
		model.addLiteral(index, 
						 model.createProperty("http://www.w3.org/2000/01/rdf-schema#label"), 
						 model.createLiteral("Synthetic container for listing all resources available on this dataset"));
		return model;
	}

	private String preProcessQuery(String query, String resourceURI, Property property){
		
		String result = replaceString(query, "?__this__", "<" + resourceURI + ">");
		if (property!=null){
			result = replaceString(result, "?__property__", "<" + property.getURI() + ">");
		}
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
}
