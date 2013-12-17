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
	private String datasetId;
	public String getDatasetId() { return datasetId; }
	private String	resourceMapId;
	public String getResourceMapId() { return this.resourceMapId; }

	public ResourceMap(Model model, Resource cho, Resource aggregation, String datasetId, String resourceMapId) {
		this.uri = cho.getURI();
		this.model = model;
		this.datasetId = datasetId;
		this.resourceMapId = resourceMapId;
		this.aggregation = model.createResource(aggregation.getURI());
		this.providedCHO = model.createResource(uri);
	}



}
