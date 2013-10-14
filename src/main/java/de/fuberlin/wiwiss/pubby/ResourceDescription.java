package de.fuberlin.wiwiss.pubby;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * A convenient interface to an RDF description of a resource.
 * Provides access to its label, a textual comment, detailed
 * representations of its properties, and so on.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class ResourceDescription {
	private final HypermediaResource hypermediaResource;
	private final Model model;
	private final Resource resource;
	private final Configuration config;
	private final Map<Property, Integer> highIndegreeProperties;
	private final Map<Property, Integer> highOutdegreeProperties;
	private PrefixMapping prefixes = null;
	private List<ResourceProperty> properties = null;
	
	public ResourceDescription(HypermediaResource controller, Model model, 
			Configuration config) {
		this(controller, model, null, null, config);
   	}

	public ResourceDescription(HypermediaResource controller, Model model, 
			Map<Property, Integer> highIndegreeProperties,
			Map<Property, Integer> highOutdegreeProperties,
			Configuration config) {
		this.hypermediaResource = controller;
		this.model = model;
		this.resource = model.getResource(controller.getAbsoluteIRI());
		this.config = config;
		this.highIndegreeProperties = highIndegreeProperties == null ?
				Collections.<Property, Integer>emptyMap() : highIndegreeProperties;
		this.highOutdegreeProperties = highOutdegreeProperties == null ?
				Collections.<Property, Integer>emptyMap() : highOutdegreeProperties;
	}

	public ResourceDescription(Resource resource, Model model, Configuration config) {
		this.hypermediaResource = null;
		this.model = model;
		this.resource = resource;
		this.config = config;
		this.highIndegreeProperties = Collections.<Property, Integer>emptyMap(); 
		this.highOutdegreeProperties = Collections.<Property, Integer>emptyMap(); 
	}

	public String getURI() {
		return resource.getURI();
	}

	public Model getModel() {
		return model;
	}
	
	/**
	 * If {@link #getLabel()} is non null, return the label. If it is null,
	 * generate an attempt at a human-readable title from the URI. If the
	 * resource is blank, return null.
	 */
	public String getTitle() {
		String label = getLabel();
		if (label == null && resource.isURIResource()) {
			label = new URIPrefixer(resource, getPrefixes()).getLocalName();
		}
		// TODO: This should get the correct language from getLabel() and pass it on
		return toTitleCase(label, null);
	}

	public String getLabel() {
		Collection<RDFNode> candidates = getValuesFromMultipleProperties(config.getLabelProperties());
		return getBestLanguageMatch(candidates, config.getDefaultLanguage());
	}
	
	public String getComment() {
		Collection<RDFNode> candidates = getValuesFromMultipleProperties(config.getCommentProperties());
		return getBestLanguageMatch(candidates, config.getDefaultLanguage());
	}
	
	public String getImageURL() {
		Collection<RDFNode> candidates = getValuesFromMultipleProperties(config.getImageProperties());
		Iterator<RDFNode> it = candidates.iterator();
		while (it.hasNext()) {
			RDFNode candidate = (RDFNode) it.next();
			if (candidate.isURIResource()) {
				return ((Resource) candidate.as(Resource.class)).getURI();
			}
		}
		return null;
	}
	
	public ResourceProperty getProperty(Property property, boolean isInverse) {
		for (ResourceProperty p: getProperties()) {
			if (p.getURI().equals(property.getURI()) && p.isInverse() == isInverse) {
				return p;
			}
		}
		return null;
	}
	
	public List<ResourceProperty> getProperties() {
		if (properties == null) {
			properties = buildProperties();
		}
		return properties;
	}
	
	private List<ResourceProperty> buildProperties() {
		Map<String,PropertyBuilder> propertyBuilders = 
				new HashMap<String,PropertyBuilder>();
		StmtIterator it = resource.listProperties();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			Property predicate = stmt.getPredicate();
			String key = "=>" + predicate;
			if (!propertyBuilders.containsKey(key)) {
				propertyBuilders.put(key, new PropertyBuilder(predicate, false, config.getVocabularyStore()));
			}
			propertyBuilders.get(key).addValue(stmt.getObject());
		}
		it = model.listStatements(null, null, resource);
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			Property predicate = stmt.getPredicate();
			String key = "<=" + predicate;
			if (!propertyBuilders.containsKey(key)) {
				propertyBuilders.put(key, new PropertyBuilder(predicate, true, config.getVocabularyStore()));
			}
			propertyBuilders.get(key).addValue(stmt.getSubject());
		}
		for (Property p: highIndegreeProperties.keySet()) {
			String key = "<=" + p;
			if (!propertyBuilders.containsKey(key)) {
				propertyBuilders.put(key, new PropertyBuilder(p, true, config.getVocabularyStore()));
			}
			propertyBuilders.get(key).addHighDegreeArcs(highIndegreeProperties.get(p));
		}
		for (Property p: highOutdegreeProperties.keySet()) {
			String key = "=>" + p;
			if (!propertyBuilders.containsKey(key)) {
				propertyBuilders.put(key, new PropertyBuilder(p, false, config.getVocabularyStore()));
			}
			propertyBuilders.get(key).addHighDegreeArcs(highOutdegreeProperties.get(p));
		}
		List<ResourceProperty> results = new ArrayList<ResourceProperty>();
		Iterator<PropertyBuilder> it2 = propertyBuilders.values().iterator();
		while (it2.hasNext()) {
			PropertyBuilder propertyBuilder = (PropertyBuilder) it2.next();
			results.add(propertyBuilder.toProperty());
		}
		Collections.sort(results);
		return results;
	}

	/**
	 * Returns a prefix mapping containing all prefixes from the input model
	 * and from the configuration, with the configuration taking precedence.
	 */
	private PrefixMapping getPrefixes() {
		if (prefixes == null) {
			prefixes = new PrefixMappingImpl();
			prefixes.setNsPrefixes(model);
			for (String prefix: config.getPrefixes().getNsPrefixMap().keySet()) {
				prefixes.setNsPrefix(prefix, config.getPrefixes().getNsPrefixURI(prefix));
			}
		}
		return prefixes;
	}

	private Collection<RDFNode> getValuesFromMultipleProperties(
			Collection<Property> properties) {
		Collection<RDFNode> results = new ArrayList<RDFNode>();
		Iterator<Property> it = properties.iterator();
		while (it.hasNext()) {
			com.hp.hpl.jena.rdf.model.Property property = (com.hp.hpl.jena.rdf.model.Property) it.next();
			StmtIterator labelIt = resource.listProperties(property);
			while (labelIt.hasNext()) {
				RDFNode label = labelIt.nextStatement().getObject();
				results.add(label);
			}
		}
		return results;
	}
	
	private String getBestLanguageMatch(Collection<RDFNode> nodes, String lang) {
		Iterator<RDFNode> it = nodes.iterator();
		String aLiteral = null;
		while (it.hasNext()) {
			RDFNode candidate = it.next();
			if (!candidate.isLiteral()) continue;
			Literal literal = (Literal) candidate.as(Literal.class);
			if (lang == null
					|| lang.equals(literal.getLanguage())) {
				return literal.getString();
			}
			aLiteral = literal.getString();
		}
		return aLiteral;
	}
	
	public class ResourceProperty implements Comparable<ResourceProperty> {
		private final Property predicate;
		private final URIPrefixer predicatePrefixer;
		private final boolean isInverse;
		private final List<Value> values;
		private final int blankNodeCount;
		private final int highDegreeArcCount;
		private final VocabularyStore vocabularyStore;
		public ResourceProperty(Property predicate, boolean isInverse, List<Value> values,
				int blankNodeCount, int highDegreeArcCount,
				VocabularyStore vocabularyStore) {
			this.predicate = predicate;
			this.predicatePrefixer = new URIPrefixer(predicate, getPrefixes());
			this.isInverse = isInverse;
			this.values = values;
			this.blankNodeCount = blankNodeCount;
			this.highDegreeArcCount = highDegreeArcCount;
			this.vocabularyStore = vocabularyStore;
		}
		public boolean isInverse() {
			return isInverse;
		}
		public String getURI() {
			return predicate.getURI();
		}
		public boolean hasPrefix() {
			return predicatePrefixer.hasPrefix();
		}
		public String getPrefix() {
			return predicatePrefixer.getPrefix();
		}
		public String getLocalName() {
			return predicatePrefixer.getLocalName();
		}
		public String getLabel() {
			return getLabel(isMultiValued());
		}
		public String getLabel(boolean preferPlural) {
			return toTitleCase(vocabularyStore.getLabel(predicate.getURI(), preferPlural), null);
		}
		public String getInverseLabel() {
			return getInverseLabel(isMultiValued());
		}
		public String getInverseLabel(boolean preferPlural) {
			return toTitleCase(vocabularyStore.getInverseLabel(predicate.getURI(), preferPlural), null);
		}
		public String getDescription() {
			return vocabularyStore.getDescription(predicate.getURI());
		}
		public List<Value> getValues() {
			if (highDegreeArcCount > 0) return Collections.emptyList();
			return values;
		}
		public int getBlankNodeCount() {
			if (highDegreeArcCount > 0) return 0;
			return blankNodeCount;
		}
		public int getHighDegreeArcCount() {
			if (highDegreeArcCount == 0) return 0;
			return highDegreeArcCount + blankNodeCount + values.size();
		}
		public boolean isMultiValued() {
			return values.size() + highDegreeArcCount + blankNodeCount > 1;
		}
		public String getPathPageURL() {
			if (hypermediaResource == null) {
				return null;
			}
			return isInverse 
					? hypermediaResource.getInversePathPageURL(predicate) 
					: hypermediaResource.getPathPageURL(predicate);
		}
		public String getValuesPageURL() {
			if (hypermediaResource == null) {
				return null;
			}
			return isInverse
					? hypermediaResource.getInverseValuesPageURL(predicate) 
					: hypermediaResource.getValuesPageURL(predicate);
		}
		public int compareTo(ResourceProperty other) {
			if (!(other instanceof ResourceProperty)) {
				return 0;
			}
			ResourceProperty otherProperty = (ResourceProperty) other;
			int myWeight = config.getVocabularyStore().getWeight(predicate);
			int otherWeight = config.getVocabularyStore().getWeight(otherProperty.predicate);
			if (myWeight < otherWeight) return -1;
			if (myWeight > otherWeight) return 1;
			String propertyLocalName = getLocalName();
			String otherLocalName = otherProperty.getLocalName();
			if (propertyLocalName.compareTo(otherLocalName) != 0) {
				return propertyLocalName.compareTo(otherLocalName);
			}
			if (this.isInverse() != otherProperty.isInverse()) {
				return (this.isInverse()) ? 1 : -1;
			}
			return 0;
		}
	}
	
	private class PropertyBuilder {
		private final Property predicate;
		private final boolean isInverse;
		private final List<Value> values = new ArrayList<Value>();
		private int blankNodeCount = 0;
		private int highDegreeArcCount = 0;
		private VocabularyStore vocabularyStore;
		PropertyBuilder(Property predicate, boolean isInverse, VocabularyStore vocabularyStore) {
			this.predicate = predicate;
			this.isInverse = isInverse;
			this.vocabularyStore = vocabularyStore;
		}
		void addValue(RDFNode valueNode) {
			if (valueNode.isAnon()) {
				blankNodeCount++;
				return;
			}
			values.add(new Value(valueNode, predicate, vocabularyStore));
		}
		void addHighDegreeArcs(int count) {
			highDegreeArcCount += count;
		}
		ResourceProperty toProperty() {
			Collections.sort(values);
			return new ResourceProperty(predicate, isInverse, values, 
					blankNodeCount, highDegreeArcCount, vocabularyStore);
		}
	}
	
	public class Value implements Comparable<Value> {
		private final RDFNode node;
		private URIPrefixer prefixer;
		private Property predicate;
		private VocabularyStore vocabularyStore;
		public Value(RDFNode valueNode, Property predicate, VocabularyStore vocabularyStore) {
			this.node = valueNode;
			this.predicate = predicate;
			this.vocabularyStore = vocabularyStore;
			if (valueNode.isURIResource()) {
				prefixer = new URIPrefixer((Resource) valueNode.as(Resource.class), getPrefixes());
			}
		}
		public Node getNode() {
			return node.asNode();
		}
		public boolean hasPrefix() {
			return prefixer != null && prefixer.hasPrefix();
		}
		public String getPrefix() {
			if (prefixer == null) {
				return null;
			}
			return prefixer.getPrefix();
		}
		public String getLocalName() {
			if (prefixer == null) {
				return null;
			}
			return prefixer.getLocalName();
		}
		public String getLabel() {
			if (!node.isResource()) return null;
			String result = null;
			if (node.isURIResource()) {
				result = vocabularyStore.getLabel(node.asNode().getURI(), false);
			}
			if (result == null) {
				result = new ResourceDescription(node.asResource(), model, config).getLabel();
			}
			return toTitleCase(result, null);
		}
		public String getDescription() {
			return vocabularyStore.getDescription(node.asNode().getURI());
		}
		public String getDatatypeLabel() {
			if (!node.isLiteral()) return null;
			String uri = ((Literal) node.as(Literal.class)).getDatatypeURI();
			if (uri == null) return null;
			URIPrefixer datatypePrefixer = new URIPrefixer(uri, getPrefixes());
			if (datatypePrefixer.hasPrefix()) {
				return datatypePrefixer.toTurtle();
			} else {
				return "?:" + datatypePrefixer.getLocalName();
			}
		}
		public boolean isType() {
			return predicate.equals(RDF.type);
		}
		public int compareTo(Value other) {
			if (!(other instanceof Value)) {
				return 0;
			}
			Value otherValue = (Value) other;
			if (getNode().isURI() && otherValue.getNode().isURI()) {
				return getNode().getURI().compareTo(otherValue.getNode().getURI());
			}
			if (getNode().isURI()) {
				return 1;
			}
			if (otherValue.getNode().isURI()) {
				return -1;
			}
			if (getNode().isBlank() && otherValue.getNode().isBlank()) {
				return getNode().getBlankNodeLabel().compareTo(otherValue.getNode().getBlankNodeLabel());
			}
			if (getNode().isBlank()) {
				return 1;
			}
			if (otherValue.getNode().isBlank()) {
				return -1;
			}
			// TODO Typed literals, language literals
			return getNode().getLiteralLexicalForm().compareTo(otherValue.getNode().getLiteralLexicalForm());
		}
	}

	/**
	 * Converts a string to Title Case. Also trims surrounding whitespace
	 * and collapses consecutive whitespace characters within into a single
	 * space. If the language is English or null, English rules are used.
	 */
	public String toTitleCase(String s, String lang) {
		if (s == null) return null;
		if (lang == null) {
			lang = config.getDefaultLanguage();
		}
		Set<String> uncapitalizedWords = Collections.emptySet();
		if (lang == null || english.matcher(lang).matches()) {
			uncapitalizedWords = englishUncapitalizedWords;
		}
		StringBuffer result = new StringBuffer();
		Matcher matcher = wordPattern.matcher(s);
		boolean first = true;
		while (matcher.find()) {
			if (!first) result.append(' ');
			String word = matcher.group();
			if ("".equals(word)) continue;
			if (first || !uncapitalizedWords.contains(word)) {
				word = word.substring(0, 1).toUpperCase() + word.substring(1);
			}
			result.append(word);
			first = false;
		}
		return result.toString();
	}
	private static Pattern wordPattern = Pattern.compile("[^ \t\r\n]+");
	private static Pattern english = Pattern.compile("^en(-.*)?$", Pattern.CASE_INSENSITIVE);
	private static Set<String> englishUncapitalizedWords = 
			new HashSet<String>(Arrays.asList(
					// Prepositions
					"above", "about", "across", "against", "along", "among",
					"around", "at", "before", "behind", "below", "beneath",
					"beside", "between", "beyond", "by", "down", "during",
					"except", "for", "from", "in", "inside", "into", "like",
					"near", "of", "off", "on", "since", "to", "toward", 
					"through", "under", "until", "up", "upon", "with", "within",
					// Articles
					"a", "an", "the",
					// Conjunctions
					"and", "but", "for", "nor", "or", "so", "yet" 
			));
}
