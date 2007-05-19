package de.fuberlin.wiwiss.pubby.servlets;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class OutputRequestParamHandler {
	private static final String ATTRIBUTE_NAME_IS_HANDLED =
		"OutputRequestParamHandler.isHandled";
	private final static HashMap mimeTypes = new HashMap();
	static {
		mimeTypes.put("n3", "text/n3;charset=utf-8");
		mimeTypes.put("rdfxml", "application/rdf+xml");
	}
	
	private final HttpServletRequest request;
	private final String requestedType;

	public OutputRequestParamHandler(HttpServletRequest request) {
		this.request = request;
		requestedType = identifyRequestedType(request.getParameter("output"));
	}
	
	public boolean isMatchingRequest() {
		if ("true".equals(request.getAttribute(ATTRIBUTE_NAME_IS_HANDLED))) {
			return false;
		}
		return requestedType != null;
	}

	public HttpServletRequest getModifiedRequest() {
		return new WrappedRequest();
	}
	
	private String identifyRequestedType(String parameterValue) {
		if (mimeTypes.containsKey(parameterValue)) {
			return parameterValue;
		}
		return null;
	}
	
	private class WrappedRequest extends HttpServletRequestWrapper {
		WrappedRequest() {
			super(request);
			setAttribute(ATTRIBUTE_NAME_IS_HANDLED, "true");
		}
		public String getHeader(String name) {
			if ("accept".equals(name.toLowerCase())) {
				return (String) mimeTypes.get(requestedType);
			}
			return super.getHeader(name);
		}
		public Enumeration getHeaderNames() {
			final Enumeration realHeaders = super.getHeaderNames();
			return new Enumeration() {
				private String prefetched = null;
				public boolean hasMoreElements() {
					while (prefetched == null && realHeaders.hasMoreElements()) {
						String next = (String) realHeaders.nextElement();
						if (!"accept".equals(next.toLowerCase())) {
							prefetched = next;
						}
					}
					return (prefetched != null);
				}
				public Object nextElement() {
					return prefetched;
				}
			};
		}
		public Enumeration getHeaders(String name) {
			if ("accept".equals(name.toLowerCase())) {
				Vector v = new Vector();
				v.add(getHeader(name));
				return v.elements();
			}
			return super.getHeaders(name);
		}
	}
}
