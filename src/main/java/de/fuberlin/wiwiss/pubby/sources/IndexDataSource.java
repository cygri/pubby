package de.fuberlin.wiwiss.pubby.sources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * A {@link DataSource} that wraps another data source and adds an
 * index of the resources in that data source.
 */
public class IndexDataSource implements DataSource {
	private final String indexIRI;
	private final DataSource wrapped;
	
	public IndexDataSource(String indexIRI, DataSource wrapped) {
		this.indexIRI = indexIRI;
		this.wrapped = wrapped;
	}
	
	@Override
	public boolean canDescribe(String absoluteIRI) {
		return indexIRI.equals(absoluteIRI) || wrapped.canDescribe(absoluteIRI);
	}
	
	@Override
	public Model describeResource(String iri) {
		if (!indexIRI.equals(iri)) return wrapped.describeResource(iri);
		Model result = ModelFactory.createDefaultModel();
		result.setNsPrefix("sioc", SIOC_NS);
		result.setNsPrefix("rdfs", RDFS.getURI());
		Resource index = result.createResource(indexIRI);
		// TODO: Get label from the vocabulary store, and make it i18nable
		index.addProperty(RDFS.label, "Index of Resources", "en");
		int count = 0;
		for (Resource r: wrapped.getIndex()) {
			if (index.equals(r)) continue;
			index.addProperty(siocContainerOf, r);
			count++;
		}
		if (count == 0) {
			index.addProperty(RDFS.comment, "The index is empty. " +
					"Perhaps a misconfiguration excludes all resources " +
					"from all datasets?", "en");
		}
		return result;
	}
	private final static String SIOC_NS = "http://rdfs.org/sioc/ns#";
	private final static Property siocContainerOf = 
			ResourceFactory.createProperty(SIOC_NS + "container_of");

	@Override
	public Map<Property, Integer> getHighIndegreeProperties(String resourceIRI) {
		return wrapped.getHighIndegreeProperties(resourceIRI);
	}

	@Override
	public Map<Property, Integer> getHighOutdegreeProperties(String resourceIRI) {
		return wrapped.getHighOutdegreeProperties(resourceIRI);
	}

	/**
	 * Describe the index resource, and extract all the statements that
	 * have our property and the right subject/object.
	 */
	@Override
	public Model listPropertyValues(String resourceIRI,
			Property property, boolean isInverse) {
		if (!indexIRI.equals(resourceIRI)) {
			return wrapped.listPropertyValues(resourceIRI, property, isInverse);
		}
		Model all = describeResource(resourceIRI);
		Resource r = all.getResource(resourceIRI);
		StmtIterator it = isInverse
				? all.listStatements(null, property, r)
				: all.listStatements(r, property, (RDFNode) null);
		Model result = ModelFactory.createDefaultModel();
		while (it.hasNext()) {
			result.add(it.next());
		}
		return result;
	}

	@Override
	public List<Resource> getIndex() {
		List<Resource> wrappedIndex = wrapped.getIndex();
		List<Resource> result = new ArrayList<Resource>(wrappedIndex.size() + 1);
		result.add(ResourceFactory.createResource(indexIRI));
		result.addAll(wrappedIndex);
		return result;
	}
}
