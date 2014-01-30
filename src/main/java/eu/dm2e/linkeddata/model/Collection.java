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
import com.hp.hpl.jena.util.FileManager;

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
	public Collection(FileManager fm, String apiBase, Model model, String providerId, String collectionId) {
		super(fm);
		this.apiBase = apiBase;
		this.model = null != model ? model : ModelFactory.createDefaultModel();
		this.providerId = providerId;
		this.collectionId = collectionId;
	}
	public Collection(FileManager fm, String apiBase, String fromUri, IdentifierType type) {
		super(fm);
		this.model = ModelFactory.createDefaultModel();
		this.apiBase = apiBase;

		if (type.equals(IdentifierType.OAI_IDENTIFIER)) {
			// Parse as oai identifier
			// oai:dm2e:bbaw:dta:20863:1386762086592
			fromUri = fromUri.replace("__", "/");
			String[] s = fromUri.split(":");
			if (s.length != 5) {
				throw new IllegalArgumentException("Identifier '" + fromUri +"' is not a valid OAI identifier");
			}
			this.providerId = s[2];
			this.collectionId = s[3];
		} else if (type.equals(IdentifierType.OAI_SET_SPEC)) {
			String[] s = fromUri.split(":");
			String setType = s[0];
			log.debug("Type is " + setType);
			if (setType.equals("collection")) {
				if (s.length != 3) {
					throw new IllegalArgumentException("Identifier '" + fromUri +"' is not a valid collection setSpec");
				}
				this.providerId = s[1];
				this.collectionId = s[2];
			}
		}
	}
	@Override
	public String getRetrievalUri() { return getCollectionUri(); }
	public String getCollectionUri() { return String.format("%s/dataset/%s/%s", apiBase, providerId, collectionId); }
	public Resource getCollectionResource() { return getModel().createResource(getCollectionUri()); }
//	public void read() {
//		long t0 = System.currentTimeMillis();
//		getModel().read(getCollectionUri());
//		long t1 = System.currentTimeMillis();
//		log.debug(String.format("Reading of collection '%s/%s' took %sms", providerId, collectionId, (t1-t0)));
//		isRead = true;
//	}
	
	/**
	 * List the version ids of a collections
	 * @return Set of VersionedDataset IDs
	 */
	// NOTE make sure the collection model is actually read!
	public Set<String> listVersionIds() {
		HashSet<String> set = new HashSet<String>();
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
		Set<VersionedDataset> versionedDatasetSet = new HashSet<VersionedDataset>();
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
		ArrayList<String> listOfVersions = new ArrayList<String>(setOfVersions);
		Collections.sort(listOfVersions);
		if (listOfVersions.size() == 0) {
			return null;
		}
		return listOfVersions.get(listOfVersions.size() - 1);
	}
	public VersionedDataset getLatestVersion() {
		return getVersion(getLatestVersionId());
	}
	public VersionedDataset getVersion(String versionId) {
		if (null == versionId) {
			return null;
		}
		return new VersionedDataset(this.fileManager, apiBase, null, providerId, collectionId, versionId);
	}
}
