package de.fuberlin.wiwiss.pubby.negotiation;

import junit.framework.TestCase;

public class PubbyNegotiatorTest extends TestCase {
	private ContentTypeNegotiator negotiator;
	
	public void setUp() {
		negotiator = PubbyNegotiator.getPubbyNegotiator();
	}
	
	public void testAcceptRDFXML() {
		assertEquals("application/rdf+xml",
				negotiator.getBestMatch("application/rdf+xml").getMediaType());
	}
	
	public void testAcceptHTML() {
		assertEquals("text/html",
				negotiator.getBestMatch("text/html").getMediaType());
	}
	
	public void testAcceptXHTMLGetsHTML() {
		assertEquals("text/html",
				negotiator.getBestMatch("application/xhtml+xml").getMediaType());
	}
	
	public void testGetTurtle() {
		assertEquals("application/x-turtle",
				negotiator.getBestMatch("application/x-turtle").getMediaType());
		assertEquals("application/x-turtle",
				negotiator.getBestMatch("application/turtle").getMediaType());
		assertEquals("application/x-turtle",
				negotiator.getBestMatch("text/turtle").getMediaType());
	}
	
	public void testGetN3() {
		assertEquals("text/rdf+n3;charset=utf-8",
				negotiator.getBestMatch("text/rdf+n3").getMediaType());
		assertEquals("text/rdf+n3;charset=utf-8",
				negotiator.getBestMatch("text/n3").getMediaType());
		assertEquals("text/rdf+n3;charset=utf-8",
				negotiator.getBestMatch("application/n3").getMediaType());
	}
	
	public void testGetNTriples() {
		assertEquals("text/plain",
				negotiator.getBestMatch("text/plain").getMediaType());
	}
	
	public void testBrowsersGetHTML() {
		// Safari and Mozilla have text/html;q=0.9,text/plain;q=0.8,*/*;q=0.5
		// We want them to see HTML
		assertEquals("text/html", 
				negotiator.getBestMatch("text/html;q=0.9,text/plain;q=0.8,*/*;q=0.5").getMediaType());
	}
	
	public void testAcceptXMLGetsRDFXML() {
		assertEquals("application/rdf+xml",
				negotiator.getBestMatch("application/xml").getMediaType());
		assertEquals("application/rdf+xml",
				negotiator.getBestMatch("text/xml").getMediaType());
	}
	
	public void testNoAcceptGetsHTML() {
		assertEquals("text/html",
				negotiator.getBestMatch(null).getMediaType());
	}
	
	public void testSafariGetsHTML() {
		// Some versions of Safari send a broken "*/*" Accept header.
		// We must override this to send HTML.
		assertEquals("text/html",
				negotiator.getBestMatch("*/*", 
						"Mozilla/5.0 (Macintosh; U; Intel Mac OS X; en) " +
						"AppleWebKit/522.11.1 (KHTML, like Gecko) " +
						"Version/3.0.3 Safari/522.12.1").getMediaType());
	}
	
	public void testAcceptEverythingGetsHTML() {
		assertEquals("text/html", negotiator.getBestMatch("*/*").getMediaType());
	}
}
