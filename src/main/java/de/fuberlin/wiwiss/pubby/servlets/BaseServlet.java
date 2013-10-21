package de.fuberlin.wiwiss.pubby.servlets;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.Dataset;
import de.fuberlin.wiwiss.pubby.HypermediaResource;
import de.fuberlin.wiwiss.pubby.MetadataConfiguration;
import de.fuberlin.wiwiss.pubby.ModelUtil;
import de.fuberlin.wiwiss.pubby.vocab.FOAF;

/**
 * An abstract base servlet for servlets that manage a namespace of resources.
 * This class handles preprocessing of the request to extract the
 * resource URI relative to the namespace root, and manages the
 * {@link Configuration} instance shared by all servlets.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public abstract class BaseServlet extends HttpServlet {
	private Configuration config;
	private String initError;
	
	public void init() throws ServletException {
		config = (Configuration) getServletContext().getAttribute(
				ServletContextInitializer.SERVER_CONFIGURATION);
		if (config == null) {
			initError = (String) getServletContext().getAttribute(
					ServletContextInitializer.ERROR_MESSAGE);
		}
	}
	
	// TODO: This should be somewhere else, doesn't fit here
	protected void addDocumentMetadata(Model model, HypermediaResource controller,
			String documentURL, String title) {
		ModelUtil.addNSIfUndefined(model, "foaf", FOAF.getURI());
		ModelUtil.addNSIfUndefined(model, "rdfs", RDFS.getURI());

		// Add document metadata
		Resource topic = model.getResource(controller.getAbsoluteIRI());
		Resource document = model.getResource(documentURL);
		document.addProperty(FOAF.primaryTopic, topic);
		document.addProperty(RDFS.label, title);
		
		// Add custom metadata
		for (Dataset dataset: config.getDatasets()) {
			MetadataConfiguration metadata = dataset.getMetadataConfiguration();
			metadata.addCustomMetadata(model, document);
			metadata.addMetadataFromTemplate(model, controller);
		}
	}

	// TODO: This should be somewhere else, doesn't fit here
	protected void addPageMetadata(Context context, 
			HypermediaResource controller, PrefixMapping prefixes) {
		try {
			Model metadataModel = ModelFactory.createDefaultModel();
			for (Dataset dataset: config.getDatasets()) {
				MetadataConfiguration metadata = dataset.getMetadataConfiguration();
				Resource document = metadata.addMetadataFromTemplate(metadataModel, controller);
				// Replaced the commented line by the following one because the
				// RDF graph we want to talk about is a specific representation
				// of the data identified by the getDataURL() URI.
				//                                       Olaf, May 28, 2010
				// context.put("metadata", metadata.getResource(resource.getDataURL()));
				context.put("metadata", document);
			}

			Map<String,String> nsSet = metadataModel.getNsPrefixMap();
			nsSet.putAll(prefixes.getNsPrefixMap());
			context.put("prefixes", nsSet.entrySet());
			context.put("blankNodesMap", new HashMap<Resource,String>());
		}
		catch (Exception e) {
			context.put("metadata", Boolean.FALSE);
		}
	}
	
	protected abstract boolean doGet(
			String relativeURI,
			HttpServletRequest request,
			HttpServletResponse response,
			Configuration config) throws IOException, ServletException;
	
	public void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		if (initError != null) {
			sendInitialization500(response, initError);
			return;
		}
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
	
	protected void sendInitialization500(HttpServletResponse response, String message) throws IOException {
		response.setStatus(500);
		VelocityHelper template = new VelocityHelper(getServletContext(), response);
		Context context = template.getVelocityContext();
		context.put("message", message);
		context.put("title", "Configuration error");
		template.renderXHTML("500init.vm");
	}
	
	protected String addQueryString(String dataURL, HttpServletRequest request) {
		if (request.getParameter("output") == null) {
			return dataURL;
		}
		return dataURL + "?output=" + request.getParameter("output");
	}

	private static final long serialVersionUID = 7594710471966527559L;
}