package de.fuberlin.wiwiss.pubby;

/**
 * IRI rewriter that implements special behaviour for Pubby: Any IRI
 * within a certain namespace (the namespace that Pubby is serving)
 * will have characters that interfer with IRI derferencing %-escaped.
 * Any IRI outside that namespace is left alone. The escaped characters
 * are '#' and '?'.
 * 
 * This rewriter can also encode URIs to IRIs as part of the rewriting
 * process. Pubby always wants to work on IRIs. Most data sources use
 * IRIs. Some don't, and all characters outside US-ASCII appear %-encoded
 * in their IRIs. If configured to encode URIs to IRIs, then in the rewritten
 * identifiers we will see proper Unicode characters instead of %-encoded
 * sequences. Note that we do this only within the namespace.
 * 
 * TODO: The URI-to-IRI rewriting should probably be a separate class.
 */
public class PubbyIRIEscaper extends IRIRewriter {
	private final String namespace;
	private final boolean encodeURIsToIRIs;
	
	public PubbyIRIEscaper(String namespace, boolean encodeURIsToIRIs) {
		this.namespace = namespace;
		this.encodeURIsToIRIs = encodeURIsToIRIs;
	}
	
	@Override
	public String rewrite(String absoluteIRI) {
		if (absoluteIRI.startsWith(namespace)) {
			if (encodeURIsToIRIs) {
				// absoluteIRI is really a URI
				absoluteIRI = IRIEncoder.toIRI(absoluteIRI);
			}
			return escapeSpecialCharacters(absoluteIRI);
		}
		return absoluteIRI;
	}

	@Override
	public String unrewrite(String absoluteIRI) {
		if (absoluteIRI.startsWith(namespace)) {
			if (encodeURIsToIRIs) {
				absoluteIRI = IRIEncoder.toURI(absoluteIRI);
				// It's now really a URI, not an IRI
			}
			return unescapeSpecialCharacters(absoluteIRI);
		}
		return absoluteIRI;
	}
	
	/**
	 * Escapes any characters that have special meaning in IRIs so that
	 * they are safe for use in a Pubby path
	 */
	public static String escapeSpecialCharacters(String absoluteIRI) {
		return absoluteIRI.replace("#", "%23").replace("?", "%3F");		
	}

	/**
	 * Reverses the escaping done by {@link #unescapeSpecialCharacters(String)}.
	 */
	public static String unescapeSpecialCharacters(String absoluteIRI) {
		return absoluteIRI.replace("%23", "#").replace("%3F", "?");
	}
}
