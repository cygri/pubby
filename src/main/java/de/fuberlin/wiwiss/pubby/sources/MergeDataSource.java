package de.fuberlin.wiwiss.pubby.sources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;

import de.fuberlin.wiwiss.pubby.ModelUtil;

/**
 * A {@link DataSource} that presents an RDF merge of multiple other
 * data sources. Can also be initialized with a prefix mapping that will
 * be guaranteed to be defined on all result models.
 */
public class MergeDataSource implements DataSource {
	private final Collection<DataSource> sources;
	private final PrefixMapping prefixes;
	
	public MergeDataSource(Collection<DataSource> sources) {
		this(sources, new PrefixMappingImpl());
	}
	
	public MergeDataSource(Collection<DataSource> sources, PrefixMapping prefixes) {
		this.sources = sources;
		this.prefixes = prefixes;
	}
	
	@Override
	public boolean canDescribe(String absoluteIRI) {
		for (DataSource source: sources) {
			if (source.canDescribe(absoluteIRI)) return true;
		}
		return false;
	}
	
	@Override
	public Model describeResource(String iri) {
		Model result = ModelFactory.createDefaultModel();
		for (DataSource source: sources) {
			if (!source.canDescribe(iri)) continue;
			ModelUtil.mergeModels(result, source.describeResource(iri));
		}
		ModelUtil.mergePrefixes(result, prefixes);
		return result;
	}

	@Override
	public Map<Property, Integer> getHighIndegreeProperties(String resourceIRI) {
		Map<Property, Integer> result = new HashMap<Property, Integer>();
		for (DataSource source: sources) {
			result = addIntegerMaps(result, 
					source.getHighIndegreeProperties(resourceIRI));
		}
		return result;
	}

	@Override
	public Map<Property, Integer> getHighOutdegreeProperties(String resourceIRI) {
		Map<Property, Integer> result = new HashMap<Property, Integer>();
		for (DataSource source: sources) {
			result = addIntegerMaps(result, 
					source.getHighOutdegreeProperties(resourceIRI));
		}
		return result;
	}

	@Override
	public Model listPropertyValues(String resourceIRI, Property property,
			boolean isInverse) {
		Model result = ModelFactory.createDefaultModel();
		for (DataSource source: sources) {
			ModelUtil.mergeModels(result, source.describeResource(resourceIRI));
		}
		ModelUtil.mergePrefixes(result, prefixes);
		return result;
	}

	@Override
	public List<Resource> getIndex() {
		List<Resource> result = new ArrayList<Resource>();
		for (DataSource source: sources) {
			result.addAll(source.getIndex());
		}
		return result;
	}

	private <K> Map<K, Integer> addIntegerMaps(Map<K, Integer> map1, Map<K, Integer> map2) {
		if (map1 == null) return map2;
		if (map2 == null) return map1;
		for (K key: map2.keySet()) {
			int value = map2.get(key);
			if (value == 0) continue;
			map1.put(key, map1.containsKey(key) ? map1.get(key) + value : value);
		}
		return map1;
	}
}
