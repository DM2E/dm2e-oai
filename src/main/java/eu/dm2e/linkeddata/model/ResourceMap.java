package eu.dm2e.linkeddata.model;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Encapsulating a resource map with references to the "main" Aggregation and CHO of this resource map
 */
public class ResourceMap {
	private Model model;
	public Model getModel() { return model; }
	private Resource aggregation;
	public Resource getAggregation() { return aggregation; }
	private Resource providedCHO;
	public Resource getProvidedCHO() { return providedCHO; }
	private String uri;
	public String getUri() { return uri; }

	public ResourceMap(String uri, Model model, Resource aggregation) {
		this.uri = uri;
		this.model = model;
		this.aggregation = model.createResource(aggregation.getURI());
		this.providedCHO = model.createResource(uri);
	}
}
