package de.fuberlin.wiwiss.pubby;


/**
 * A resource that is mapped between the SPARQL dataset and the Web server.
 * 
 * TODO: This class is kind of silly. Merge with {@link IRITranslator} to a single class that can map back and forth?
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class MappedResource {
	private final String datasetURI;
	private final Dataset datasetConfig;
	private final HypermediaResource hypermediaResource;
	
	public MappedResource(String relativeWebURI, String datasetURI, 
			Configuration config, Dataset dataset) {
		this.hypermediaResource = config.getController(relativeWebURI, false);
		this.datasetURI = datasetURI;
		this.datasetConfig = dataset;
	}

	/**
	 * @return The dataset which contains the description of this resource
	 */
	public Dataset getDataset() {
		return datasetConfig;
	}
	
	/**
	 * @return the resource's URI within the SPARQL dataset
	 */
	public String getDatasetURI() {
		return datasetURI;
	}
	
	public HypermediaResource getController() {
		return hypermediaResource;
	}
}
