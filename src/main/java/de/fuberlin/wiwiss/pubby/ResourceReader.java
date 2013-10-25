package de.fuberlin.wiwiss.pubby;

import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.PrintUtil;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.XSD;

import de.fuberlin.wiwiss.pubby.vocab.CONF;

/**
 * Convenience superclass for classes that read properties of
 * an RDF resource, intended for making the reading of configuration
 * files easier. 
 */
public abstract class ResourceReader {
	private final Resource resource;
	
	public ResourceReader(Resource resource) {
		this.resource = resource;
	}
	
	public Resource getSelf() {
		return resource;
	}
	
	public Model getModel() {
		return resource.getModel();
	}

	public boolean hasType(Resource class_) {
		return resource.hasProperty(RDF.type, class_);
	}
	
	public boolean hasProperty(Property p) {
		return resource.hasProperty(p);
	}
	
	public Resource getResource(Property p) {
		if (!resource.hasProperty(p)) return null;
		assertHasOneValue(p);
		assertResourceValue(p);
		return resource.getProperty(p).getResource();		
	}
	
	public Set<Resource> getResources(Property p) {
		Set<Resource> result = new HashSet<Resource>();
		StmtIterator it = resource.listProperties(p);
		while (it.hasNext()) {
			Statement stmt = it.next();
			assertResourceValue(stmt);
			result.add(stmt.getResource());
		}
		return result;
	}

	public Set<Property> getProperties(Property p) {
		Set<Property> result = new HashSet<Property>();
		for (Resource r: getResources(CONF.labelProperty)) {
			result.add(r.as(Property.class));
		}
		return result;
	}
	
	public String getIRI(Property p) {
		return getIRI(p, null);
	}
	
	public String getIRI(Property p, String defaultValue) {
		if (!resource.hasProperty(p)) return defaultValue;
		assertHasOneValue(p);
		assertIRIValue(p);
		return resource.getProperty(p).getResource().getURI();		
	}
	
	public String getRequiredIRI(Property p) {
		if (!resource.hasProperty(p)) {
			raiseMissingProperty(p);
		}
		return getIRI(p);
	}
	
	public Set<String> getIRIs(Property p) {
		Set<String> result = new HashSet<String>();
		StmtIterator it = resource.listProperties(p);
		while (it.hasNext()) {
			Statement stmt = it.next();
			assertIRIValue(stmt);
			result.add(stmt.getResource().getURI());
		}
		return result;
	}
	
	public String getString(Property p) {
		return getString(p, null);
	}
	
	public String getString(Property p, String defaultValue) {
		if (!resource.hasProperty(p)) {
			return defaultValue;
		}
		assertHasOneValue(p);
		assertString(resource.getProperty(p));
		return resource.getProperty(p).getString();
	}
	
	public Set<String> getStrings(Property p) {
		Set<String> result = new HashSet<String>();
		StmtIterator it = resource.listProperties(p);
		while (it.hasNext()) {
			Statement stmt = it.next();
			assertString(stmt);
			result.add(stmt.getString());
		}
		return result;
	}

	public boolean getBoolean(Property p, boolean defaultValue) {
		if (!resource.hasProperty(p)) {
			return defaultValue;
		}
		assertHasOneValue(p);
		assertLiteralValue(p);
		Literal value = resource.getProperty(p).getLiteral();
		if (XSD.xboolean.getURI().equals(value.getDatatypeURI())) {
			return value.getBoolean();
		}
		if (value.getDatatypeURI() == null || XSD.xstring.getURI().equals(value.getDatatypeURI())) {
			if ("true".equals(value.getString().toLowerCase())
					|| "false".equals(value.getString().toLowerCase())) {
				return "true".equals(value.getString().toLowerCase());
			}
		}
		raiseUnexpectedDatatype("xsd:boolean", resource.getProperty(p));
		return false;
	}

	public void requireExactlyOneOf(Property... properties) {
		Property found = null;
		boolean first = true;
		StringBuilder s = new StringBuilder();
		for (Property p: properties) {
			if (resource.hasProperty(p)) {
				if (found == null) {
					found = p;
				} else {
					throw new ConfigurationException("Can't have both " +
							pretty(found) + " and " + pretty(p) + 
							" on resource " + pretty(resource));
				}
			}
			if (!first) {
				s.append(", ");
			}
			s.append(pretty(p));
			first = false;
		}
		if (found == null) {
			throw new ConfigurationException("One of " + s.toString() + 
					" required on resource " + pretty(resource));
		}
	}
	
	private void assertHasOneValue(Property p) {
		StmtIterator it = resource.listProperties(p);
		if (!it.hasNext()) {
			throw new ConfigurationException("Missing property " + pretty(p) + 
					" on resource " + pretty(resource));
		}
		it.next();
		if (it.hasNext()) {
			throw new ConfigurationException("Too many values for property " +
					pretty(p) + " on resource " + pretty(resource));
		}
	}
	
	private void assertResourceValue(Property p) {
		assertResourceValue(resource.getProperty(p));
	}
	
	private void assertResourceValue(Statement stmt) {
		if (stmt.getObject().isLiteral()) {
			throw new ConfigurationException(
					"Expected resource object, found literal: " + pretty(stmt));
		}
	}
	
	private void assertIRIValue(Property p) {
		assertIRIValue(resource.getProperty(p));
	}
	
	private void assertIRIValue(Statement stmt) {
		if (stmt.getObject().isLiteral()) {
			throw new ConfigurationException(
					"Expected IRI object, found literal: " + pretty(stmt));
		}
		if (stmt.getObject().isAnon()) {
			throw new ConfigurationException(
					"Expected IRI object, found blank node: " + pretty(stmt));
		}
	}
	
	private void assertLiteralValue(Property p) {
		assertLiteralValue(resource.getProperty(p));
	}
	
	private void assertLiteralValue(Statement stmt) {
		if (stmt.getObject().isURIResource()) {
			throw new ConfigurationException(
					"Expected literal object, found IRI: " + pretty(stmt));
		}
		if (stmt.getObject().isAnon()) {
			throw new ConfigurationException(
					"Expected literal object, found blank node: " + pretty(stmt));
		}
	}
	
	private void assertString(Statement stmt) {
		assertLiteralValue(stmt);
		Literal value = stmt.getLiteral();
		if (value.getDatatypeURI() == null || 
				XSD.xstring.getURI().equals(value.getDatatypeURI()) ||
				(RDF.getURI() + "langString").equals(value.getDatatypeURI())) {
			return;
		}
		raiseUnexpectedDatatype("string", stmt);
	}
	
	private String pretty(RDFNode node) {
		if (node.isAnon()) return "[]";
		if (node.isURIResource()) {
			Resource r = node.asResource();
			if (getModel().qnameFor(r.getURI()) == null) {
				return "<" + r.getURI() + ">";
			}
			return getModel().qnameFor(r.getURI());
		}
		return PrintUtil.print(node);
	}
	
	private String pretty(Statement stmt) {
		return pretty(stmt.getSubject()) + " " + pretty(stmt.getPredicate())
				+ " " + pretty(stmt.getObject()) + ".";
	}

	private void raiseMissingProperty(Property p) {
		throw new ConfigurationException("Missing property " + 
				pretty(p) + " on resource " + pretty(resource));
	}
	
	private void raiseUnexpectedDatatype(String expectedDatatype, Statement stmt) {
		throw new ConfigurationException(
				"Expected " + expectedDatatype + 
				" object, found other datatype: " + 
				pretty(stmt));
	}
}
