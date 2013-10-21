package de.fuberlin.wiwiss.pubby.servlets;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.HypermediaResource;

/**
 * Abstract base servlet for servlets that handle a property of a given
 * resource. The base servlet takes care of extracting the resource's URI
 * and the property's URI from the requested URL, and mapping everything to
 * the data sources. Concrete subclasses then take care of generating the
 * response.
 */
public abstract class ValuesBaseServlet extends BaseServlet {
	private static Pattern pattern = Pattern.compile("(-?)([^:/]*):([^:/]*)/(.*)");

	public abstract boolean doGet(HypermediaResource controller,
			Property property, boolean isInverse,
			HttpServletRequest request,
			HttpServletResponse response,
			Configuration config) throws IOException, ServletException;
	
	public boolean doGet(String relativeURI, 
			HttpServletRequest request,
			HttpServletResponse response,
			Configuration config) throws IOException, ServletException {
		Matcher matcher = pattern.matcher(relativeURI);
		if (!matcher.matches()) {
			return false;
		}
		boolean isInverse = "-".equals(matcher.group(1));
		String prefix = matcher.group(2);
		if (config.getPrefixes().getNsPrefixURI(prefix) == null) {
			return false;
		}
		String localName = matcher.group(3);
		relativeURI = matcher.group(4);	// Keep just last part
		Property property = ResourceFactory.createProperty(
				config.getPrefixes().getNsPrefixURI(prefix), localName);
		HypermediaResource controller = config.getController(relativeURI, false);
		if (controller == null) return false;
		return doGet(controller, property, isInverse, request, response, config);
	}

	private static final long serialVersionUID = 7393467141233996715L;
}