package de.fuberlin.wiwiss.pubby;

import java.util.regex.Pattern;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.XSD;

import de.fuberlin.wiwiss.pubby.vocab.CONF;

/**
 * The server's configuration.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class Dataset {
	private final Model model;
	private final Resource config;
	private final DataSource dataSource;
	private final Pattern datasetURIPattern;
	private final char[] fixUnescapeCharacters;
	private final Resource rdfDocumentMetadataTemplate;
	
	public Dataset(Resource config) {
		model = config.getModel();
		this.config = config;
		if (config.hasProperty(CONF.datasetURIPattern)) {
			datasetURIPattern = Pattern.compile(
					config.getProperty(CONF.datasetURIPattern).getString());
		} else {
			datasetURIPattern = Pattern.compile(".*");
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
			StmtIterator it = config.listProperties(CONF.loadRDF);
			while (it.hasNext()) {
				Statement stmt = it.nextStatement();
				FileManager.get().readModel(data, stmt.getResource().getURI());
			}
			dataSource = new ModelDataSource(data);
		}
	}

	public boolean isDatasetURI(String uri) {
		return uri.startsWith(getDatasetBase()) 
				&& datasetURIPattern.matcher(uri.substring(getDatasetBase().length())).matches();
	}
	
	public MappedResource getMappedResourceFromDatasetURI(String datasetURI, Configuration configuration) {
		return new MappedResource(
				escapeURIDelimiters(datasetURI.substring(getDatasetBase().length())),
				datasetURI,
				configuration,
				this);
	}

	public MappedResource getMappedResourceFromRelativeWebURI(String relativeWebURI, 
			boolean isResourceURI, Configuration configuration) {
		if (isResourceURI) {
			if (!"".equals(getWebResourcePrefix())) {
				if (!relativeWebURI.startsWith(getWebResourcePrefix())) {
					return null;
				}
				relativeWebURI = relativeWebURI.substring(getWebResourcePrefix().length());
			}
		}
		relativeWebURI = fixUnescapedCharacters(relativeWebURI);
		if (!datasetURIPattern.matcher(relativeWebURI).matches()) {
			return null;
		}
		return new MappedResource(
				relativeWebURI,
				getDatasetBase() + unescapeURIDelimiters(relativeWebURI),
				configuration,
				this);
	}
	
	public String getDatasetBase() {
		return config.getProperty(CONF.datasetBase).getResource().getURI();
	}
	
	public boolean getAddSameAsStatements() {
		return getBooleanConfigValue(CONF.addSameAsStatements, false);
	}
	
	public DataSource getDataSource() {
		return dataSource;
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
