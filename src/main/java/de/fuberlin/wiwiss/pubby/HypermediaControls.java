package de.fuberlin.wiwiss.pubby;

import java.net.URI;
import java.net.URISyntaxException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;

import de.fuberlin.wiwiss.pubby.sources.DataSource;

/**
 * The hypermedia interface to a specific resource. The resource may be
 * hosted by the server or merely made browsable by the server.
 */
public class HypermediaControls {
	
	/**
	 * Creates a new hypermedia resource from a Pubby path representing a
	 * resource.
	 * @param path A Pubby path, either absolute or relative
	 * @param config The server's configuration
	 * @return An object providing hypermedia controls for the resource, or
	 *     null if this is not a Pubby-browsable resource.
	 */
	public static HypermediaControls createFromPubbyPath(String path, Configuration config) {
		try {
			if (new URI(path).isAbsolute()) {
				return createFromIRI(
						PubbyIRIEscaper.unescapeSpecialCharacters(path), config);
			} else {
				return createFromIRI(config.getWebApplicationBaseURI() + 
						config.getWebResourcePrefix() + path, config);
			}
		} catch (URISyntaxException ex) {
			return null;
		}
	}
	
	/**
	 * Creates a new hypermedia resource from an absolute IRI.
	 * @param absoluteIRI An IRI identifying the resource
	 * @param config The server's configuration
	 * @return An object providing hypermedia controls for the resource, or
	 *     null if this is not a Pubby-browsable resource.
	 */
	public static HypermediaControls createFromIRI(String absoluteIRI, Configuration config) {
		if (absoluteIRI == null) return null;
		if (!config.isBrowsable(absoluteIRI)) return null;
		return new HypermediaControls(absoluteIRI, config);
	}
	
	private final String absoluteIRI;
	private final boolean isHosted;
	private final Configuration config;

	private HypermediaControls(String absoluteIRI, Configuration config) {
		this.absoluteIRI = absoluteIRI;
		this.config = config;
		this.isHosted = absoluteIRI.startsWith(config.getWebApplicationBaseURI());
	}
	
	/**
	 * A version of the resource's IRI suitable for use in constructing Pubby
	 * path URLs. If the resource is Pubby-hosted, this will be its IRI
	 * relative to the resource base
	 * (<code>conf:webBase + conf:webResourcePrefix</code>). Otherwise, it
	 * will be its full absolute IRI with special characters escaped to make
	 * it safe in constructing paths.
	 * @return the path
	 */
	private String getPubbyPath() {
		if (isHosted) {
			int resourceBaseLength = config.getWebApplicationBaseURI().length() + 
					config.getWebResourcePrefix().length();
			return absoluteIRI.substring(resourceBaseLength);
		} else {
			return PubbyIRIEscaper.escapeSpecialCharacters(absoluteIRI);
		}
	}
	
	/**
	 * Is the resource itself hosted by Pubby, that is, is it in Pubby's
	 * web base namespace? (If not, then Pubby merely provides a hypermedia
	 * browsing interface for the resource, but cannot be said to host 
	 * the resource itself.)
	 */
	public boolean isHosted() {
		return isHosted;
	}
	
	/**
	 * @return the resource's absolute IRI
	 */
	public String getAbsoluteIRI() {
		return absoluteIRI;
	}
	
	/**
	 * @return Best hyperlink for this resource (Pubby page if available).
	 */
	public String getBrowsableURL() {
		if (isHosted) {
			return absoluteIRI;
		} else {
			return getPageURL();
		}
	}
	
	/**
	 * @return A description of the resource that allows interrogation of the data
	 */
	public ResourceDescription getResourceDescription() {
		DataSource source = config.getDataSource();
		Model model = source.describeResource(absoluteIRI);
		if (model.isEmpty()) return null;
		return new ResourceDescription(this, model, 
				source.getHighIndegreeProperties(absoluteIRI), 
				source.getHighOutdegreeProperties(absoluteIRI), 
				config);
	}
	
	/**
	 * @return the HTML page describing the resource on the public Web server
	 */
	public String getPageURL() {
		return config.getWebApplicationBaseURI() + "page/" + getPubbyPath();
	}
	
	/**
	 * @return the RDF document describing the resource on the public Web server
	 */
	public String getDataURL() {
		return config.getWebApplicationBaseURI() + "data/" + getPubbyPath();

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
			String encoded = PubbyIRIEscaper.escapeSpecialCharacters(property.getURI());
			return config.getWebApplicationBaseURI() + urlPrefix +
					"!" + encoded + "///" +
					getPubbyPath();
		}
		return config.getWebApplicationBaseURI() + urlPrefix +
				config.getPrefixes().qnameFor(property.getURI()) + "/" +
				getPubbyPath();
	}
}
