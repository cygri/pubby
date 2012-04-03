package de.fuberlin.wiwiss.pubby.vocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
 
/**
 * Some vocabulary definitions from http://www.w3.org/1999/02/22-rdf-syntax-ns#
 * @author Sergio Fernández (sergio.fernandez@fundacionctic.org)
 */
public class RDF {
	
	private final static String NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	
    private static Model m_model = ModelFactory.createDefaultModel();

    public static final Property type = m_model.createProperty(NS+"type");
       
}
