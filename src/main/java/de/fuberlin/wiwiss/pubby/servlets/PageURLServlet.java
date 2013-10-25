package de.fuberlin.wiwiss.pubby.servlets;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.HypermediaControls;
import de.fuberlin.wiwiss.pubby.ResourceDescription;

/**
 * A servlet for serving the HTML page describing a resource.
 * Invokes a Velocity template.
 */
public class PageURLServlet extends BaseServlet {

	public boolean doGet(String relativeURI,
			HttpServletRequest request,
			HttpServletResponse response,
			Configuration config) throws ServletException, IOException {

		HypermediaControls controller = config.getControls(relativeURI, false);
		if (controller == null) return false;
		ResourceDescription description = controller.getResourceDescription();
		if (description == null) return false;
		
		VelocityHelper template = new VelocityHelper(getServletContext(), response);
		Context context = template.getVelocityContext();
		context.put("project_name", config.getProjectName());
		context.put("project_link", config.getProjectLink());
		context.put("uri", description.getURI());
		context.put("server_base", config.getWebApplicationBaseURI());
		context.put("rdf_link", controller.getDataURL());
		context.put("title", description.getTitle());
		context.put("comment", description.getComment());
		context.put("image", description.getImageURL());
		context.put("properties", description.getProperties());
		context.put("showLabels", config.showLabels());

		addPageMetadata(context, controller, description.getModel());
	
		template.renderXHTML("page.vm");
		return true;
	}

	private static final long serialVersionUID = 3363621132360159793L;
}
