package eu.dm2e.linkeddata.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import eu.dm2e.ws.NS;

public class Collection extends BaseModel implements Serializable {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 8312271053125465471L;

	private String providerId;
	public String getProviderId() { return providerId; }
	private String collectionId;
	public String getCollectionId() { return collectionId; }
	public Collection(String apiBase, Model model, String providerId, String collectionId) {
		this.apiBase = apiBase;
		this.model = null != model ? model : ModelFactory.createDefaultModel();
		this.providerId = providerId;
		this.collectionId = collectionId;
	}
	@Override
	public String getRetrievalUri() { return getCollectionUri(); }
	public String getCollectionUri() { return String.format("%s/dataset/%s/%s", apiBase, providerId, collectionId); }
	public Resource getCollectionResource() { return this.model.createResource(getCollectionUri()); }
	private boolean isRead = false;
	public void read() {
		long t0 = System.currentTimeMillis();
		getModel().read(getCollectionUri());
		long t1 = System.currentTimeMillis();
		log.debug(String.format("Reading of collection '%s/%s' took %sms", providerId, collectionId, (t1-t0)));
		isRead = true;
	}
	
	/**
	 * List the version ids of a collections
	 * @return Set of VersionedDataset IDs
	 */
	public Set<String> listVersionIds() {
		if (! isRead) read();
		HashSet<String> set = new HashSet<>();
		StmtIterator iter = getModel().listStatements(
				getCollectionResource(),
				getModel().createProperty(NS.DM2E_UNOFFICIAL.PROP_HAS_VERSION),
				(Resource) null);
		while (iter.hasNext()) {
			Resource res = iter.next().getObject().asResource();
			String id = res.toString().replace(getCollectionUri() + "/", "");
			set.add(id);
		}
		return set;
	}
	/**
	 * List the versioned Datasets in a collection
	 * @return Set of VersionedDatasets
	 */
	public Set<VersionedDataset> listVersions() {
		Set<VersionedDataset> versionedDatasetSet = new HashSet<>();
		Set<String> versionIds = listVersionIds();
		for (String versionId : versionIds) {
			versionedDatasetSet.add(getVersion(versionId));
		}
		return versionedDatasetSet;
	}

	/**
	 * Returns the id of the latest version of a dataset
	 * @return ID of the latest Version of a Dataset
	 */
	public String getLatestVersionId() {
		Set<String> setOfVersions = this.listVersionIds();
		ArrayList<String> listOfVersions = new ArrayList<>(setOfVersions);
		Collections.sort(listOfVersions);
		return listOfVersions.get(listOfVersions.size() - 1);
	}
	public VersionedDataset getLatestVersion() {
		return getVersion(getLatestVersionId());
	}
	public VersionedDataset getVersion(String versionId) {
		return new VersionedDataset(apiBase, null, providerId, collectionId, versionId);
	}
}
