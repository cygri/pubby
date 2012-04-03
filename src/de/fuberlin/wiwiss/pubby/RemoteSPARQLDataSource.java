package de.fuberlin.wiwiss.pubby;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.vocabulary.RDF;

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
	private String defaultGraphName;
	private String webResourcePrefix;
	private String webBase;
	private String previousDescribeQuery;
	
	public RemoteSPARQLDataSource(String endpointURL, String graphName, String webBase, String webResourcePrefix) {
		this.endpointURL = endpointURL;
		this.defaultGraphName = graphName;
		this.webBase = webBase;
		this.webResourcePrefix = webResourcePrefix;
	}
	
	public String getEndpointURL() {
		return endpointURL;
	}
	
	public String getResourceDescriptionURL(String resourceURI) {
		try {
			StringBuffer result = new StringBuffer();
			result.append(endpointURL);
			result.append("?");
			if (defaultGraphName != null) {
				result.append("default-graph-uri=");
				result.append(URLEncoder.encode(defaultGraphName, "utf-8"));
				result.append("&");
			}
			result.append("query=");
			result.append(URLEncoder.encode(buildDescribeQuery(resourceURI), "utf-8"));
			return result.toString();
		} catch (UnsupportedEncodingException ex) {
			// can't happen, utf-8 is always supported
			throw new RuntimeException(ex);
		}
	}

	private String buildDescribeQuery(String resourceURI) {
		String index = Configuration.buildIndexResource(this.webBase, this.webResourcePrefix);
		if (index.equals(resourceURI)) {
			return "CONSTRUCT { ?s <" + RDF.type.getURI() + "> ?o } WHERE { ?s <" + RDF.type.getURI() + "> ?o } LIMIT 1000";
		} else {
			return "DESCRIBE <" + resourceURI + ">";
		}
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
	
	public String getPreviousDescribeQuery() {
		return previousDescribeQuery;
	}
	
	private Model execDescribeQuery(String query) {
		previousDescribeQuery = query;
		QueryEngineHTTP endpoint = new QueryEngineHTTP(endpointURL, query);
		if (defaultGraphName != null) {
			endpoint.setDefaultGraphURIs(Collections.singletonList(defaultGraphName));
		}
		
		//FIXME: more elegant way to discriminate query execution 
		//		 (lateral effect from introducing CONSTRUCT query for listing all resources on index)
		if (query.startsWith("CONSTRUCT")) {
			Model model = endpoint.execConstruct();
			Property containerOf = model.createProperty("http://rdfs.org/sioc/ns#container_of");
			ResIterator iter = model.listSubjects();
			Resource index = model.createResource(Configuration.buildIndexResource(this.webBase, this.webResourcePrefix));
			while (iter.hasNext()) {
				Resource o = iter.next();
				if (!index.equals(o)) {
					Statement statement = model.createStatement(index, containerOf, o);
					model.add(statement);
				}
			}
			model.addLiteral(index, 
							 model.createProperty("http://www.w3.org/2000/01/rdf-schema#label"), 
							 model.createLiteral("Synthetic container for listing by default all resources on pubby"));
			return model;
		} else {
			return endpoint.execDescribe();
		}
	}
}
