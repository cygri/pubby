package de.fuberlin.wiwiss.pubby;

import java.util.HashMap;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.pubby.vocab.CONF;

/**
 * The vocabulary cache is used to store labels and descriptions
 * of classes and properties.
 *
 * TODO: This is not i18n aware. Needs ability to cache one label/desc per language, and return Literals incl language tag
 * @author Kai Eckert (kai@informatik.uni-mannheim.de)
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class VocabularyStore {
	private final Configuration configuration;
	private final Model store;
	private final Map<String, String> labelCache = new HashMap<String, String>();
	private final Map<String, String> descriptionCache = new HashMap<String, String>();
	private final Map<String, Integer> weightCache = new HashMap<String, Integer>();
	
	public VocabularyStore(Model model, Configuration configuration) {
		this.store = model;
		this.configuration = configuration;
	}
	
	public String getLabel(String uri) {
		return getLabel(uri, configuration.getDefaultLanguage());
	}

	public String getLabel(String uri, String language) {
		if (labelCache.containsKey(uri)) {
			return labelCache.get(uri);
		}
		String label = getProperty(uri, RDFS.label, null);
		labelCache.put(uri, label);
		return label;
	}

	public String getDescription(String uri) {
		return getDescription(uri, configuration.getDefaultLanguage());
	}

	public String getDescription(String uri, String language) {
		if (descriptionCache.containsKey(uri)) {
			return descriptionCache.get(uri);
		}
		String desc = getProperty(uri, RDFS.comment, null);
		descriptionCache.put(uri, desc);
		return desc;
	}

	public int getWeight(Property property) {
		if (weightCache.containsKey(property.getURI())) {
			return weightCache.get(property.getURI());
		}
		Resource r = store.getResource(property.getURI());
		int weight = r.hasProperty(CONF.weight) 
				? r.getProperty(CONF.weight).getInt() : 0;
		weightCache.put(property.getURI(), weight);
		return weight;
	}
	
	protected String getProperty(String uri, Property prop, String defaultValue) {
		String result;
		try {
			result = store.getResource(uri).getProperty(prop).getString();
		} catch (Throwable t) {
			result = defaultValue;
		}
		return result;
	}
}
