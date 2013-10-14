package de.fuberlin.wiwiss.pubby;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A data source backed by a Jena model.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class ModelDataSource implements DataSource {
	private Model model;

	public ModelDataSource(Model model) {
		this.model = model;
	}

	@Override
	public String getEndpointURL() {
		return null;
	}
	
	@Override
	public Model getResourceDescription(String resourceURI) {
		return model;
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
			boolean isInverse, boolean describeAnonymous) {
		return model;
	}
	
	@Override
	public List<Resource> getIndex() {
		List<Resource> result = new ArrayList<Resource>();
		ResIterator it = model.listSubjects();
		while (it.hasNext()) {
			Resource r = it.next();
			if (r.isAnon()) continue;
			result.add(r);
			if (result.size() >= DataSource.MAX_INDEX_SIZE) break; 
		}
		return result;
	}
}
