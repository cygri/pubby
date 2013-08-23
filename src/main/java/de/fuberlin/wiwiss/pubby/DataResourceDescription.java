package de.fuberlin.wiwiss.pubby;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


public class DataResourceDescription extends AbsResourceDescription {

	public DataResourceDescription(HypermediaResource resource, Model model, Configuration config) {
		super(resource, model, config);
	}

	public DataResourceDescription(Resource resource, Model model, Configuration config) {
		super(resource, model, config);
	}

	SimplePrefixMapping constructPrefixMapping() {
		return new SimplePrefixMapping(model, config.getPrefixes());
	}

}
