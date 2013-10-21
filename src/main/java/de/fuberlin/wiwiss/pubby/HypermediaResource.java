package de.fuberlin.wiwiss.pubby;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;

import de.fuberlin.wiwiss.pubby.sources.DataSource;

/**
 * The hypermedia interface to a specific resource hosted by the server.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class HypermediaResource {
	private final String relativeIRI;
	private final Configuration config;
	
	/**
	 * @param relativeIRI A relative IRI for the resource
	 * @param isRelativeToPubbyRoot If true, the IRI is relative to the Pubby
	 *        root (<code>conf:webBase</code>); otherwise, the IRI is relative
	 *        to some non-resource namespace such as <code>/page/</code>. The
	 *        distinction matters if <code>conf:webResourcePrefix</code> is set.
	 * @param config
	 */
	public HypermediaResource(String relativeIRI, boolean isRelativeToPubbyRoot,
			Configuration config) {
		if (isRelativeToPubbyRoot) {
			if (!relativeIRI.startsWith(config.getWebResourcePrefix())) {
				throw new IllegalArgumentException(
						"Relative IRI <" + relativeIRI +
						"> was expected to start with webResourcePrefix <" + 
						config.getWebResourcePrefix() + ">");
			}
			this.relativeIRI = relativeIRI;
		} else {
			this.relativeIRI = config.getWebResourcePrefix() + relativeIRI;
		}
		this.config = config;
	}

	/**
	 * @param absoluteIRI The absolute IRI of the resource; must be in the Pubby resource namespace
	 * @param config
	 */
	public HypermediaResource(String absoluteIRI, Configuration config) {
		String base = config.getWebApplicationBaseURI() + config.getWebResourcePrefix();
		if (!absoluteIRI.startsWith(base)) {
			throw new IllegalArgumentException(
					"Absolute IRI <" + absoluteIRI +
					"> was expected to start with <" + base + ">");
		}
		this.relativeIRI = absoluteIRI.substring(
				config.getWebApplicationBaseURI().length());
		this.config = config;
	}
	
	/**
	 * @return the resource's IRI, relative to the Pubby root (<code>conf:webBase</code>)
	 */
	public String getRelativeIRI() {
		return relativeIRI;
	}
	
	/**
	 * @return the resource's IRI on the public Web server
	 */
	public String getAbsoluteIRI() {
		return config.getWebApplicationBaseURI() + relativeIRI;
	}
	
	/**
	 * @return A description of the resource that allows interrogation of the data
	 */
	public ResourceDescription getResourceDescription() {
		DataSource source = config.getDataSource();
		Model model = source.describeResource(getAbsoluteIRI());
		if (model.isEmpty()) return null;
		return new ResourceDescription(this, model, 
				source.getHighIndegreeProperties(getAbsoluteIRI()), 
				source.getHighOutdegreeProperties(getAbsoluteIRI()), 
				config);
	}
	
	/**
	 * Strips the <code>conf:webResourcePrefix</code> from the beginning of
	 * an IRI, if one is configured.
	 * @throw IllegalArgumentException if relativeIRI does not start with webResourcePrefix
	 */
	private String stripWebResourcePrefix(String relativeIRI) {
		return relativeIRI.substring(config.getWebResourcePrefix().length()); 
	}
	
	/**
	 * @return the HTML page describing the resource on the public Web server
	 */
	public String getPageURL() {
		return config.getWebApplicationBaseURI() + "page/" +
				stripWebResourcePrefix(relativeIRI);
	}
	
	/**
	 * @return the RDF document describing the resource on the public Web server
	 */
	public String getDataURL() {
		return config.getWebApplicationBaseURI() + "data/" +
				stripWebResourcePrefix(relativeIRI);

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
				stripWebResourcePrefix(relativeIRI);
	}
}
