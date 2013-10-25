package de.fuberlin.wiwiss.pubby;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
	
	private final I18nStringValueCache labels = new I18nStringValueCache(RDFS.label, false);
	private final I18nStringValueCache pluralLabels = new I18nStringValueCache(CONF.pluralLabel, false);
	private final I18nStringValueCache inverseLabels = new I18nStringValueCache(RDFS.label, true);
	private final I18nStringValueCache inversePluralLabels = new I18nStringValueCache(CONF.pluralLabel, true);
	private final I18nStringValueCache descriptions = new I18nStringValueCache(RDFS.comment, false);
	private final IntegerValueCache weights = new IntegerValueCache(CONF.weight, false);
	private final CachedPropertyCollection highIndegreeProperties = new CachedPropertyCollection(CONF.HighIndregreeProperty);
	private final CachedPropertyCollection highOutdegreeProperties = new CachedPropertyCollection(CONF.HighOutdregreeProperty);
	
	public Literal getLabel(String iri, boolean preferPlural) {
		return getLabel(iri, preferPlural, defaultLanguage);
	}

	public Literal getLabel(String iri, boolean preferPlural, String language) {
		if (preferPlural) {
			Literal pluralLabel = pluralLabels.get(iri, language);
			return pluralLabel == null ? getLabel(iri, false, language) : pluralLabel;
		}
		return labels.get(iri, language);
	}

	public Literal getInverseLabel(String iri, boolean preferPlural) {
		return getInverseLabel(iri, preferPlural, defaultLanguage);
	}
	
	public Literal getInverseLabel(String iri, boolean preferPlural, String language) {
		if (preferPlural) {
			Literal pluralLabel = inversePluralLabels.get(iri, language);
			return pluralLabel == null ? getInverseLabel(iri, false, language) : pluralLabel;
		}
		return inverseLabels.get(iri, language);
	}
	
	public Literal getDescription(String iri) {
		return getDescription(iri, defaultLanguage);
	}

	public Literal getDescription(String iri, String language) {
		return descriptions.get(iri, language);
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
			if (dataSource == null) return Collections.emptyList();
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
		public void reportAdditional(Property p) {
			if (cache.contains(p)) return;
			cache.add(p);
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

// Currently not needed -- all strings are i18n
//	private class StringValueCache extends ValueCache<String> {
//		StringValueCache(Property p, boolean inverse) { super(p, inverse); }
//		@Override
//		String pickBestValue(Set<RDFNode> candidates) {
//			for (RDFNode node: candidates) {
//				if (!node.isLiteral()) continue;
//				Literal l = node.asLiteral();
//				String dt = l.getDatatypeURI();
//				if (dt == null || dt.equals(XSD.xstring.getURI()) || dt.equals(RDF.getURI() + "langString")) {
//					return l.getLexicalForm();
//				}
//			}
//			return null;
//		}
//	}

	private class I18nStringValueCache extends ValueCache<Collection<Literal>> {
		I18nStringValueCache(Property p, boolean inverse) {
			super (p, inverse);
		}
		Literal get(String iri, String preferredLang) {
			Literal bestMatch = null;
			int bestMatchLength = -1;
			for (Literal l: get(iri)) {
				int matchLength = getMatchLength(l.getLanguage(), preferredLang);
				if (matchLength >= bestMatchLength) {
					bestMatch = l;
					bestMatchLength = matchLength;
				}
			}
			return bestMatch;
		}
		private int getMatchLength(String langTag1, String langTag2) {
			// TODO: This is very dodgy. It reports a decent match between "xx" and "xxx". Requires some research to do properly.
			int i = 0;
			while (i < langTag1.length() && i < langTag2.length()) {
				char c1 = langTag1.charAt(i);
				char c2 = langTag2.charAt(i);
				if (Character.toLowerCase(c1) != Character.toLowerCase(c2)) break;
				i++;
			}
			return i;
		}
		
		@Override
		Collection<Literal> pickBestValue(Set<RDFNode> candidates) {
			Collection<Literal> result = new ArrayList<Literal>(candidates.size());
			for (RDFNode node: candidates) {
				if (!node.isLiteral()) continue;
				Literal l = node.asLiteral();
				String dt = l.getDatatypeURI();
				if (dt == null || dt.equals(XSD.xstring.getURI()) || dt.equals(RDF.getURI() + "langString")) {
					result.add(l);
				}
			}
			return result;
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
