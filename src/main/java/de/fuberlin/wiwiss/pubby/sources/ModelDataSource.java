package de.fuberlin.wiwiss.pubby.sources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import de.fuberlin.wiwiss.pubby.ModelUtil;

/**
 * A data source backed by a Jena model.
 */
public class ModelDataSource implements DataSource {
	private Model model;

	public ModelDataSource(Model model) {
		this.model = model;
	}

	@Override
	public boolean canDescribe(String absoluteIRI) {
		return true;
	}

	@Override
	public Model describeResource(String resourceURI) {
		System.out.println("Describing " + resourceURI);
		Resource r = ResourceFactory.createResource(resourceURI);
		if (model.contains(r, null, (RDFNode) null) || model.contains(null, null, r)) {
			return model;
		}
		return ModelUtil.EMPTY_MODEL;
	}
	
	@Override
	public Map<Property, Integer> getHighIndegreeProperties(String resourceURI) {
		return null;
	}

	@Override
	public Map<Property, Integer> getHighOutdegreeProperties(String resourceURI) {
		return null;
	}

	@Override
	public Model listPropertyValues(String resourceURI, Property property,
			boolean isInverse) {
		return model;
	}
	
	@Override
	public List<Resource> getIndex() {
		List<Resource> result = new ArrayList<Resource>();
		ResIterator subjects = model.listSubjects();
		while (subjects.hasNext() && result.size() < DataSource.MAX_INDEX_SIZE) {
			Resource r = subjects.next();
			if (r.isAnon()) continue;
			result.add(r);
		}
		NodeIterator objects = model.listObjects();
		while (objects.hasNext() && result.size() < DataSource.MAX_INDEX_SIZE) {
			RDFNode o = objects.next();
			if (!o.isURIResource()) continue;
			result.add(o.asResource());
		}
		return result;
	}
}
