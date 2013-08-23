package de.fuberlin.wiwiss.pubby;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


public class PageResourceDescription extends AbsResourceDescription {

	public PageResourceDescription(HypermediaResource resource, Model model, Configuration config) {
		super(resource, model, config);
	}

	public PageResourceDescription(Resource resource, Model model, Configuration config) {
		super(resource, model, config);
	}

	SimplePrefixMapping constructPrefixMapping() {
		return new SimplePrefixMapping(config, model, config.getPrefixes());
	}

}
