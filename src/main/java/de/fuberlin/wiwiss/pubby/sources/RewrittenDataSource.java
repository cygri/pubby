package de.fuberlin.wiwiss.pubby.sources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
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
	
	@Override
	public boolean canDescribe(String absoluteIRI) {
		return true;
	}

	@Override
	public Model describeResource(String iri) {
		Model result = rewriter.rewrite(
				original.describeResource(
						rewriter.unrewrite(iri)));
		addSameAsStatement(result, iri);
		return result;
	}

	@Override
	public Map<Property, Integer> getHighIndegreeProperties(String resourceIRI) {
		return rewriter.rewrite(
				original.getHighIndegreeProperties(
						rewriter.unrewrite(resourceIRI)));
	}

	@Override
	public Map<Property, Integer> getHighOutdegreeProperties(String resourceIRI) {
		return rewriter.rewrite(
				original.getHighOutdegreeProperties(
						rewriter.unrewrite(resourceIRI)));
	}

	@Override
	public Model listPropertyValues(String resourceIRI, Property property,
			boolean isInverse) {
		Model result = rewriter.rewrite(
				original.listPropertyValues(
						rewriter.unrewrite(resourceIRI), 
						rewriter.unrewrite(property), 
						isInverse));
		if (property.equals(OWL.sameAs) && !isInverse) {
			addSameAsStatement(result, resourceIRI);
		}
		return result;
	}

	@Override
	public List<Resource> getIndex() {
		List<Resource> originalIndex = original.getIndex();
		List<Resource> result = new ArrayList<Resource>(originalIndex.size());
		for (Resource r: originalIndex) {
			result.add(rewriter.rewrite(r));
		}
		return result;
	}
	
	private void addSameAsStatement(Model model, String rewrittenIRI) {
		if (!addSameAs || model.isEmpty()) return;
		String originalIRI = rewriter.unrewrite(rewrittenIRI);
		Resource rewritten = model.getResource(rewrittenIRI);
		Resource unrewritten = model.getResource(originalIRI);
		if (rewritten.equals(unrewritten)) return;
		rewritten.addProperty(OWL.sameAs, unrewritten);
		ModelUtil.addNSIfUndefined(model, "owl", OWL.NS);
	}
}
