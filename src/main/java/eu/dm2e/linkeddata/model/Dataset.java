package eu.dm2e.linkeddata.model;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Encapsulating a dataset including the RDF model of the dataset and it's
 * dataset/version IDs
 */
public class Dataset {

	private Model model;
	public Model getModel() { return model; }
	private String datasetId;
	public String getDatasetId() { return datasetId; }
	private String versionId;
	public String getVersionId() { return versionId; }
	private String uri;
	public String getUri() { return uri; }
	private Resource resource;
	public Resource getResource() { return resource; }

	public Dataset(String uri, Model model, String datasetId, String versionId) {
		this.uri = uri;
		this.resource = model.createResource(uri);
		this.model = model;
		this.datasetId = datasetId;
		this.versionId = versionId;
	}

}
