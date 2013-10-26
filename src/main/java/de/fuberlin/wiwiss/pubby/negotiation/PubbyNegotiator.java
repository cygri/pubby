package de.fuberlin.wiwiss.pubby.negotiation;

import java.util.regex.Pattern;

public class PubbyNegotiator {
	private final static ContentTypeNegotiator pubbyNegotiator;
	private final static ContentTypeNegotiator dataNegotiator;
	
	static {
		pubbyNegotiator = new ContentTypeNegotiator();
		pubbyNegotiator.setDefaultAccept("text/html");
		
		// MSIE (7.0) sends either */*, or */* with a list of other random types,
		// but always without q values, so it doesn't provide any basis for
		// actual negotiation. We will simply send HTML to MSIE, no matter what.
		pubbyNegotiator.addUserAgentOverride(Pattern.compile("MSIE"), null, "text/html");
		
		// Send Turtle to clients that indicate they accept everything.
		// This is specifically so that cURL sees Turtle.
		//
		// NOTE: Rumor has it that some browsers send the Accept header
		//       "*/*" too, but I believe that's only when images or scripts
		//       are requested, not for normal web page requests.   --RC 
		pubbyNegotiator.addUserAgentOverride(null, "*/*", "text/turtle");

		pubbyNegotiator.addVariant("text/html;q=0.81")
				.addAliasMediaType("application/xhtml+xml;q=0.81");
		pubbyNegotiator.addVariant("application/rdf+xml")
				.addAliasMediaType("application/xml;q=0.45")
				.addAliasMediaType("text/xml;q=0.4");
		pubbyNegotiator.addVariant("text/rdf+n3;charset=utf-8;q=0.95")
				.addAliasMediaType("text/n3;q=0.5")
				.addAliasMediaType("application/n3;q=0.5");
		pubbyNegotiator.addVariant("application/x-turtle;q=0.95")
				.addAliasMediaType("application/turtle;q=0.8")
				.addAliasMediaType("text/turtle;q=0.5");
		pubbyNegotiator.addVariant("text/plain;q=0.2");

		dataNegotiator = new ContentTypeNegotiator();
		dataNegotiator.addVariant("application/rdf+xml;q=0.99")
				.addAliasMediaType("application/xml;q=0.45")
				.addAliasMediaType("text/xml;q=0.4");
		dataNegotiator.addVariant("text/rdf+n3;charset=utf-8")
				.addAliasMediaType("text/n3;q=0.5")
				.addAliasMediaType("application/n3;q=0.5");
		dataNegotiator.addVariant("application/x-turtle;q=0.99")
				.addAliasMediaType("application/turtle;q=0.8")
				.addAliasMediaType("text/turtle;q=0.5");
		dataNegotiator.addVariant("text/plain;q=0.2");
	}
	
	public static ContentTypeNegotiator getPubbyNegotiator() {
		return pubbyNegotiator;
	}
	
	public static ContentTypeNegotiator getDataNegotiator() {
		return dataNegotiator;
	}
}
