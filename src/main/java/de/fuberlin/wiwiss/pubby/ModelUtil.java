package de.fuberlin.wiwiss.pubby;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.PrefixMapping;

public class ModelUtil {

	/**
	 * Adds the source model to the target model, modifying the target
	 * in place. Overrides any prefixes in the target with those from
	 * the source.
	 */
	public static void mergeModels(Model target, Model source) {
		target.add(source);
		for (String prefix: source.getNsPrefixMap().keySet()) {
			target.setNsPrefix(prefix, source.getNsPrefixURI(prefix));
		}
	}
	
	public static void addNSIfUndefined(PrefixMapping m, String prefix, String uri) {
		if (m.getNsURIPrefix(uri) != null) return;
		if (m.getNsPrefixURI(prefix) != null) return;
		m.setNsPrefix(prefix, uri);
	}
	
	/**
	 * Singleton; only public static methods
	 */
	private ModelUtil() {}
}
