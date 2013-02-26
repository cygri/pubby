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

		Model description = getResourceDescription(resources);

		// Check if resource exists in dataset
		if (description.size() == 0) {
			response.setStatus(404);
			response.setContentType("text/plain");
			response.getOutputStream().println("Nothing known about <" + controller.getAbsoluteIRI() + ">");
			return true;
		}
		
		// Add links to RDF documents with descriptions of the blank nodes
		Resource r = description.getResource(controller.getAbsoluteIRI());
		StmtIterator it = r.listProperties();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			if (!stmt.getObject().isAnon()) continue;
			String pathDataURL = controller.getPathDataURL(stmt.getPredicate());
			((Resource) stmt.getResource()).addProperty(RDFS.seeAlso, 
					description.createResource(pathDataURL));
		}
		it = description.listStatements(null, null, r);
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			if (!stmt.getSubject().isAnon()) continue;
			String pathDataURL = controller.getInversePathDataURL(stmt.getPredicate());
			((Resource) stmt.getSubject().as(Resource.class)).addProperty(RDFS.seeAlso, 
					description.createResource(pathDataURL));
		}
		
		// Add document metadata
		if (description.qnameFor(FOAF.primaryTopic.getURI()) == null
				&& description.getNsPrefixURI("foaf") == null) {
			description.setNsPrefix("foaf", FOAF.NS);
		}
		if (description.qnameFor(RDFS.label.getURI()) == null
				&& description.getNsPrefixURI("rdfs") == null) {
			description.setNsPrefix("rdfs", RDFS.getURI());
		}
		Resource document = description.getResource(addQueryString(controller.getDataURL(), request));
		document.addProperty(FOAF.primaryTopic, r);
		document.addProperty(RDFS.label, 
				"RDF description of " + 
				new ResourceDescription(controller, description, config).getLabel());
		
		// Add provenance. This seems out of place here.
		for (MappedResource resource: resources) {
			resource.getDataset().addDocumentMetadata(description, document);
			resource.getDataset().addMetadataFromTemplate(description, resource, getServletContext());
		}

		ModelResponse server = new ModelResponse(description, request, response);
		server.serve();
		return true;
	}
	
	private static final long serialVersionUID = 6825866213915066364L;
}