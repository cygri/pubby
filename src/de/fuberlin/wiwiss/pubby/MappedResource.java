package de.fuberlin.wiwiss.pubby;

import com.hp.hpl.jena.rdf.model.Property;

/**
 * A resource that is mapped between the SPARQL dataset and the Web server.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class MappedResource {
	private String relativeWebURI;
	private String datasetURI;
	private Configuration config;
	
	public MappedResource(String relativeWebURI, String datasetURI, Configuration config) {
		this.relativeWebURI = relativeWebURI;
		this.datasetURI = datasetURI;
		this.config = config;
	}

	/**
	 * @return the resource's URI within the SPARQL dataset
	 */
	public String getDatasetURI() {
		return datasetURI;
	}
	
	/**
	 * @return the resource's URI on the public Web server
	 */
	public String getWebURI() {
		return config.getWebApplicationBaseURI() + 
				config.getWebResourcePrefix() + relativeWebURI;
	}
	
	/**
	 * @return the HTML page describing the resource on the public Web server
	 */
	public String getPageURL() {
		return config.getWebApplicationBaseURI() + "page/" + relativeWebURI;
	}
	
	/**
	 * @return the RDF document describing the resource on the public Web server
	 */
	public String getDataURL() {
		return config.getWebApplicationBaseURI() + "data/" + relativeWebURI;
	}
	
	public String getPathPageURL(Property property) {
		return getPathURL("pathpage/", property);
	}
	
	public String getPathDataURL(Property property) {
		return getPathURL("pathdata/", property);
	}
	
	public String getInversePathPageURL(Property property) {
		return getPathURL("pathpage/-", property);
	}
	
	public String getInversePathDataURL(Property property) {
		return getPathURL("pathdata/-", property);
	}
	
	private String getPathURL(String urlPrefix, Property property) {
		if (config.getPrefixes().qnameFor(property.getURI()) == null) {
			return null;
		}
		return config.getWebApplicationBaseURI() + urlPrefix +
				config.getPrefixes().qnameFor(property.getURI()) + "/" +
				relativeWebURI;
	}
}
