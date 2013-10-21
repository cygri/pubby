package de.fuberlin.wiwiss.pubby;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

import de.fuberlin.wiwiss.pubby.sources.RemoteSPARQLDataSource;
import de.fuberlin.wiwiss.pubby.vocab.CONF;
import de.fuberlin.wiwiss.pubby.vocab.META;

public class MetadataConfiguration extends ResourceReader {
	private final static String metadataPlaceholderURIPrefix = "about:metadata:";

	private final Resource customTemplate;
	private final String metadataTemplate;
	private final RemoteSPARQLDataSource sparqlDataSource;
	
	public MetadataConfiguration(Resource dataset) {
		this(dataset, null);
	}
	
	public MetadataConfiguration(Resource dataset, 
			RemoteSPARQLDataSource sparqlDataSource) {
		super(dataset);
		this.customTemplate = getResource(CONF.rdfDocumentMetadata);
		this.metadataTemplate = getIRI(CONF.metadataTemplate);
		this.sparqlDataSource = sparqlDataSource;
	}
	
	public void addCustomMetadata(Model document, Resource documentResource) {
		if (customTemplate == null) return;
		StmtIterator it = customTemplate.listProperties();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			document.add(documentResource, stmt.getPredicate(), stmt.getObject());
		}
		it = customTemplate.getModel().listStatements(null, null, customTemplate);
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			if (stmt.getPredicate().equals(CONF.rdfDocumentMetadata)) {
				continue;
			}
			document.add(stmt.getSubject(), stmt.getPredicate(), documentResource);
		}
	}
	
	public Resource addMetadataFromTemplate(Model document, HypermediaResource controller) {
		if (metadataTemplate == null) {
			return null;
		}

		Calendar currentTime;
		Resource currentDocRepr;
		currentTime = Calendar.getInstance();
		
		// add metadata from templates
		Model tplModel = FileManager.get().loadModel(metadataTemplate);

		// iterate over template statements to replace placeholders
		Model metadata = ModelFactory.createDefaultModel();
		currentDocRepr = metadata.createResource();
		StmtIterator it = tplModel.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			Resource subj = stmt.getSubject();
			Property pred = stmt.getPredicate();
			RDFNode  obj  = stmt.getObject();
			
			try {
				if (subj.toString().contains(metadataPlaceholderURIPrefix)){
					subj = (Resource) parsePlaceholder(subj, controller, currentTime, currentDocRepr);
					if (subj == null) {
						// create a unique blank node with a fixed id.
						subj = getModel().createResource(new AnonId(String.valueOf(stmt.getSubject().hashCode())));
					}
				}
				
				if (obj.toString().contains(metadataPlaceholderURIPrefix)){
					obj = parsePlaceholder(obj, controller, currentTime, currentDocRepr);
				}
				
				// only add statements with some objects
				if (obj != null) {
					stmt = metadata.createStatement(subj,pred,obj);
					metadata.add(stmt);
				}
			} catch (Exception e) {
				// something went wrong, oops - lets better remove the offending statement
				metadata.remove(stmt);
				e.printStackTrace();
			}
		}
		
		// remove blank nodes that don't have any properties
		boolean changes = true;
		while ( changes ) {
			changes = false;
			StmtIterator stmtIt = metadata.listStatements();
			List<Statement> remList = new ArrayList<Statement>();
			while (stmtIt.hasNext()) {
				Statement s = stmtIt.nextStatement();
				if (    s.getObject().isAnon()
				     && ! ((Resource) s.getObject().as(Resource.class)).listProperties().hasNext() ) {
					remList.add(s);
					changes = true;
				}
			}
			metadata.remove(remList);
		}

		if (document != null) {
			document.add( metadata );
		}

		return currentDocRepr;
	}
	
	private RDFNode parsePlaceholder(RDFNode phRes, HypermediaResource controller, 
			Calendar currentTime, Resource currentDocRepr) {
		String phURI = phRes.asNode().getURI();
		// get package name and placeholder name from placeholder URI
		phURI = phURI.replace(metadataPlaceholderURIPrefix, "");
		String phPackage = phURI.substring(0, phURI.indexOf(":")+1);
		String phName = phURI.replace(phPackage, "");
		phPackage = phPackage.replace(":", "");
		Resource dataset = getSelf();
		
		if (phPackage.equals("runtime")) {
			// <about:metadata:runtime:query> - the SPARQL Query used to get the RDF Graph
			if (phName.equals("query")) {
				if (sparqlDataSource == null) return null;
				return getModel().createTypedLiteral(sparqlDataSource.getPreviousDescribeQuery());
			}
			// <about:metadata:runtime:time> - the current time
			if (phName.equals("time")) {
				return getModel().createTypedLiteral(currentTime);
			}
			// <about:metadata:runtime:graph> - URI of the graph
			if (phName.equals("graph")) {
				// Replaced the commented line by the following one because the
				// RDF graph we want to talk about is a specific representation
				// of the data identified by the getDataURL() URI.
				//                                       Olaf, May 28, 2010
				// return model.createResource(describedResource.getDataURL());
				return currentDocRepr;
			}
			// <about:metadata:runtime:data> - URI of the data
			if (phName.equals("data")) {
				return getModel().createResource(controller.getDataURL());
			}
			// <about:metadata:runtime:resource> - URI of the resource
			if (phName.equals("resource")) {
				return getModel().createResource(controller.getAbsoluteIRI());
			}
		}
		
		// <about:metadata:config:*> - The configuration parameters
		if (phPackage.equals("config")) {
			// look for requested property in the dataset config
			Property p  = getModel().createProperty(CONF.NS + phName);
			if (dataset.hasProperty(p))
				return dataset.getProperty(p).getObject();
			
			// find pointer to the global configuration set...
			StmtIterator it = dataset.getModel().listStatements(null, CONF.dataset, dataset);
			Statement ptrStmt = it.nextStatement();
			if (ptrStmt == null) return null;
			
			// look in global config if nothing found so far
			Resource globalConfig = ptrStmt.getSubject();
			if (globalConfig.hasProperty(p))
				return globalConfig.getProperty(p).getObject();
		}
		
		// <about:metadata:metadata:*> - The metadata provided by users
		if (phPackage.equals("metadata")) {
			// look for requested property in the dataset config
			Property p  = getModel().createProperty(META.NS + phName);
			if (dataset.hasProperty(p))
				return dataset.getProperty(p).getObject();
			
			// find pointer to the global configuration set...
			StmtIterator it = dataset.getModel().listStatements(null, CONF.dataset, dataset);
			Statement ptrStmt = it.nextStatement();
			if (ptrStmt == null) return null;
			
			// look in global config if nothing found so far
			Resource globalConfig = ptrStmt.getSubject();
			if (globalConfig.hasProperty(p))
				return globalConfig.getProperty(p).getObject();
		}

		return getModel().createResource(new AnonId(String.valueOf(phRes.hashCode())));
	}
}	
