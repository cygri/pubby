package de.fuberlin.wiwiss.pubby.servlets;
import java.io.IOException;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joseki.Joseki;
import org.joseki.http.AcceptItem;
import org.joseki.http.AcceptList;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.MappedResource;

/**
 * Servlet that handles the public URIs of mapped resources.
 * It redirects either to the page URL or to the data URL,
 * based on content negotiation.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class WebURIServlet extends BaseServlet {

	private static String contentTypeHTML = "text/html";
	private static String contentTypeXHTML = "application/xhtml+xml";

	private static AcceptItem defaultContentType = new AcceptItem(contentTypeHTML);
    
	private static AcceptList supportedContentTypes = new AcceptList(new String[]{
			// HTML types
			contentTypeHTML,
			contentTypeXHTML,

			// RDF types should mirror Joseki's accepted formats from ResponseHttp.java
//			Joseki.contentTypeXML,
			Joseki.contentTypeRDFXML,
			Joseki.contentTypeTurtle,
			Joseki.contentTypeAppN3,
			Joseki.contentTypeTextN3,
			Joseki.contentTypeNTriples
	});

	public boolean doGet(String relativeURI, HttpServletRequest request,
			HttpServletResponse response, Configuration config) throws IOException {
		String prefix = config.getWebResourcePrefix();
		if (!"".equals(prefix)) {
			if (!relativeURI.startsWith(prefix)) {
				return false;
			}
			relativeURI = relativeURI.substring(prefix.length());
		}
		MappedResource resource = config.getMappedResourceFromRelativeWebURI(relativeURI);
		response.setStatus(303);
		if (clientPrefersHTML(request)) {
			response.addHeader("Location", resource.getPageURL());
		} else if (config.redirectRDFRequestsToEndpoint()) {
			response.addHeader("Location", 
					config.getDataSource().getResourceDescriptionURL(resource.getDatasetURI()));	
		} else {
			response.addHeader("Location", resource.getDataURL());
		}
		return true;
	}
	
	private boolean clientPrefersHTML(HttpServletRequest request) {
		// This should use Joseki's HttpUtils.chooseContentType(...), but it
		// is buggy, so we use our own implementation until Joseki is fixed.
		// The Joseki version ignores ";q=x.x" values in the "Accept:" header.
		AcceptItem bestFormat = chooseContentType(
				request, supportedContentTypes, defaultContentType);

		return contentTypeHTML.equals(bestFormat.getAcceptType())
				|| contentTypeXHTML.equals(bestFormat.getAcceptType());
	}

	private AcceptItem chooseContentType(HttpServletRequest request, 
			AcceptList offeredContentTypes, AcceptItem defaultContentType) {
		String acceptHeader = request.getHeader("Accept");
		if (acceptHeader == null) {
			return defaultContentType;
		}
		AcceptList acceptedContentTypes = new AcceptList(acceptHeader);
		AcceptItem bestMatch = match(acceptedContentTypes, offeredContentTypes) ;
		if (bestMatch == null) {
			return defaultContentType;
		}
		return bestMatch;
	}
	
	private AcceptItem match(AcceptList accepted, AcceptList offered) {
        for ( Iterator iter = accepted.iterator() ; iter.hasNext() ; )
        {
            AcceptItem i2 = (AcceptItem)iter.next() ;
            AcceptItem m = offered.match(i2) ;
            if ( m != null )
                return m ;
        }
        return null ;
	}
}