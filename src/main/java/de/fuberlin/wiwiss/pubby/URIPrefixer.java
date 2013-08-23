package de.fuberlin.wiwiss.pubby;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Helper class that splits URIs into prefix and local name
 * according to a Jena PrefixMapping.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class URIPrefixer {
	private final Resource resource;
	private final String prefix;
	private final String localName;

	public URIPrefixer(String uri, SimplePrefixMapping prefixes) {
		this(ResourceFactory.createResource(uri), prefixes);
	}
	
	public URIPrefixer(Resource resource, SimplePrefixMapping prefixes) {
		this.resource = resource;
		String uri = resource.getURI();
		String namespaceURI = prefixes.getNamespace(uri);
		if (namespaceURI != null) {
			localName = uri.substring(namespaceURI.length());;
			prefix = prefixes.lookupPrefix(namespaceURI);
			
		} else {
			prefix = null;
			localName = null;
		}
	}
	
	public boolean hasPrefix() {
		return prefix != null;
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public String getLocalName() {
		if (localName == null) {
			Matcher matcher = Pattern.compile("([^#/:?]+)[#/:?]*$").matcher(resource.getURI());
			if (matcher.find()) {
				return matcher.group(1);
			}
			return "";	// Only happens if the URI contains only excluded chars
		}
		return localName;
	}
	
	public String toTurtle() {
		if (hasPrefix()) {
			return getPrefix() + ":" + getLocalName();
		}
		return "<" + resource.getURI() + ">";
	}
}
