package eu.dm2e.linkeddata.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

import eu.dm2e.ws.NS;

/**
 * Encapsulating a dataset including the RDF model of the dataset and it's
 * dataset/version IDs
 */
public class VersionedDataset extends BaseModel implements Serializable {
	

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 4552098072833374231L;

	private String providerId;
	public String getProviderId() { return providerId; }
	private String collectionId;
	public String getCollectionId() { return collectionId; }
	private String versionId;
	public String getVersionId() { return versionId; }
	public VersionedDataset(FileManager fm, String apiBase, Model model, String providerId, String collectionId, String versionId) {
		super(fm);
		this.apiBase = apiBase;
		this.model = null  != model ? model : ModelFactory.createDefaultModel();
		this.providerId = providerId;
		this.collectionId = collectionId;
		this.versionId = versionId;
	}
	
//	public VersionedDataset(String apiBase, String fromUri, IdentifierType type) {
//		
//		if (! type.equals(IdentifierType.OAI_IDENTIFIER)) {
//			throw new NotImplementedException();
//		}
//		
//	}

	public Resource getVersionedDatasetResource() {
		return this.getModel().createResource(this.getVersionedDatasetUri());
	}
	
	public String getVersionedDatasetUri() {
		return String.format("%s/dataset/%s/%s/%s",
                apiBase,
                providerId,
                collectionId,
                versionId
        );
	}
	@Override
	public String getRetrievalUri() { return getVersionedDatasetUri(); }

	public Set<ResourceMap> listResourceMaps() {
		StmtIterator resMapIter = getModel().listStatements(
				getVersionedDatasetResource(),
				getModel().createProperty(NS.DM2E_UNOFFICIAL.PROP_CONTAINS_CHO),
				(Resource) null);
		Set<ResourceMap> set = new HashSet<ResourceMap>();
		while (resMapIter.hasNext()) {
			Statement stmt = resMapIter.next();
			final String resMapUri = stmt.getObject().asResource().getURI();
//			log.debug("ResourceMap: " + resMapUri);
//			log.debug("versioneddataseturi: " + getVersionedDatasetUri());
			String resMapId = resMapUri.replaceFirst(".*/([^/?]+).*", "$1");
			log.debug("ResourceId: " + resMapId);
			ResourceMap resMap = new ResourceMap(this.fileManager, apiBase, null, providerId, collectionId, resMapId, versionId);
			set.add(resMap);
		}
		return set;
	}


}
