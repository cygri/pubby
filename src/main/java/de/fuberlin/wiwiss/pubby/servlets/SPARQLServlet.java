package de.fuberlin.wiwiss.pubby.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.vocabulary.DCTerms;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.HypermediaControls;
import de.fuberlin.wiwiss.pubby.ResourceDescription;
import de.fuberlin.wiwiss.pubby.ResourceDescription.Value;

public class SPARQLServlet extends HttpServlet {
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

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		if (initError != null) {
			sendInitialization500(resp, initError);
			return;
		}
		String title = req.getParameter("title");
		if (title == null) {
			title = "Search Results";
		}
		String query = req.getParameter("query");
		if (query == null) {
			send404(resp, null);
		}
		String q = req.getParameter("q");
		if (q != null) {
			// TODO SPARQL injection :-(
			if (q.contains(" ")) {
				q = "'" + q + "'";
			}
			query = query.replace("?q", "\"" + q + "\"");
		}
		String endpointURL = "http://localhost:8890/sparql";
		System.out.println(query);
		QueryEngineHTTP endpoint = new QueryEngineHTTP(endpointURL, query);
		ResultSet rs = endpoint.execSelect();
		List<Value> results = new ArrayList<Value>();
		HypermediaControls controller = HypermediaControls.createFromIRI("http://planning.derilinx.ie:8080/sparql", config);
		ResourceDescription desc = new ResourceDescription(controller, ModelFactory.createDefaultModel(), config);
		while (rs.hasNext()) {
			RDFNode node = rs.next().get("result");
			if (node.isURIResource() && node.asResource().getURI().startsWith("http://planning.derilinx.ie/")) {
				node = desc.getModel().createResource(node.asResource().getURI().replace("planning.derilinx.ie", "planning.derilinx.ie:8080"));
			}
			results.add(desc.new Value(node, DCTerms.hasPart, config.getVocabularyStore()));
		}

		VelocityHelper template = new VelocityHelper(getServletContext(), resp);
		Context context = template.getVelocityContext();
		context.put("project_name", config.getProjectName());
		context.put("project_link", config.getProjectLink());
		context.put("server_base", config.getWebApplicationBaseURI());
		context.put("title", title);
		context.put("results", results);
//		context.put("head_title", resource.getTitle() + " \u00BB " + property.getCompleteLabel());
//		context.put("property", property);
//		context.put("back_uri", controller.getBrowsableURL());
//		context.put("back_label", resource.getTitle());
		context.put("showLabels", config.showLabels());

		template.renderXHTML("query-result.vm");
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
}
