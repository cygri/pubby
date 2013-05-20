package cz.cvut.kbss.pubby.servlets;

import com.hp.hpl.jena.rdf.model.Resource;
import de.fuberlin.wiwiss.pubby.ResourceDescription;

/**
 * Petr Křemen, 2013
 * petr@sio2.cz
 */
public interface ResourceDescriptionProvider {
     public Resource get(String url);
}
