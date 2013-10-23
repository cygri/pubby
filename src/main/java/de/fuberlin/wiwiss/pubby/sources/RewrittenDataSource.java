package de.fuberlin.wiwiss.pubby.sources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;

import de.fuberlin.wiwiss.pubby.IRIRewriter;
import de.fuberlin.wiwiss.pubby.ModelUtil;

/**
 * Wraps a {@link DataSource} by applying a {@link IRIRewriter}. The result
 * is a data source that contains the same data as the original, but with
 * all IRIs replaced according to the rewriter.
 * 
 * Optionally, may add <code>owl:sameAs</code> statements to indicate that the
 * rewritten and original IRIs identify the same entity.
 */
public class RewrittenDataSource implements DataSource {
	private final DataSource original;
	private final IRIRewriter rewriter;
	private final boolean addSameAs;
	
	public RewrittenDataSource(DataSource original, IRIRewriter rewriter) {
		this(original, rewriter, false);
	}
	
	public RewrittenDataSource(DataSource original, IRIRewriter rewriter,
			boolean addSameAsStatements) {
		this.original = original;
		this.rewriter = rewriter;
		this.addSameAs = addSameAsStatements;
	}
	
	private boolean isOriginalIRI(String absoluteIRI) {
		try {
			rewriter.unrewrite(absoluteIRI);
			return false;
		} catch (IllegalArgumentException ex) {
			// Tried to unrewrite an IRI that is already in original form
			return true;
		}
	}
	
	@Override
	public boolean canDescribe(String absoluteIRI) {
		if (isOriginalIRI(absoluteIRI)) {
			// According to our logic, the original namespace is empty
			// because we transplanted it. It only contains a sameAs
			// statements for every resource in it.
			return addSameAs && original.canDescribe(absoluteIRI);
		}
		return original.canDescribe(rewriter.unrewrite(absoluteIRI));
	}

	@Override
	public Model describeResource(String iri) {
		if (isOriginalIRI(iri)) {
			// According to our logic, the original namespace is empty
			// because we transplanted it. It only contains a sameAs
			// statements for every resource in it.
			if (!addSameAs || original.describeResource(iri).isEmpty()) {
				return ModelUtil.EMPTY_MODEL;
			}
			Model result = ModelFactory.createDefaultModel();
			addSameAsStatement(result, rewriter.rewrite(iri));
			return result;
		}
		// Normal case -- a rewritten IRI
		Model result = rewriter.rewrite(
				original.describeResource(
						rewriter.unrewrite(iri)));
		if (addSameAs && !result.isEmpty()) {
			addSameAsStatement(result, iri);
		}
		return result;
	}

	@Override
	public Map<Property, Integer> getHighIndegreeProperties(String resourceIRI) {
		if (isOriginalIRI(resourceIRI)) return null;
		return rewriter.rewrite(
				original.getHighIndegreeProperties(
						rewriter.unrewrite(resourceIRI)));
	}

	@Override
	public Map<Property, Integer> getHighOutdegreeProperties(String resourceIRI) {
		if (isOriginalIRI(resourceIRI)) return null;
		return rewriter.rewrite(
				original.getHighOutdegreeProperties(
						rewriter.unrewrite(resourceIRI)));
	}

	@Override
	public Model listPropertyValues(String resourceIRI, Property property,
			boolean isInverse) {
		if (isOriginalIRI(resourceIRI)) {
			// According to our logic, the original namespace is empty
			// because we transplanted it. It only contains a sameAs
			// statements for every resource in it.
			if (!addSameAs || !property.equals(OWL.sameAs) || !isInverse || 
					original.describeResource(resourceIRI).isEmpty()) {
				return ModelUtil.EMPTY_MODEL;
			}
			Model result = ModelFactory.createDefaultModel();
			addSameAsStatement(result, rewriter.rewrite(resourceIRI));
			return result;
		}
		// Normal case -- a rewritten IRI
		Model result = rewriter.rewrite(
				original.listPropertyValues(
						rewriter.unrewrite(resourceIRI), 
						rewriter.unrewrite(property), 
						isInverse));
		if (addSameAs && !result.isEmpty() && property.equals(OWL.sameAs) && !isInverse) {
			addSameAsStatement(result, resourceIRI);
		}
		return result;
	}

	@Override
	public List<Resource> getIndex() {
		List<Resource> originalIndex = original.getIndex();
		List<Resource> result = new ArrayList<Resource>(originalIndex.size());
		for (Resource r: originalIndex) {
			if (!canDescribe(r.getURI())) continue;
			result.add(rewriter.rewrite(r));
		}
		return result;
	}
	
	private void addSameAsStatement(Model model, String rewrittenIRI) {
		String originalIRI = rewriter.unrewrite(rewrittenIRI);
		Resource rewritten = model.getResource(rewrittenIRI);
		Resource unrewritten = model.getResource(originalIRI);
		if (rewritten.equals(unrewritten)) return;
		rewritten.addProperty(OWL.sameAs, unrewritten);
		ModelUtil.addNSIfUndefined(model, "owl", OWL.NS);
	}
}
