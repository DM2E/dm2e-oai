package eu.dm2e.linkeddata.model;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;

import net.sf.ehcache.pool.sizeof.annotations.IgnoreSizeOf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Encapsulating a resource map with references to the "main" Aggregation and CHO of this resource map
 * 
 * http://$API_BASE/$providerId/datasetId/$resourceMapId[/$versionId]
 */
public class ResourceMap extends BaseModel implements Serializable{


	/**
	 * 
	 */
	private static final long	serialVersionUID	= 4761678119692905128L;

	Logger log = LoggerFactory.getLogger(getClass().getName());

	private String providerId;
	public String getProviderId() { return providerId; }
	private String collectionId;
	public String getCollectionId() { return collectionId; }
	private String versionId;
	public String getVersionId() { return versionId; }
	private String	itemId;
	public String getItemId() { return this.itemId; }

	public ResourceMap(String apiBase, Model model, String providerId, String datasetId, String itemId, String versionId) {
		this.apiBase = apiBase;
		this.model = null  != model ? model : ModelFactory.createDefaultModel();
		this.providerId = providerId;
		this.collectionId = datasetId;
		this.itemId = itemId;
		this.versionId = versionId;
	}
	
	public ResourceMap(String apiBase, String fromUri, IdentifierType type, String versionId) {
		this.apiBase = apiBase;
		this.model = ModelFactory.createDefaultModel();
		this.versionId = versionId;

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
			this.itemId = s[4];
		}  else if (type.equals(IdentifierType.URL)) {
			// Parse as URI
			//	http://lelystad.informatik.uni-mannheim.de:3000/direct/item/bbaw/dta/20863/1386762086592
			fromUri = fromUri.replace(apiBase + "/", "");
			String[] s = fromUri.split("/", 3); // ['item', 'bbaw', 'dta/20863/1386762086592']
			this.providerId = s[1];	 			// 'bbaw'
			s = s[2].split("/", 2);				// ['dta', '20863/1386762086592']
			this.collectionId = s[0]; 			// 'dta'
			this.itemId = s[1].substring(0, s[1].lastIndexOf('/')); // '20863'
			this.versionId = s[1].substring(s[1].lastIndexOf('/') + 1); // '1386762086592'
		} else {
			throw new NotImplementedException();
		}
		
	}
	
	public String getResourceMapUri()  { return String.format("%s/resourcemap/%s/%s/%s/%s",	apiBase, providerId, collectionId, itemId, versionId); }
	public String getProvidedCHO_Uri() { return String.format("%s/item/%s/%s/%s", 	    apiBase, providerId, collectionId, itemId); }
	public String getAggregationUri()  { return String.format("%s/aggregation/%s/%s/%s",	apiBase, providerId, collectionId, itemId); }
	public Resource getResourceMapResource() { return this.getModel().createResource(getResourceMapUri()); }
	public Resource getProvidedCHO_Resource() { return this.getModel().createResource(getProvidedCHO_Uri()); }
	public Resource getAggregationResource() { return this.getModel().createResource(getAggregationUri()); }


	@Override
	public String getRetrievalUri() { return getResourceMapUri(); }

}
