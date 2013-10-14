package de.fuberlin.wiwiss.pubby;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import com.hp.hpl.jena.n3.IRIResolver;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.vocabulary.XSD;

import de.fuberlin.wiwiss.pubby.vocab.CONF;
import de.fuberlin.wiwiss.pubby.vocab.META;

/**
 * A dataset block in the server's configuration.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author Hannes Muehleisen
 * @author Olaf Hartig
 * @version $Id$
 */
public class Dataset {
	private final static String metadataPlaceholderURIPrefix = "about:metadata:";

	private final Model model;
	private final Resource dataset;
	private final DataSource dataSource;
	private final String datasetBase;
	private final Pattern datasetURIPattern;
	private final char[] fixUnescapeCharacters;
	private final Resource rdfDocumentMetadataTemplate;
	private final String metadataTemplate;
	private Calendar currentTime;
	private Resource currentDocRepr;
	
	/**
	 * Creates a degenerate dataset that contains only a single URI.
	 * 
	 * TODO: This is all wrong here, but we currently need it because we can't make a MappedResource without a Dataset
	 * 
	 * @param dataSource The data source that can describe the URI
	 * @param constantURI The single constant URI contained in this dataset
	 */
	public Dataset(DataSource dataSource, String constantURI) {
		this.dataSource = dataSource;
		model = ModelFactory.createDefaultModel();
		dataset = model.createResource();
		datasetBase = constantURI;
		datasetURIPattern = Pattern.compile("^$");
		fixUnescapeCharacters = new char[0];
		rdfDocumentMetadataTemplate = null;
		metadataTemplate = null;
	}
	
	public Dataset(Resource config, Configuration configuration) {
		this.dataset = config;
		model = config.getModel();
		if (config.hasProperty(CONF.datasetBase)) {
			datasetBase = config.getProperty(CONF.datasetBase).getResource().getURI();
		} else {
			datasetBase = configuration.getWebApplicationBaseURI();
		}
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
		if (config.hasProperty(CONF.metadataTemplate)) {
			metadataTemplate = config.getProperty(CONF.metadataTemplate).getString();
		} else {
			metadataTemplate = null;
		}
		if (config.hasProperty(CONF.sparqlEndpoint)) {
			String endpointURL = config.getProperty(CONF.sparqlEndpoint).getResource().getURI();
			String defaultGraphURI = config.hasProperty(CONF.sparqlDefaultGraph)
					? config.getProperty(CONF.sparqlDefaultGraph).getResource().getURI()
					: null;
			dataSource = new RemoteSPARQLDataSource(
					endpointURL,
					defaultGraphURI,
					supportsSPARQL11(),
					listSPARQLQueries(CONF.resourceDescriptionQuery),
					listSPARQLQueries(CONF.propertyListQuery),
					listSPARQLQueries(CONF.inversePropertyListQuery),
					listSPARQLQueries(CONF.anonymousPropertyDescriptionQuery),
					listSPARQLQueries(CONF.anonymousInversePropertyDescriptionQuery),
					configuration.getHighIndegreeProperties(),
					configuration.getHighOutdegreeProperties());
			if (config.hasProperty(CONF.contentType)) {
				((RemoteSPARQLDataSource) dataSource).setContentType(
						config.getProperty(CONF.contentType).getString());
			}
		} else {
			Model data = ModelFactory.createDefaultModel();
			StmtIterator it = config.listProperties(CONF.loadRDF);
			while (it.hasNext()) {
				Statement stmt = it.nextStatement();
				String fileName = stmt.getResource().getURI();
				// If the location is a local file, then use webBase as base URI
				// to resolve relative URIs in the file. Having file:/// URIs in
				// there would likely not be useful to anyone.
				fileName = IRIResolver.resolveGlobal(fileName);
				String base = (fileName.startsWith("file:/") ? 
						configuration.getWebApplicationBaseURI() : fileName);

				Model m = FileManager.get().loadModel(fileName, base, null);
				data.add(m);
				
				// We'd like to do simply data.setNsPrefix(m), but that leaves relative
				// namespace URIs like <#> unresolved, so we do a big dance to make them
				// absolute.
				for (String prefix: m.getNsPrefixMap().keySet()) {
					String uri = IRIResolver.resolve(m.getNsPrefixMap().get(prefix), base);
					data.setNsPrefix(prefix, uri);
				}
			}
			dataSource = new ModelDataSource(data);
		}
	}

