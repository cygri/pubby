package de.fuberlin.wiwiss.pubby;

import java.util.ArrayList;
import java.util.Collection;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

import de.fuberlin.wiwiss.pubby.vocab.CONF;

public class Configuration {
	private final Model model;
	private final Resource config;
	private final DataSource dataSource;
	private final PrefixMapping prefixes;
	private final Collection labelProperties;
	private final Collection commentProperties;
	private final Collection imageProperties;
	private final char[] fixUnescapeCharacters;
	private final Resource rdfDocumentMetadataTemplate;
	
	public Configuration(Model configurationModel) {
		model = configurationModel;
		StmtIterator it = model.listStatements(null, RDF.type, CONF.Configuration);
		if (!it.hasNext()) {
			throw new IllegalArgumentException(
					"No conf:Configuration found in configuration model");
		}
		config = it.nextStatement().getSubject();
		labelProperties = new ArrayList();
		it = model.listStatements(config, CONF.labelProperty, (RDFNode) null);
		while (it.hasNext()) {
			labelProperties.add(it.nextStatement().getObject().as(Property.class));
		}
		if (labelProperties.isEmpty()) {
			labelProperties.add(RDFS.label);
			labelProperties.add(DC.title);
			labelProperties.add(model.createProperty("http://xmlns.com/foaf/0.1/name"));
		}
		commentProperties = new ArrayList();
		it = model.listStatements(config, CONF.commentProperty, (RDFNode) null);
		while (it.hasNext()) {
			commentProperties.add(it.nextStatement().getObject().as(Property.class));
		}
		if (commentProperties.isEmpty()) {
			commentProperties.add(RDFS.comment);
			commentProperties.add(DC.description);
		}
		imageProperties = new ArrayList();
		it = model.listStatements(config, CONF.imageProperty, (RDFNode) null);
		while (it.hasNext()) {
			imageProperties.add(it.nextStatement().getObject().as(Property.class));
		}
		if (imageProperties.isEmpty()) {
			imageProperties.add(model.createProperty("http://xmlns.com/foaf/0.1/depiction"));
		}
		
		prefixes = new PrefixMappingImpl();		
		if (config.hasProperty(CONF.usePrefixesFrom)) {
			it = config.listProperties(CONF.usePrefixesFrom);
			while (it.hasNext()) {
				Statement stmt = it.nextStatement();
				prefixes.setNsPrefixes(FileManager.get().loadModel(
						stmt.getResource().getURI()));
			}
		} else {
			prefixes.setNsPrefixes(model);
		}
		if (prefixes.getNsURIPrefix(CONF.NS) != null) {
			prefixes.removeNsPrefix(prefixes.getNsURIPrefix(CONF.NS));
		}
		if (config.hasProperty(CONF.fixUnescapedCharacters)) {
			String chars = config.getProperty(CONF.fixUnescapedCharacters).getString();
			fixUnescapeCharacters = new char[chars.length()];
			for (int i = 0; i < chars.length(); i++) {
				fixUnescapeCharacters[i] = chars.charAt(i);
			}
		} else {
			fixUnescapeCharacters = new char[0];
		}
		if (config.hasProperty(CONF.rdfDocumentMetadata)) {
			rdfDocumentMetadataTemplate = config.getProperty(CONF.rdfDocumentMetadata).getResource();
		} else {
			rdfDocumentMetadataTemplate = null;
		}
		if (config.hasProperty(CONF.sparqlEndpoint)) {
			String endpointURL = config.getProperty(CONF.sparqlEndpoint).getResource().getURI();
			String defaultGraph = config.hasProperty(CONF.sparqlDefaultGraph)
					? config.getProperty(CONF.sparqlDefaultGraph).getResource().getURI()
					: null;
			dataSource = new RemoteSPARQLDataSource(endpointURL, defaultGraph);
		} else {
			Model data = ModelFactory.createDefaultModel();
			it = config.listProperties(CONF.loadRDF);
			while (it.hasNext()) {
				Statement stmt = it.nextStatement();
				FileManager.get().readModel(data, stmt.getResource().getURI());
			}
			dataSource = new ModelDataSource(data);
		}
	}

	public boolean isDatasetURI(String uri) {
		return uri.startsWith(getDatasetBase());
	}
	
	public MappedResource getMappedResourceFromDatasetURI(String datasetURI) {
		return new MappedResource(
				escapeURIDelimiters(datasetURI.substring(getDatasetBase().length())),
				datasetURI,
				this);
	}

	public MappedResource getMappedResourceFromRelativeWebURI(String relativeWebURI) {
		relativeWebURI = fixUnescapedCharacters(relativeWebURI);
		return new MappedResource(
				relativeWebURI,
				getDatasetBase() + unescapeURIDelimiters(relativeWebURI),
				this);
	}
	
	public String getDatasetBase() {
		return config.getProperty(CONF.datasetBase).getResource().getURI();
	}
	
	public String getDatasetURI(String relativeResourceURI) {
		return getMappedResourceFromRelativeWebURI(relativeResourceURI).getDatasetURI();
	}

