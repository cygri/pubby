package de.fuberlin.wiwiss.pubby.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.HypermediaControls;
import de.fuberlin.wiwiss.pubby.ModelResponse;
import de.fuberlin.wiwiss.pubby.ResourceDescription;

/**
 * Servlet for serving RDF documents containing a description
 * of a given resource.
 */
public class DataURLServlet extends BaseServlet {
	
	@Override
	protected boolean doGet(String relativeURI,
			HttpServletRequest request, 
			HttpServletResponse response,
			Configuration config) throws IOException {
		HypermediaControls controller = config.getControls(relativeURI, false);

		ResourceDescription description = controller == null ? 
				null : controller.getResourceDescription();
		// Check if resource exists in dataset
		if (description == null) {
			response.setStatus(404);
			response.setContentType("text/plain");
			response.getOutputStream().println("Nothing known about <" + controller.getAbsoluteIRI() + ">");
			return true;
		}
		Model model = description.getModel();
		
		addHighDegreePropertyLinks(model, controller);
		
		addDocumentMetadata(model, controller, 
				addQueryString(controller.getDataURL(), request),
				"RDF description of " + description.getTitle());
		
		ModelResponse server = new ModelResponse(model, request, response);
		server.serve();
		return true;
	}
	
	private void addHighDegreePropertyLinks(Model model, HypermediaControls controller) {
		// TODO: This should re-use the logic from ResourceDescription and ResourceProperty to decide where to create these links
		// Add links to RDF documents with descriptions of the blank nodes
		Resource r = model.getResource(controller.getAbsoluteIRI());
		StmtIterator it = r.listProperties();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			if (!stmt.getObject().isAnon()) continue;
			String pathDataURL = controller.getValuesDataURL(stmt.getPredicate());
			if (pathDataURL == null) continue;
			((Resource) stmt.getResource()).addProperty(RDFS.seeAlso, 
					model.createResource(pathDataURL));
		}
		it = model.listStatements(null, null, r);
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			if (!stmt.getSubject().isAnon()) continue;
			String pathDataURL = controller.getInverseValuesDataURL(stmt.getPredicate());
			if (pathDataURL == null) continue;
			((Resource) stmt.getSubject().as(Resource.class)).addProperty(RDFS.seeAlso, 
					model.createResource(pathDataURL));
		}
	}
	
	private static final long serialVersionUID = 6825866213915066364L;
}