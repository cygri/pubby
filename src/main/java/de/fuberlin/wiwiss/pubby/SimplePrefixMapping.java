package de.fuberlin.wiwiss.pubby;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.hp.hpl.jena.shared.PrefixMapping;

class SimplePrefixMapping {
	
	final Map.Entry<String, String> uri2prefix[];

	SimplePrefixMapping(PrefixMapping ... mappings) {
		this(null, mappings);
	}

	@SuppressWarnings("unchecked")
	public SimplePrefixMapping(Configuration config, PrefixMapping ... mappings) {
		Map<String,String> uri2prefix = new HashMap<String,String>();
		for (PrefixMapping pm: mappings) {
			copyPrefixes(uri2prefix, pm);
		}
		if (config != null) {
			Map<String,String> newStuff = new HashMap<String,String>();
			for (Map.Entry<String, String> ns2p : uri2prefix.entrySet() ) {
				String uri = ns2p.getKey();
				String prefix = ns2p.getValue();
				newStuff.put(Dataset.escapeURIDelimiters(uri), prefix);
				MappedResource mapped = config.mapResource(uri);
				if (mapped != null) {
					newStuff.put(mapped.getController().getAbsoluteIRI(),prefix);
					newStuff.put(Dataset.escapeURIDelimiters(mapped.getController().getAbsoluteIRI()),prefix);
				}
			}
			newStuff.putAll(uri2prefix); // these ones take precedence
			uri2prefix = newStuff;
		}
		this.uri2prefix = uri2prefix.entrySet().toArray(new Map.Entry[uri2prefix.size()]);
		Arrays.sort(this.uri2prefix, new Comparator<Map.Entry<String, String>>(){

			@Override
			public int compare(Entry<String, String> uri1p1, Entry<String, String> uri2p2) {
				// longest uris first
				return uri2p2.getKey().length() - uri1p1.getKey().length();
			}});
	}

	private void copyPrefixes(Map<String,String> uri2prefix, PrefixMapping prefixMapping) {
		for (Map.Entry<String,String> p2ns: prefixMapping.getNsPrefixMap().entrySet()) {
			uri2prefix.put(p2ns.getValue(), p2ns.getKey());
		}
	}

	String lookupPrefix(String uri) {
		for (Map.Entry<String, String> ns2p : uri2prefix ) {
			if (uri.equals(ns2p.getKey())) {
				return ns2p.getValue();
			}
		}
		return null;
	}

	String getNamespace(String uri) {
		for (Map.Entry<String, String> ns2p : uri2prefix ) {
			if (uri.startsWith(ns2p.getKey())) {
				return ns2p.getKey();
			}
		}
		return null;
	}

	public String qnameFor(String uri) {
		String ns = getNamespace(uri);
		String prefix = lookupPrefix(ns);
		return prefix + ":" + uri.substring(ns.length());
	}

}