	public String getWebURI(String relativeResourceURI) {
		return getMappedResourceFromRelativeWebURI(relativeResourceURI).getWebURI();
	}

	public String getDataURL(String relativeResourceURI) {
		return getMappedResourceFromRelativeWebURI(relativeResourceURI).getDataURL();
	}

	public String getPageURL(String relativeResourceURI) {
		return getMappedResourceFromRelativeWebURI(relativeResourceURI).getPageURL();
	}

	public String getPathDataURL(String relativeResourceURI, Property property, boolean isInverse) {
		if (isInverse) {
			return getMappedResourceFromRelativeWebURI(relativeResourceURI).getInversePathDataURL(property);
		} else {
			return getMappedResourceFromRelativeWebURI(relativeResourceURI).getPathDataURL(property);
		}
	}
	
	public String getPathPageURL(String relativeResourceURI, Property property, boolean isInverse) {
		if (isInverse) {
			return getMappedResourceFromRelativeWebURI(relativeResourceURI).getInversePathPageURL(property);
		} else {
			return getMappedResourceFromRelativeWebURI(relativeResourceURI).getPathPageURL(property);
		}
	}
	
	public boolean getAddSameAsStatements() {
		return getBooleanConfigValue(CONF.addSameAsStatements, false);
	}
	
	public PrefixMapping getPrefixes() {
		return prefixes;
	}

	public Collection getLabelProperties() {
		return labelProperties;
	}
	
	public Collection getCommentProperties() {
		return commentProperties;
	}
	
	public Collection getImageProperties() {
		return imageProperties;
	}
	
	public String getDefaultLanguage() {
		if (!config.hasProperty(CONF.defaultLanguage)) {
			return null;
		}
		return config.getProperty(CONF.defaultLanguage).getString();
	}
	
	public MappedResource getIndexResource() {
		if (!config.hasProperty(CONF.indexResource)) {
			return null;
		}
		return getMappedResourceFromDatasetURI(
				config.getProperty(CONF.indexResource).getResource().getURI());
	}
	
	public String getProjectLink() {
		return config.getProperty(CONF.datasetHomepage).getResource().getURI();
	}

	public String getProjectName() {
		return config.getProperty(CONF.datasetName).getString();
	}

	public DataSource getDataSource() {
		return dataSource;
	}
	
//	public String getSPARQLEndpointURL() {
//		return config.getProperty(CONF.sparqlEndpoint).getResource().getURI();
//	}
//	
//	public String getSPARQLDefaultGraphURI() {
//		return config.getProperty(CONF.sparqlDefaultGraph).getResource().getURI();
//	}

	public String getWebApplicationBaseURI() {
		return config.getProperty(CONF.webBase).getResource().getURI();
	}

	public boolean redirectRDFRequestsToEndpoint() {
		return getBooleanConfigValue(CONF.redirectRDFRequestsToEndpoint, false);
	}
	
	public String getWebResourcePrefix() {
		if (config.hasProperty(CONF.webResourcePrefix)) {
			return config.getProperty(CONF.webResourcePrefix).getString();
		}
		return "";
	}

	public void addDocumentMetadata(Model document, Resource documentResource) {
		if (rdfDocumentMetadataTemplate == null) {
			return;
		}
		StmtIterator it = rdfDocumentMetadataTemplate.listProperties();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			document.add(documentResource, stmt.getPredicate(), stmt.getObject());
		}
		it = this.model.listStatements(null, null, rdfDocumentMetadataTemplate);
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			if (stmt.getPredicate().equals(CONF.rdfDocumentMetadata)) {
				continue;
			}
			document.add(stmt.getSubject(), stmt.getPredicate(), documentResource);
		}
	}
	
	private boolean getBooleanConfigValue(Property property, boolean defaultValue) {
		if (!config.hasProperty(property)) {
			return defaultValue;
		}
		Literal value = config.getProperty(property).getLiteral();
		if (XSD.xboolean.equals(value.getDatatype())) {
			return value.getBoolean();
		}
		return "true".equals(value.getString());
	}

	private String fixUnescapedCharacters(String uri) {
		if (fixUnescapeCharacters.length == 0) {
			return uri;
		}
		StringBuffer encoded = new StringBuffer(uri.length() + 4);
		for (int charIndex = 0; charIndex < uri.length(); charIndex++) {
			boolean encodeThis = false;
			for (int i = 0; i < fixUnescapeCharacters.length; i++) {
				if (uri.charAt(charIndex) == fixUnescapeCharacters[i]) {
					encodeThis = true;
					break;
				}
			}
			if (encodeThis) {
				encoded.append('%');
				byte b = (byte) uri.charAt(charIndex);
				encoded.append(Integer.toString(b, 16).toUpperCase());
			} else {
				encoded.append(uri.charAt(charIndex));
			}
		}
		return encoded.toString();
	}

	private String escapeURIDelimiters(String uri) {
		return uri.replaceAll("#", "%23").replaceAll("\\?", "%3F");
	}
	
	private String unescapeURIDelimiters(String uri) {
		return uri.replaceAll("%23", "#").replaceAll("%3F", "?");
	}
}
