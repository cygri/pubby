package de.fuberlin.wiwiss.pubby;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

import de.fuberlin.wiwiss.pubby.sources.DataSource;
import de.fuberlin.wiwiss.pubby.vocab.CONF;

/**
 * A store for labels, descriptions and other metadata of classes and
 * properties. Values are retrieved from a {@link DataSource} and
 * cached.
 *
 * TODO: This is not i18n aware. Needs ability to cache one label/desc per language, and return Literals incl language tag
 * 
 * @author Kai Eckert (kai@informatik.uni-mannheim.de)
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class VocabularyStore {
	private DataSource dataSource;
	private String defaultLanguage = "en";
	
	/**
	 * Needs to be set before the instance is used! This is to allow creation
	 * of the store before the dataset is fully assembled.
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public void setDefaultLanguage(String defaultLanguage) {
		this.defaultLanguage = defaultLanguage;
	}
	
	private final StringValueCache labels = new StringValueCache(RDFS.label, false);
	private final StringValueCache pluralLabels = new StringValueCache(CONF.pluralLabel, false);
	private final StringValueCache inverseLabels = new StringValueCache(RDFS.label, true);
	private final StringValueCache inversePluralLabels = new StringValueCache(CONF.pluralLabel, true);
	private final StringValueCache descriptions = new StringValueCache(RDFS.comment, false);
	private final IntegerValueCache weights = new IntegerValueCache(CONF.weight, false);
	private final CachedPropertyCollection highIndegreeProperties = new CachedPropertyCollection(CONF.HighIndregreeProperty);
	private final CachedPropertyCollection highOutdegreeProperties = new CachedPropertyCollection(CONF.HighOutdregreeProperty);
	
	public String getLabel(String iri, boolean preferPlural) {
		return getLabel(iri, preferPlural, defaultLanguage);
	}

	public String getLabel(String iri, boolean preferPlural, String language) {
		// TODO: Use language!
		if (preferPlural) {
			String pluralLabel = pluralLabels.get(iri);
			return pluralLabel == null ? getLabel(iri, false) : pluralLabel;
		}
		return labels.get(iri);
	}

	public String getInverseLabel(String iri, boolean preferPlural) {
		return getInverseLabel(iri, preferPlural, defaultLanguage);
	}
	
	public String getInverseLabel(String iri, boolean preferPlural, String language) {
		// TODO: Use language!
		if (preferPlural) {
			String pluralLabel = inversePluralLabels.get(iri);
			return pluralLabel == null ? getLabel(iri, false) : pluralLabel;
		}
		return inverseLabels.get(iri);
	}
	
	public String getDescription(String iri) {
		return getDescription(iri, defaultLanguage);
	}

	public String getDescription(String iri, String language) {
		// TODO: Use language!
		return descriptions.get(iri);
	}

	public int getWeight(Property property) {
		Integer result = weights.get(property.getURI());
		return result == null ? 0 : result.intValue();
	}

	public CachedPropertyCollection getHighIndegreeProperties() {
		return highIndegreeProperties;
	}

	public CachedPropertyCollection getHighOutdegreeProperties() {
		return highOutdegreeProperties;
	}

	public class CachedPropertyCollection {
		private final Resource type;
		private Collection<Property> cache = null;
		CachedPropertyCollection(Resource type) {
			this.type = type;
		}
		public Collection<Property> get() {
			if (cache != null) return cache;
			cache = new ArrayList<Property>();
			Model result = dataSource.listPropertyValues(type.getURI(), RDF.type, true);
			StmtIterator it = result.listStatements(null, RDF.type, type);
			while (it.hasNext()) {
				Resource r = it.next().getSubject();
				if (!r.isURIResource()) continue;
				cache.add(r.as(Property.class));
			}
			return cache;
		}
	}
	
	private abstract class ValueCache<K> {
		private final Property property;
		private final boolean inverse;
		private final Map<String, K> cache = new HashMap<String, K>();
		ValueCache(Property property, boolean inverse) {
			this.property = property;
			this.inverse = inverse;
		}
		abstract K pickBestValue(Set<RDFNode> candidates);
		K get(String iri) {
			if (cache.containsKey(iri)) {
				return cache.get(iri);
			}
			K best = null;
			if (dataSource.canDescribe(iri)) {
				best = pickBestFromModel(dataSource.describeResource(iri), iri);
			}
			cache.put(iri, best);
			return best;
		}
		private K pickBestFromModel(Model m, String iri) {
			Resource r = m.getResource(iri);
			Set<RDFNode> nodes = inverse ? getInverseValues(r) : getValues(r);
			return pickBestValue(nodes);
		}
		private Set<RDFNode> getValues(Resource r) {
			Set<RDFNode> nodes = new HashSet<RDFNode>();
			StmtIterator it = r.listProperties(property);
			while (it.hasNext()) {
				nodes.add(it.next().getObject());
			}
			return nodes;
		}
		private Set<RDFNode> getInverseValues(Resource r) {
			Set<RDFNode> nodes = new HashSet<RDFNode>();
			StmtIterator it = r.listProperties(OWL.inverseOf);
			while (it.hasNext()) {
				RDFNode object = it.next().getObject();
				if (!object.isResource()) continue;
				StmtIterator it2 = object.asResource().listProperties(property);
				while (it2.hasNext()) {
					nodes.add(it2.next().getObject());
				}
			}
			return nodes;
		}
	}

	private class StringValueCache extends ValueCache<String> {
		StringValueCache(Property p, boolean inverse) { super(p, inverse); }
		@Override
		String pickBestValue(Set<RDFNode> candidates) {
			for (RDFNode node: candidates) {
				if (!node.isLiteral()) continue;
				Literal l = node.asLiteral();
				String dt = l.getDatatypeURI();
				if (dt == null || dt.equals(XSD.xstring.getURI()) || dt.equals(RDF.getURI() + "langString")) {
					return l.getLexicalForm();
				}
			}
			return null;
		}
	}

	private class IntegerValueCache extends ValueCache<Integer> {
		IntegerValueCache(Property p, boolean inverse) { super(p, inverse); }
		@Override
		Integer pickBestValue(Set<RDFNode> candidates) {
			for (RDFNode node: candidates) {
				if (!node.isLiteral()) continue;
				try {
					return node.asLiteral().getInt();
				} catch (JenaException ex) {
					continue;
				}
			}
			return null;
		}
	}
}
