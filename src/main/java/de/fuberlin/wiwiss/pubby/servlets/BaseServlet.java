package de.fuberlin.wiwiss.pubby.servlets;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.OWL;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.DataSource;
import de.fuberlin.wiwiss.pubby.HypermediaResource;
import de.fuberlin.wiwiss.pubby.IRITranslator;
import de.fuberlin.wiwiss.pubby.MappedResource;
import de.fuberlin.wiwiss.pubby.ModelUtil;
import de.fuberlin.wiwiss.pubby.ResourceDescription;

/**
 * An abstract base servlet for servlets that manage a namespace of resources.
 * This class handles preprocessing of the request to extract the
 * resource URI relative to the namespace root, and manages the
 * {@link Configuration} instance shared by all servlets.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 * @param <K>
 */
public abstract class BaseServlet extends HttpServlet {
	private final static String SERVER_CONFIGURATION =
		BaseServlet.class.getName() + ".serverConfiguration";
	
	private Configuration config;

	public void init() throws ServletException {
		synchronized (getServletContext()) {
			if (getServletContext().getAttribute(SERVER_CONFIGURATION) == null) {
				getServletContext().setAttribute(SERVER_CONFIGURATION, createServerConfiguration());
			}
		}
		config = 
			(Configuration) getServletContext().getAttribute(SERVER_CONFIGURATION);
	}

	private Configuration createServerConfiguration() throws UnavailableException {
		String param = getServletContext().getInitParameter("config-file");
		if (param == null) {
			throw new UnavailableException("Missing context parameter 'config-file'");
		}
		File configFile = new File(param);
		if (!configFile.isAbsolute()) {
			configFile = new File(getServletContext().getRealPath("/") + "/WEB-INF/" + param);
		}
		try {
			return new Configuration(
					FileManager.get().loadModel(
							configFile.getAbsoluteFile().toURI().toString()));
		} catch (JenaException ex) {
			throw new UnavailableException(
					"Error loading config file " + configFile.getAbsoluteFile().toURI() + ": " + ex.getMessage());
		}
	}
	
	protected ResourceDescription getResourceDescription(
			HypermediaResource controller,
			Collection<MappedResource> resources) {
		Model model = ModelFactory.createDefaultModel();
		Map<Property, Integer> highIndegreeProperties = new HashMap<Property, Integer>();
		Map<Property, Integer> highOutdegreeProperties = new HashMap<Property, Integer>();
		for (MappedResource resource: resources) {
			IRITranslator translator = new IRITranslator(resource.getDataset(),
					config);
			DataSource dataSource = resource.getDataset().getDataSource();
			Model originalDescription = dataSource.getResourceDescription(
							resource.getDatasetURI());
			Model translatedDescription = translator.getTranslated(originalDescription);

			// TODO: Extend IRITranslator so that it can translate the Property=>Integer map to take care of URI rewriting
			highIndegreeProperties = addIntegerMaps(highIndegreeProperties, 
					dataSource.getHighIndegreeProperties(resource.getDatasetURI()));
			highOutdegreeProperties = addIntegerMaps(highOutdegreeProperties, 
					dataSource.getHighOutdegreeProperties(resource.getDatasetURI()));

			// Add owl:sameAs statements referring to the original dataset URI
			// TODO: Make this a wrapper around DataSource
			if (resource.getDataset().getAddSameAsStatements()) {
				Resource r1 = translatedDescription.getResource(
						resource.getController().getAbsoluteIRI());
				Resource r2 = translatedDescription.getResource(
						resource.getDatasetURI());
				if (!r1.equals(r2)) {
					r1.addProperty(OWL.sameAs, r2);
					ModelUtil.addNSIfUndefined(translatedDescription, "owl", OWL.NS);
				}
			}
			ModelUtil.mergeModels(model, translatedDescription);
		}
		if (model.isEmpty()) return null;
		return new ResourceDescription(controller, model, 
				highIndegreeProperties, highOutdegreeProperties, config);
	}
	
	private <K> Map<K, Integer> addIntegerMaps(Map<K, Integer> map1, Map<K, Integer> map2) {
		if (map1 == null) return map2;
		if (map2 == null) return map1;
		for (K key: map2.keySet()) {
			int value = map2.get(key);
			if (value == 0) continue;
			map1.put(key, map1.containsKey(key) ? map1.get(key) + value : value);
		}
		return map1;
	}
	
	protected Model listPropertyValues(Collection<MappedResource> resources, 
			Property property, boolean isInverse, boolean describeAnonymous) {
		Model result = ModelFactory.createDefaultModel();
		for (MappedResource resource: resources) {
			ModelUtil.mergeModels(result, 
					new IRITranslator(resource.getDataset(), config).getTranslated(
							resource.getDataset().getDataSource().listPropertyValues(
									resource.getDatasetURI(), property, isInverse, 
									describeAnonymous)));
		}
		return result;
	}

	// TODO: This is crap. Should return an actual account of the provenance of all resources, regardless of data source
	protected String getFirstSPARQLEndpoint(Collection<MappedResource> resources) {
		for (MappedResource resource: resources) {
			if (resource.getDataset().getDataSource().getEndpointURL() != null) {
				return resource.getDataset().getDataSource().getEndpointURL();
			}
		}
		return null;
	}
	
	protected abstract boolean doGet(
			String relativeURI,
			HttpServletRequest request,
			HttpServletResponse response,
			Configuration config) throws IOException, ServletException;
	
	public void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		String relativeURI = request.getRequestURI().substring(
				request.getContextPath().length() + request.getServletPath().length());
		// Some servlet containers keep the leading slash, some don't
		if (!"".equals(relativeURI) && "/".equals(relativeURI.substring(0, 1))) {
			relativeURI = relativeURI.substring(1);
		}
		if (!doGet(relativeURI, request, response, config)) {
			send404(response, null);
		}
	}
	
	protected void send404(HttpServletResponse response, String resourceURI) throws IOException {
		response.setStatus(404);
		VelocityHelper template = new VelocityHelper(getServletContext(), response);
		Context context = template.getVelocityContext();
		context.put("project_name", config.getProjectName());
		context.put("project_link", config.getProjectLink());
		context.put("server_base", config.getWebApplicationBaseURI());
		context.put("title", "404 Not Found");
		if (resourceURI != null) {
			context.put("uri", resourceURI);
		}
		template.renderXHTML("404.vm");
	}
	
	protected String addQueryString(String dataURL, HttpServletRequest request) {
		if (request.getParameter("output") == null) {
			return dataURL;
		}
		return dataURL + "?output=" + request.getParameter("output");
	}

	private static final long serialVersionUID = 7594710471966527559L;
}