	public boolean isDatasetURI(String uri) {
		return uri.startsWith(datasetBase) 
				&& datasetURIPattern.matcher(uri.substring(datasetBase.length())).matches();
	}
	
	public MappedResource getMappedResourceFromDatasetURI(String datasetURI, Configuration configuration) {
		if (!isDatasetURI(datasetURI)) return null;
		return new MappedResource(
				escapeURIDelimiters(datasetURI.substring(datasetBase.length())),
				datasetURI,
				configuration,
				this);
	}

	public MappedResource getMappedResourceFromRelativeWebURI(String relativeWebURI, 
			boolean isResourceURI, Configuration configuration) {
		if (isResourceURI) {
			if (!"".equals(configuration.getWebResourcePrefix())) {
				if (!relativeWebURI.startsWith(configuration.getWebResourcePrefix())) {
					return null;
				}
				relativeWebURI = relativeWebURI.substring(configuration.getWebResourcePrefix().length());
			}
		}
		relativeWebURI = fixUnescapedCharacters(relativeWebURI);
		if (!datasetURIPattern.matcher(relativeWebURI).matches()) {
			return null;
		}
		String decoded = getSupportsIRIs() ? IRIEncoder.toIRI(relativeWebURI) : relativeWebURI;
		return new MappedResource(
				decoded,
				datasetBase + unescapeURIDelimiters(decoded),
				configuration,
				this);
	}
	
	public boolean getSupportsIRIs() {
		return getBooleanConfigValue(CONF.supportsIRIs, true);
	}
	
	public boolean getAddSameAsStatements() {
		return getBooleanConfigValue(CONF.addSameAsStatements, false);
	}
	
	public DataSource getDataSource() {
		return dataSource;
	}
	
	public boolean supportsSPARQL11() {
		return getBooleanConfigValue(CONF.supportsSPARQL11, false);
	}
	
	public List<HypermediaResource> getIndex(Configuration configuration) {
		List<HypermediaResource> result = new ArrayList<HypermediaResource>();
		for (Resource r: dataSource.getIndex()) {
			if (!r.getURI().startsWith(datasetBase)) continue;
			result.add(getMappedResourceFromDatasetURI(
					r.getURI(), configuration).getController());
		}
		return result;
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
	
	public Resource addMetadataFromTemplate(Model document, MappedResource describedResource, ServletContext context) {
		if (metadataTemplate == null) {
			return null;
		}
		
		currentTime = Calendar.getInstance();
		
		// add metadata from templates
		Model tplModel = ModelFactory.createDefaultModel();
		String tplPath = context.getRealPath("/") + "/WEB-INF/templates/" + metadataTemplate;
		FileManager.get().readModel( tplModel, tplPath, FileUtils.guessLang(tplPath,"N3") );

		// iterate over template statements to replace placeholders
		Model metadata = ModelFactory.createDefaultModel();
		currentDocRepr = metadata.createResource();
		StmtIterator it = tplModel.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			Resource subj = stmt.getSubject();
			Property pred = stmt.getPredicate();
			RDFNode  obj  = stmt.getObject();
			
			try {
				if (subj.toString().contains(metadataPlaceholderURIPrefix)){
					subj = (Resource) parsePlaceholder(subj, describedResource, context);
					if (subj == null) {
						// create a unique blank node with a fixed id.
						subj = model.createResource(new AnonId(String.valueOf(stmt.getSubject().hashCode())));
					}
				}
				
				if (obj.toString().contains(metadataPlaceholderURIPrefix)){
					obj = parsePlaceholder(obj, describedResource, context);
				}
				
				// only add statements with some objects
				if (obj != null) {
					stmt = metadata.createStatement(subj,pred,obj);
					metadata.add(stmt);
				}
			} catch (Exception e) {
				// something went wrong, oops - lets better remove the offending statement
				metadata.remove(stmt);
				e.printStackTrace();
			}
		}
		
		// remove blank nodes that don't have any properties
		boolean changes = true;
		while ( changes ) {
			changes = false;
			StmtIterator stmtIt = metadata.listStatements();
			List<Statement> remList = new ArrayList<Statement>();
			while (stmtIt.hasNext()) {
				Statement s = stmtIt.nextStatement();
				if (    s.getObject().isAnon()
				     && ! ((Resource) s.getObject().as(Resource.class)).listProperties().hasNext() ) {
					remList.add(s);
					changes = true;
				}
			}
			metadata.remove(remList);
		}

		if (document != null) {
			document.add( metadata );
		}

		return currentDocRepr;
	}
	
