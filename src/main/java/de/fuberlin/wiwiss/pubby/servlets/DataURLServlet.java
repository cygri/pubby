package de.fuberlin.wiwiss.pubby.servlets;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.HypermediaResource;
import de.fuberlin.wiwiss.pubby.MappedResource;
import de.fuberlin.wiwiss.pubby.ModelResponse;
import de.fuberlin.wiwiss.pubby.ResourceDescription;
import de.fuberlin.wiwiss.pubby.vocab.FOAF;

/**
 * Servlet for serving RDF documents containing a description
 * of a given resource.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class DataURLServlet extends BaseURLServlet {
	
	@Override
	protected boolean doGet(HypermediaResource controller,
			Collection<MappedResource> resources,
			HttpServletRequest request, 
			HttpServletResponse response,
			Configuration config) throws IOException {

		ResourceDescription description = getResourceDescription(controller, resources);
		// Check if resource exists in dataset
		if (description == null) {
			response.setStatus(404);
			response.setContentType("text/plain");
			response.getOutputStream().println("Nothing known about <" + controller.getAbsoluteIRI() + ">");
			return true;
		}
		Model model = description.getModel();
		
		// Add links to RDF documents with descriptions of the blank nodes
		Resource r = model.getResource(controller.getAbsoluteIRI());
		StmtIterator it = r.listProperties();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			if (!stmt.getObject().isAnon()) continue;
			String pathDataURL = controller.getPathDataURL(stmt.getPredicate());
			if (pathDataURL == null) continue;
			((Resource) stmt.getResource()).addProperty(RDFS.seeAlso, 
					model.createResource(pathDataURL));
		}
		it = model.listStatements(null, null, r);
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			if (!stmt.getSubject().isAnon()) continue;
			String pathDataURL = controller.getInversePathDataURL(stmt.getPredicate());
			if (pathDataURL == null) continue;
			((Resource) stmt.getSubject().as(Resource.class)).addProperty(RDFS.seeAlso, 
					model.createResource(pathDataURL));
		}
		
		// Add document metadata
		if (model.qnameFor(FOAF.primaryTopic.getURI()) == null
				&& model.getNsPrefixURI("foaf") == null) {
			model.setNsPrefix("foaf", FOAF.NS);
		}
		if (model.qnameFor(RDFS.label.getURI()) == null
				&& model.getNsPrefixURI("rdfs") == null) {
			model.setNsPrefix("rdfs", RDFS.getURI());
		}
		Resource document = model.getResource(addQueryString(controller.getDataURL(), request));
		document.addProperty(FOAF.primaryTopic, r);
		document.addProperty(RDFS.label, 
				"RDF description of " + description.getTitle());
		
		// Add provenance. This seems out of place here.
		for (MappedResource resource: resources) {
			resource.getDataset().addDocumentMetadata(model, document);
			resource.getDataset().addMetadataFromTemplate(model, resource, getServletContext());
		}

		ModelResponse server = new ModelResponse(model, request, response);
		server.serve();
		return true;
	}
	
	private static final long serialVersionUID = 6825866213915066364L;
}