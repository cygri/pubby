package de.fuberlin.wiwiss.pubby;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;
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

/**
 * The server's configuration.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class Configuration {
	private static final String DEFAULT_PROJECT_NAME = "Untitled Dataset";
	
	private final Model model;
	private final Resource config;
	private final PrefixMapping prefixes;
	private final Collection<Property> labelProperties;
	private final Collection<Property> commentProperties;
	private final Collection<Property> imageProperties;
	private final ArrayList<Dataset> datasets = new ArrayList<Dataset>();
	
	public Configuration(Model configurationModel) {
		model = configurationModel;
		StmtIterator it = model.listStatements(null, RDF.type, CONF.Configuration);
		if (!it.hasNext()) {
			throw new IllegalArgumentException(
					"No conf:Configuration found in configuration model");
		}
		config = it.nextStatement().getSubject();

		it = model.listStatements(config, CONF.dataset, (RDFNode) null);
		while (it.hasNext()) {
			datasets.add(new Dataset(it.nextStatement().getResource()));
		}
		labelProperties = new ArrayList<Property>();
		it = model.listStatements(config, CONF.labelProperty, (RDFNode) null);
		while (it.hasNext()) {
			labelProperties.add(it.nextStatement().getObject().as(Property.class));
		}
		if (labelProperties.isEmpty()) {
			labelProperties.add(RDFS.label);
			labelProperties.add(DC.title);
			labelProperties.add(model.createProperty("http://xmlns.com/foaf/0.1/name"));
		}
		commentProperties = new ArrayList<Property>();
		it = model.listStatements(config, CONF.commentProperty, (RDFNode) null);
		while (it.hasNext()) {
			commentProperties.add(it.nextStatement().getObject().as(Property.class));
		}
		if (commentProperties.isEmpty()) {
			commentProperties.add(RDFS.comment);
			commentProperties.add(DC.description);
		}
		imageProperties = new ArrayList<Property>();
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
				String uri = stmt.getResource().getURI();
				prefixes.setNsPrefixes(FileManager.get().loadModel(uri));
			}
		} else {
			prefixes.setNsPrefixes(model);
		}
		if (prefixes.getNsURIPrefix(CONF.NS) != null) {
			prefixes.removeNsPrefix(prefixes.getNsURIPrefix(CONF.NS));
		}
		// If no prefix is defined for the RDF and XSD namespaces, set them,
		// unless that would overwrite something. This is the namespaces that
		// have syntactic sugar in Turtle.
		ModelUtil.addNSIfUndefined(prefixes, "rdf", RDF.getURI());
		ModelUtil.addNSIfUndefined(prefixes, "xsd", XSD.getURI());
		// If we don't have an indexResource, then add an index builder dataset
		// as the first dataset. It will be responsible for handling the
		// homepage/index resource.
		if (!config.hasProperty(CONF.indexResource)) {
			String indexURL = getWebApplicationBaseURI();
			List<Dataset> realDatasets = new ArrayList<Dataset>(datasets);
			Dataset indexDataset = new Dataset(new IndexDataSource(indexURL, realDatasets, this), indexURL);
			datasets.add(0, indexDataset);
		}
	}

	/**
	 * @param relativeIRI IRI relative to the server base. Note that a request URI needs to be percent-decoded first.
	 * @param stripResourcePrefix If true, the webResourcePrefix will be stripped to derive the real relative IRI.
	 * @return
	 */
	public HypermediaResource getController(String relativeIRI, boolean stripResourcePrefix) {
		if (stripResourcePrefix) {
			if (!relativeIRI.startsWith(getWebResourcePrefix())) return null;
			relativeIRI = relativeIRI.substring(getWebResourcePrefix().length());
		}
		return new HypermediaResource(relativeIRI, this);
	}
	
	public Collection<MappedResource> getMappedResourcesFromRelativeWebURI(String relativeWebURI, boolean isResourceURI) {
		Collection<MappedResource> results = new ArrayList<MappedResource>();
		for (Dataset dataset: datasets) {
			MappedResource resource = dataset.getMappedResourceFromRelativeWebURI(
					relativeWebURI, isResourceURI, this);
			if (resource == null) continue;
			results.add(resource);
		}
		return results;
	}
	
	public PrefixMapping getPrefixes() {
		return prefixes;
	}

	public Collection<Property> getLabelProperties() {
		return labelProperties;
	}
	
	public Collection<Property> getCommentProperties() {
		return commentProperties;
	}
	
	public Collection<Property> getImageProperties() {
		return imageProperties;
	}
	
	public String getDefaultLanguage() {
		if (!config.hasProperty(CONF.defaultLanguage)) {
			return null;
		}
		return config.getProperty(CONF.defaultLanguage).getString();
	}
	
	public HypermediaResource getIndexResource() {
		if (!config.hasProperty(CONF.indexResource)) {
			return null;
		}
		String uri = config.getProperty(CONF.indexResource).getResource().getURI();
		String resourceBase = getWebApplicationBaseURI() + getWebResourcePrefix();
		if (!uri.startsWith(resourceBase)) {
			throw new RuntimeException("conf:indexResource must start with " + resourceBase);
		}
		return new HypermediaResource(uri.substring(resourceBase.length()), this);
	}
	
	public String getProjectLink() {
		Statement stmt = config.getProperty(CONF.projectHomepage);
		return stmt == null ? null : stmt.getResource().getURI();
	}

	public String getProjectName() {
		Statement stmt = config.getProperty(CONF.projectName);
		return stmt == null ? DEFAULT_PROJECT_NAME : stmt.getString();
	}

	public String getWebApplicationBaseURI() {
		return config.getProperty(CONF.webBase).getResource().getURI();
	}

	public String getWebResourcePrefix() {
		if (config.hasProperty(CONF.webResourcePrefix)) {
			return config.getProperty(CONF.webResourcePrefix).getString();
		}
		return "";
	}

	public List<Dataset> getDatasets() {
		return datasets;
	}
}