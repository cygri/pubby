package de.fuberlin.wiwiss.pubby;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * Translates an RDF model from the dataset into one fit for publication
 * on the server by replacing URIs, adding the correct prefixes etc. 
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class ModelTranslator {
	private final Model model;
	private final Dataset dataset;
	private final Configuration serverConfig;
	
	public ModelTranslator(Model model, Dataset dataset, Configuration configuration) {
		this.model = model;
		this.dataset = dataset;
		this.serverConfig = configuration;
	}
	
	public Model getTranslated() {
		Model result = ModelFactory.createDefaultModel();
		result.setNsPrefixes(model);
		for (String prefix: serverConfig.getPrefixes().getNsPrefixMap().keySet()) {
			result.setNsPrefix(prefix, serverConfig.getPrefixes().getNsPrefixURI(prefix));
		}
		StmtIterator it = model.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			Resource s = stmt.getSubject();
			if (s.isURIResource()) {
				s = result.createResource(getPublicURI(s.getURI()));
			}
			Property p = result.createProperty(
					getPublicURI(stmt.getPredicate().getURI()));
			RDFNode o = stmt.getObject();
			if (o.isURIResource()) {
				o = result.createResource(
						getPublicURI(((Resource) o).getURI()));
			}
			result.add(s, p, o);
		}
		return result;
	}
	
	private String getPublicURI(String datasetURI) {
		for (Dataset dataset: serverConfig.getDatasets()) {
			MappedResource resource = dataset.getMappedResourceFromDatasetURI(datasetURI, serverConfig);
			if (resource != null) 
				return resource.getController().getAbsoluteIRI();
		}
		return datasetURI;
	}
}
