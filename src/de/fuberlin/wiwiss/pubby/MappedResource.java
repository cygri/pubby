package de.fuberlin.wiwiss.pubby;

import com.hp.hpl.jena.rdf.model.Property;

public class MappedResource {
	private String relativeWebURI;
	private String datasetURI;
	private Configuration config;
	
	public MappedResource(String relativeWebURI, String datasetURI, Configuration config) {
		this.relativeWebURI = relativeWebURI;
		this.datasetURI = datasetURI;
		this.config = config;
	}

	public String getDatasetURI() {
		return datasetURI;
	}
	
	public String getWebURI() {
		return config.getWebApplicationBaseURI() + 
				config.getWebResourcePrefix() + relativeWebURI;
	}
	
	public String getPageURL() {
		return config.getWebApplicationBaseURI() + "page/" + relativeWebURI;
	}
	
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
