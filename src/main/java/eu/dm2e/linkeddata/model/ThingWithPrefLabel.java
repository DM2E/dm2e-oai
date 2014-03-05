package eu.dm2e.linkeddata.model;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

import eu.dm2e.NS;

public class ThingWithPrefLabel extends BaseModel {

	private Object	providerId;
	private Object	collectionId;
	private String	uriLabel;
	private String	retrievalUri;
	private String	conceptUri;
	public Resource getConceptResource() { return getModel().createResource(getConceptUri()); }
	
	public ThingWithPrefLabel(FileManager fm, String apiBase, Model model, String providerId, String datasetId, String uriLabel) {
		super(fm);
		this.apiBase = apiBase;
		this.model = null != model ? model : ModelFactory.createDefaultModel();
		this.providerId = providerId;
		this.collectionId = datasetId;
		this.uriLabel = uriLabel;
	}
	
	public ThingWithPrefLabel(FileManager fm, String apiBase, Model model, String retrievalUri) {
		super(fm);
		this.apiBase = apiBase;
		this.model = null != model ? model : ModelFactory.createDefaultModel();
		this.retrievalUri = retrievalUri;
		this.conceptUri = retrievalUri;
	}

	@Override
	public String getRetrievalUri() { return (retrievalUri != null) ? retrievalUri : getConceptUri(); }
	public String getConceptUri() {
		return (null != conceptUri) ? conceptUri : String.format("%s/concept/%s/%s/%s", apiBase, providerId, collectionId, uriLabel);
	}
	
	public String getPrefLabel() {
		StmtIterator stmtIter = this.model.listStatements(
				getConceptResource(), 
				getModel().createProperty(NS.SKOS.PROP_PREF_LABEL),
				(Literal) null);
		if (! stmtIter.hasNext()) return null;
		String prefLabel = stmtIter.next().getObject().asLiteral().getValue().toString();
		return prefLabel;
	}

	public String getRdfType() {
		StmtIterator stmtIter = this.model.listStatements(
				getConceptResource(), 
				getModel().createProperty(NS.RDF.PROP_TYPE),
				(Literal) null);
		if (! stmtIter.hasNext()) return NS.OWL.THING;
		return stmtIter.next().getObject().asResource().getURI();
	}

}
