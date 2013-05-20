package cz.cvut.kbss.pubby.vocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Petr Křemen 2013
 * petr@sio2.cz
 */
public class GEO {

    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static Model m_model = ModelFactory.createDefaultModel();

    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://www.w3.org/2003/01/geo/wgs84_pos#";

    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}

    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );

    /** <p>Longitude property.</p> */
    public static final Property P_LONG = m_model.createProperty( NS + "long" );

    /** <p>Latitude property.</p> */
    public static final Property P_LAT = m_model.createProperty( NS + "lat" );

    /** <p>Latitude property.</p> */
    public static final Property P_LOCATION = m_model.createProperty( NS + "location" );
}