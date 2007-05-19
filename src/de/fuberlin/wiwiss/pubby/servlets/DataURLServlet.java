package de.fuberlin.wiwiss.pubby.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joseki.http.ModelResponse;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.MappedResource;
import de.fuberlin.wiwiss.pubby.ResourceDescription;
import de.fuberlin.wiwiss.pubby.vocab.FOAF;

public class DataURLServlet extends BaseResourceServlet {
	
	protected boolean doGet(MappedResource resource,
			HttpServletRequest request, 
			HttpServletResponse response,
			Configuration config) throws IOException {

		OutputRequestParamHandler handler = new OutputRequestParamHandler(request);
		if (handler.isMatchingRequest()) {
			request = handler.getModifiedRequest();
		}
		
		String datasetURI = resource.getDatasetURI();
		String webURI = resource.getWebURI();
		Model description = getResourceDescription(datasetURI);
		if (description.size() == 0) {
			response.setStatus(404);
			response.setContentType("text/plain");
			response.getOutputStream().println("Nothing known about <" + webURI + ">");
			return true;
		}
		Resource r = description.getResource(webURI);
		if (config.getAddSameAsStatements()) {
			r.addProperty(OWL.sameAs, description.createResource(datasetURI));
		}
		StmtIterator it = r.listProperties();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			if (!stmt.getObject().isAnon()) continue;
			String pathDataURL = resource.getPathDataURL(stmt.getPredicate());
			((Resource) stmt.getResource()).addProperty(RDFS.seeAlso, 
					description.createResource(pathDataURL));
		}
		it = description.listStatements(null, null, r);
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			if (!stmt.getSubject().isAnon()) continue;
			String pathDataURL = resource.getInversePathDataURL(stmt.getPredicate());
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
		Resource document = description.getResource(resource.getDataURL());
		document.addProperty(FOAF.primaryTopic, r);
		document.addProperty(RDFS.label, 
				"RDF description of " + 
				new ResourceDescription(resource, description, config).getLabel());
		config.addDocumentMetadata(description, document);

		new ModelResponse(description, request, response).serve();
		return true;
	}
}