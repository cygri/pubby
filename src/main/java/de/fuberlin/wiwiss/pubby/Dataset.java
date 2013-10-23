package de.fuberlin.wiwiss.pubby;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.hp.hpl.jena.n3.IRIResolver;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;

import de.fuberlin.wiwiss.pubby.sources.DataSource;
import de.fuberlin.wiwiss.pubby.sources.FilteredDataSource;
import de.fuberlin.wiwiss.pubby.sources.ModelDataSource;
import de.fuberlin.wiwiss.pubby.sources.RemoteSPARQLDataSource;
import de.fuberlin.wiwiss.pubby.sources.RewrittenDataSource;
import de.fuberlin.wiwiss.pubby.vocab.CONF;

/**
 * A dataset block in the server's configuration.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author Hannes Muehleisen
 * @author Olaf Hartig
 * @version $Id$
 */
public class Dataset extends ResourceReader {
	private final DataSource dataSource;
	private final MetadataConfiguration metadata;
	
	// TODO: This is a rather dirty hack. We may need DataSource.getProvenance() or something
	private RemoteSPARQLDataSource sparqlDataSource = null;
	
	public Dataset(Resource dataset, Configuration configuration) {
		super(dataset);
		dataSource = buildDataSource(configuration);
		metadata = new MetadataConfiguration(dataset, sparqlDataSource);
	}

	public DataSource getDataSource() {
		return dataSource;
	}
	
	public MetadataConfiguration getMetadataConfiguration() {
		return metadata;
	}
	
	public boolean supportsIRIs() {
		return getBoolean(CONF.supportsIRIs, true);
	}
	
	public boolean addSameAsStatements() {
		return getBoolean(CONF.addSameAsStatements, false);
	}
	
	public boolean supportsSPARQL11() {
		return getBoolean(CONF.supportsSPARQL11, false);
	}

	/**
	 * Gets all values of <tt>conf:browsableNamespace</tt> declared on the
	 * dataset resource. Does not include values inherited from the
	 * configuration resource.
	 *  
	 * @return Namespace IRIs of browsable namespaces
	 */
	public Set<String> getBrowsableNamespaces() {
		return getIRIs(CONF.browsableNamespace);
	}
	

	private DataSource buildDataSource(Configuration configuration) {
		requireExactlyOneOf(CONF.sparqlEndpoint, CONF.loadRDF);

		DataSource result;
		if (hasProperty(CONF.sparqlEndpoint)) {
			
			// SPARQL data source
			String endpointURL = getIRI(CONF.sparqlEndpoint);
			String defaultGraphURI = getIRI(CONF.sparqlDefaultGraph);
			sparqlDataSource = new RemoteSPARQLDataSource(
					endpointURL,
					defaultGraphURI,
					supportsSPARQL11(),
					getStrings(CONF.resourceDescriptionQuery),
					getStrings(CONF.propertyListQuery),
					getStrings(CONF.inversePropertyListQuery),
					getStrings(CONF.anonymousPropertyDescriptionQuery),
					getStrings(CONF.anonymousInversePropertyDescriptionQuery),
					configuration.getVocabularyStore().getHighIndegreeProperties(),
					configuration.getVocabularyStore().getHighOutdegreeProperties());
			if (hasProperty(CONF.contentType)) {
				sparqlDataSource.setGraphContentType(getString(CONF.contentType));
			}
			for (String param: getStrings(CONF.queryParamSelect)) {
				sparqlDataSource.addSelectQueryParam(param);
			}
			for (String param: getStrings(CONF.queryParamGraph)) {
				sparqlDataSource.addGraphQueryParam(param);
			}
			result = sparqlDataSource;
			
		} else {
			
			// File data source
			Model data = ModelFactory.createDefaultModel();
			for (String fileName: getIRIs(CONF.loadRDF)) {
				// If the location is a local file, then use webBase as base URI
				// to resolve relative URIs in the file. Having file:/// URIs in
				// there would likely not be useful to anyone.
				fileName = IRIResolver.resolveGlobal(fileName);
				String base = (fileName.startsWith("file:/") ? 
						configuration.getWebApplicationBaseURI() : fileName);

				try {
					Model m = FileManager.get().loadModel(fileName, base, null);
					data.add(m);
				
					// We'd like to do simply data.setNsPrefix(m), but that leaves relative
					// namespace URIs like <#> unresolved, so we do a big dance to make them
					// absolute.
					for (String prefix: m.getNsPrefixMap().keySet()) {
						String uri = IRIResolver.resolve(m.getNsPrefixMap().get(prefix), base);
						data.setNsPrefix(prefix, uri);
					}
				} catch (JenaException ex) {
					throw new ConfigurationException("Error reading <" + fileName + ">: " + ex.getMessage());
				}
			}
			result = new ModelDataSource(data);
		}

		// If conf:datasetURIPattern is set, then filter the dataset accordingly.
		if (hasProperty(CONF.datasetURIPattern)) {
			final Pattern pattern = Pattern.compile(getString(CONF.datasetURIPattern));
			result = new FilteredDataSource(result) {
				@Override
				public boolean canDescribe(String absoluteIRI) {
					return pattern.matcher(absoluteIRI).find();
				}
			};
		}
		
		IRIRewriter rewriter = IRIRewriter.identity;
		
		// If conf:datasetBase is set (and different from conf:webBase),
		// rewrite the IRIs accordingly
		// Base IRI for IRIs considered to be "in" the data source
		String fullWebBase = configuration.getWebApplicationBaseURI() + 
				configuration.getWebResourcePrefix();
		String datasetBase = getIRI(CONF.datasetBase, 
				fullWebBase);
		if (!datasetBase.equals(fullWebBase)) {
			rewriter = IRIRewriter.createNamespaceBased(datasetBase, fullWebBase);
		}

		// Escape special characters in IRIs
		rewriter = IRIRewriter.chain(rewriter, new PubbyIRIEscaper(
				fullWebBase, !supportsIRIs()));

		result = new RewrittenDataSource(
				result, rewriter, addSameAsStatements());

		// Determine all browsable namespaces for this dataset
		final Set<String> browsableNamespaces = new HashSet<String>();
		browsableNamespaces.add(fullWebBase);
		for (String iri: getBrowsableNamespaces()) {
			browsableNamespaces.add(iri);
		}
		browsableNamespaces.addAll(configuration.getBrowsableNamespaces());

		// Filter the dataset to keep only those resources in the datasetBase
		// and in browsable namespaces, unless it's an annotation provider
		if (!hasType(CONF.AnnotationProvider)) {
			result = new FilteredDataSource(result) {
				@Override
				public boolean canDescribe(String absoluteIRI) {
					for (String namespace: browsableNamespaces) {
						if (absoluteIRI.startsWith(namespace)) return true;
					}
					return false;
				}
			};
		}
		
		return result;
	}
}
