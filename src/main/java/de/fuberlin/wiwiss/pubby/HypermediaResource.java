package de.fuberlin.wiwiss.pubby;

import com.hp.hpl.jena.rdf.model.Property;

/**
 * The hypermedia interface to a specific resource managed by the server.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class HypermediaResource {
	private final String relativeIRI;
	private final Configuration config;
	
	public HypermediaResource(String relativeIRI, Configuration config) {
		this.relativeIRI = relativeIRI;
		this.config = config;
	}

	/**
	 * @return the resource's IRI, relative to the web server root
	 */
	public String getRelativeIRI() {
		return relativeIRI;
	}
	
	/**
	 * @return the resource's IRI on the public Web server
	 */
	public String getAbsoluteIRI() {
		return config.getWebApplicationBaseURI() +
				config.getWebResourcePrefix() + 
				relativeIRI;
	}
	
	/**
	 * @return the HTML page describing the resource on the public Web server
	 */
	public String getPageURL() {
		return config.getWebApplicationBaseURI() + "page/" + relativeIRI;
	}
	
	/**
	 * @return the RDF document describing the resource on the public Web server
	 */
	public String getDataURL() {
		return config.getWebApplicationBaseURI() + "data/" + relativeIRI;
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
	
	public String getValuesPageURL(Property property) {
		return getPathURL("values/", property);
	}
	
	public String getInverseValuesPageURL(Property property) {
		return getPathURL("values/-", property);
	}
	
	public String getValuesDataURL(Property property) {
		return getPathURL("values.data/", property);
	}
	
	public String getInverseValuesDataURL(Property property) {
		return getPathURL("values.data/-", property);
	}
	
	private String getPathURL(String urlPrefix, Property property) {
		if (config.getPrefixes().qnameFor(property.getURI()) == null) {
			return null;
		}
		return config.getWebApplicationBaseURI() + urlPrefix +
				config.getPrefixes().qnameFor(property.getURI()) + "/" +
				relativeIRI;
	}
}
