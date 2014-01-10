package eu.dm2e.linkeddata.model;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import eu.dm2e.ws.NS;

public class ThingWithPrefLabel extends BaseModel {

	private Object	providerId;
	private Object	collectionId;
	private String	uriLabel;
	private String	retrievalUri;
	private String	conceptUri;
	public Resource getConceptResource() { return getModel().createResource(getConceptUri()); }
	
	public ThingWithPrefLabel(String apiBase, Model model, String providerId, String datasetId, String uriLabel) {
		// TODO Auto-generated constructor stub
		this.apiBase = apiBase;
		this.model = null != model ? model : ModelFactory.createDefaultModel();
		this.providerId = providerId;
		this.collectionId = datasetId;
		this.uriLabel = uriLabel;
	}
	
	public ThingWithPrefLabel(String apiBase, Model model, String retrievalUri) {
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

}
