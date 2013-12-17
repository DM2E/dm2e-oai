package eu.dm2e.linkeddata;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import eu.dm2e.linkeddata.model.Dataset;
import eu.dm2e.linkeddata.model.ResourceMap;
import eu.dm2e.ws.NS;


public class Dm2eApiClient {
	
	private String	apiBase;

	public Dm2eApiClient(String apiBase) {
		// TODO Auto-generated constructor stub
		this.apiBase = apiBase;
	}
	
	public String dumpModel(Model model) {
		StringWriter strwriter = new StringWriter();
		model.write(strwriter, "N3");
		return strwriter.toString();
	}

	/**
	 * Return the latest version of a Dataset
	 * @param datasetId
	 * @return Latest Version of a Dataset
	 */
	public Dataset getDataset(String datasetId) {
		String latestVersionId = findLatestVersion(datasetId);
		return getDataset(datasetId, latestVersionId);
	}
	
	/**
	 * Return a dataset, defined by the dataset id and a version id
	 * @param datasetId
	 * @param versionId
	 * @return 
	 */
	// http://lelystad.informatik.uni-mannheim.de:3000/direct/dataset/bbaw/dta/1386762086592
	public Dataset getDataset(String datasetId, String versionId) {
		String uri = apiBase + "/dataset/" + datasetId + "/" + versionId;
		Model model = ModelFactory.createDefaultModel();
		model.read(uri);
		return new Dataset(uri, model, datasetId, versionId);
	}
	
	/**
	 * Returns the id of the latest version of a dataset
	 * @param datasetId
	 * @return ID of the latest Version of a Dataset
	 */
	public String findLatestVersion(String datasetId) {
		Set<String> setOfVersions = this.listVersions(datasetId);
		ArrayList<String> listOfVersions = new ArrayList<>(setOfVersions);
		Collections.sort(listOfVersions);
		return listOfVersions.get(listOfVersions.size() - 1);
	}

	/**
	 * List the version ids of a dataset
	 * @param datasetId
	 * @return Set of Dataset IDs
	 */
	public Set<String> listVersions(String datasetId) {
		Logger log = LoggerFactory.getLogger(getClass().getName());
		String uri = apiBase + "/dataset/" + datasetId;
		HashSet<String> set = new HashSet<>();
		Model model = ModelFactory.createDefaultModel();
		model.read(uri);
		log.trace("list versions response: " + dumpModel(model));
		StmtIterator iter = model.listStatements( model.createResource(uri),
				model.createProperty(NS.DM2E_UNOFFICIAL.PROP_HAS_VERSION), (Resource)null);
		while (iter.hasNext()) {
			Resource res = iter.next().getObject().asResource();
			String id = res.toString().replace(uri + "/", "");
			set.add(id);
		}
		return set;
	}
	
	/**
	 * List the collections on a server
	 * @return Set of collection IDs
	 */
	public Set<String> listDatasets() {
		Logger log = LoggerFactory.getLogger(getClass().getName());
		String uri = apiBase + "/list";
		HashSet<String> set = new HashSet<>();
		Model model = ModelFactory.createDefaultModel();
		model.read(uri);
		log.trace("list collections response: " + dumpModel(model));
		StmtIterator collectionIter = model.listStatements(
				model.createResource(uri),
				model.createProperty(NS.DM2E_UNOFFICIAL.PROP_HAS_COLLECTION),
				(Resource)null);
		while (collectionIter.hasNext()) {
			Resource collectionRes = collectionIter.next().getObject().asResource();
			String datasetId = collectionRes.toString().replace(apiBase + "/dataset/", "");
			set.add(datasetId);
		}
		return set;
	}
	
	public Set<String> listResourceMaps(Dataset ds) {
		Logger log = LoggerFactory.getLogger(getClass().getName());
		StmtIterator resMapIter = ds.getModel().listStatements(
				ds.getResource(),
				ds.getModel().createProperty(NS.DM2E_UNOFFICIAL.PROP_CONTAINS_CHO),
				(Resource)null
				);
		Set<String> set = new HashSet<>();
		while (resMapIter.hasNext()) {
			Statement stmt = resMapIter.next();
			final String resMapUri = stmt.getObject().asResource().getURI();
			String resMapId = resMapUri.replace(apiBase + "/item/" + ds.getDatasetId() + "/", "");
			log.debug("ResourceMap: " + resMapId);
			set.add(resMapId);
		}
		return set;
	}
	
	public ResourceMap getResourceMap(Dataset ds, String resourceMapId) {
		String uri = apiBase + "/item/" + ds.getDatasetId() + "/" + resourceMapId;
		Model model = ModelFactory.createDefaultModel();
		model.read(uri);
		Resource aggRes = model.createResource(uri.replace("/item/", "/aggregation/"));
		return new ResourceMap(uri, model, aggRes);
	}
	public ResourceMap getResourceMap(String datasetId, String resourceMapId) {
		String uri = apiBase + "/item/" + datasetId + "/" + resourceMapId;
		Model model = ModelFactory.createDefaultModel();
		model.read(uri);
		Resource aggRes = model.createResource(uri.replace("/item/", "/aggregation/"));
		return new ResourceMap(uri, model, aggRes);
	}
	
}