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
public class IRITranslator {
	private final Dataset dataset;
	private final Configuration serverConfig;
	
	public IRITranslator(Dataset dataset, Configuration configuration) {
		this.dataset = dataset;
		this.serverConfig = configuration;
	}
	
	public Model getTranslated(Model model) {
		Model result = ModelFactory.createDefaultModel();
		for (String prefix: model.getNsPrefixMap().keySet()) {
			// Skip prefixes ns1, ns2, etc, which are usually
			// auto-assigned by the endpoint and do more harm than good
			if (prefix.matches("^ns[0-9]+$")) continue;

			String uri = model.getNsPrefixURI(prefix);
			result.setNsPrefix(prefix, getPublicURI(uri));
		}
		for (String prefix: serverConfig.getPrefixes().getNsPrefixMap().keySet()) {
			String uri = serverConfig.getPrefixes().getNsPrefixURI(prefix);
			result.setNsPrefix(prefix, uri);
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
		MappedResource resource = dataset.getMappedResourceFromDatasetURI(datasetURI, serverConfig);
		if (resource == null) return datasetURI;
		return resource.getController().getAbsoluteIRI();
	}
}