	private RDFNode parsePlaceholder(RDFNode phRes, MappedResource describedResource, ServletContext context) {
		String phURI = phRes.asNode().getURI();
		// get package name and placeholder name from placeholder URI
		phURI = phURI.replace(metadataPlaceholderURIPrefix, "");
		String phPackage = phURI.substring(0, phURI.indexOf(":")+1);
		String phName = phURI.replace(phPackage, "");
		phPackage = phPackage.replace(":", "");
		
		if (phPackage.equals("runtime")) {
			// <about:metadata:runtime:query> - the SPARQL Query used to get the RDF Graph
			if (phName.equals("query")) {
				RemoteSPARQLDataSource ds = (RemoteSPARQLDataSource) describedResource.getDataset().getDataSource();
				return model.createTypedLiteral(ds.getPreviousDescribeQuery());
			}
			// <about:metadata:runtime:time> - the current time
			if (phName.equals("time")) {
				return model.createTypedLiteral(currentTime);
			}
			// <about:metadata:runtime:graph> - URI of the graph
			if (phName.equals("graph")) {
				// Replaced the commented line by the following one because the
				// RDF graph we want to talk about is a specific representation
				// of the data identified by the getDataURL() URI.
				//                                       Olaf, May 28, 2010
				// return model.createResource(describedResource.getDataURL());
				return currentDocRepr;
			}
			// <about:metadata:runtime:data> - URI of the data
			if (phName.equals("data")) {
				return model.createResource(describedResource.getController().getDataURL());
			}
			// <about:metadata:runtime:resource> - URI of the resource
			if (phName.equals("resource")) {
				return model.createResource(describedResource.getController().getAbsoluteIRI());
			}
		}
		
		// <about:metadata:config:*> - The configuration parameters
		if (phPackage.equals("config")) {
			// look for requested property in the dataset config
			Property p  = model.createProperty(CONF.NS + phName);
			if (dataset.hasProperty(p))
				return dataset.getProperty(p).getObject();
			
			// find pointer to the global configuration set...
			StmtIterator it = dataset.getModel().listStatements(null, CONF.dataset, dataset);
			Statement ptrStmt = it.nextStatement();
			if (ptrStmt == null) return null;
			
			// look in global config if nothing found so far
			Resource globalConfig = ptrStmt.getSubject();
			if (globalConfig.hasProperty(p))
				return globalConfig.getProperty(p).getObject();
		}
		
		// <about:metadata:metadata:*> - The metadata provided by users
		if (phPackage.equals("metadata")) {
			// look for requested property in the dataset config
			Property p  = model.createProperty(META.NS + phName);
			if (dataset.hasProperty(p))
				return dataset.getProperty(p).getObject();
			
			// find pointer to the global configuration set...
			StmtIterator it = dataset.getModel().listStatements(null, CONF.dataset, dataset);
			Statement ptrStmt = it.nextStatement();
			if (ptrStmt == null) return null;
			
			// look in global config if nothing found so far
			Resource globalConfig = ptrStmt.getSubject();
			if (globalConfig.hasProperty(p))
				return globalConfig.getProperty(p).getObject();
		}

		return model.createResource(new AnonId(String.valueOf(phRes.hashCode())));
	}
	
	private boolean getBooleanConfigValue(Property property, boolean defaultValue) {
		if (!dataset.hasProperty(property)) {
			return defaultValue;
		}
		Literal value = dataset.getProperty(property).getLiteral();
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
			if ((int) uri.charAt(charIndex) > 127) {
				encodeThis = true;
			}
			for (int i = 0; i < fixUnescapeCharacters.length; i++) {
				if (uri.charAt(charIndex) == fixUnescapeCharacters[i]) {
					encodeThis = true;
					break;
				}
			}
			if (encodeThis) {
				encoded.append('%');
				int b = (int) uri.charAt(charIndex);
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

	private List<String> listSPARQLQueries(Property queryProperty) {
		List<String> list = new ArrayList<String>();
		StmtIterator it = dataset.listProperties(queryProperty);
		while (it.hasNext()) {
			list.add(it.nextStatement().getString());
		}
		return list.isEmpty() ? null : list;
	}
}
