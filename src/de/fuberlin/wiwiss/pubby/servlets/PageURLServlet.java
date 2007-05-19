package de.fuberlin.wiwiss.pubby.servlets;
import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.hp.hpl.jena.rdf.model.Model;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.MappedResource;
import de.fuberlin.wiwiss.pubby.ResourceDescription;

public class PageURLServlet extends BaseResourceServlet {

	public boolean doGet(MappedResource resource, 
			HttpServletRequest request,
			HttpServletResponse response,
			Configuration config) throws ServletException, IOException {

		Model description = getResourceDescription(resource.getDatasetURI());
		if (description.size() == 0) {
			return false;
		}

		ResourceDescription resourceDescription = new ResourceDescription(
				resource, description, config);
		String discoLink = "http://www4.wiwiss.fu-berlin.de/rdf_browser/?browse_uri=" +
				URLEncoder.encode(resource.getWebURI(), "utf-8");
		String tabulatorLink = "http://dig.csail.mit.edu/2005/ajar/ajaw/tab.html?uri=" +
				URLEncoder.encode(resource.getWebURI(), "utf-8");
		String openLinkLink = "http://dbpedia.openlinksw.com:8890/DAV/JS/rdfbrowser/index.html?uri=" +
				URLEncoder.encode(resource.getWebURI(), "utf-8");
		VelocityHelper template = new VelocityHelper(getServletContext(), response);
		Context context = template.getVelocityContext();
		context.put("project_name", config.getProjectName());
		context.put("project_link", config.getProjectLink());
		context.put("uri", resourceDescription.getURI());
		context.put("server_base", config.getWebApplicationBaseURI());
		context.put("rdf_link", resource.getDataURL());
		context.put("disco_link", discoLink);
		context.put("tabulator_link", tabulatorLink);
		context.put("openlink_link", openLinkLink);
		context.put("sparql_endpoint", config.getDataSource().getEndpointURL());
		context.put("title", resourceDescription.getLabel());
		context.put("comment", resourceDescription.getComment());
		context.put("image", resourceDescription.getImageURL());
		context.put("properties", resourceDescription.getProperties());
		template.renderXHTML("page.vm");
		return true;
	}
}