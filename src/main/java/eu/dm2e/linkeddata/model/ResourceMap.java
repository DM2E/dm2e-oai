package eu.dm2e.linkeddata.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.dm2e.ws.NS;

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
	public String toString() {
		return "ResourceMap " + getResourceMapUri();
	}

	@Override
	public String getRetrievalUri() { return getResourceMapUri(); }
	public QuerySolution getAllTitles() {
		Map<String,String> prefixMap = new HashMap<>();
		prefixMap.put("dm2e", NS.DM2E.BASE);
		prefixMap.put("dc", NS.DC.BASE);
		prefixMap.put("rdf", NS.RDF.BASE);
		prefixMap.put("edm", NS.EDM.BASE);
		prefixMap.put("dcterms", NS.DCTERMS.BASE);
		ParameterizedSparqlString sb = new ParameterizedSparqlString();
		sb.setNsPrefixes(prefixMap);
		sb.setParam("cho", getProvidedCHO_Resource());
		sb.append("SELECT ?dcterms_title ?dm2e_subtitle WHERE {  \n");
		sb.append("   OPTIONAL { ?cho dcterms:title ?dcterms_title . }  \n");
		sb.append("   OPTIONAL { ?cho dc:title ?dc_title . }  \n");
		sb.append("   OPTIONAL { ?cho dm2e:subtitle ?dm2e_subtitle . }  \n");
		sb.append(" } ");
		Query query = QueryFactory.create(sb.toString());
		QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
		QuerySolution soln = null;
		try {
			ResultSet results = qexec.execSelect() ;
			if (results.hasNext()) soln = results.nextSolution();
		} finally { qexec.close(); }
		return soln;
	}
	public String getFirstPageLink() {
		Map<String,String> prefixMap = new HashMap<>();
		prefixMap.put("dm2e", NS.DM2E.BASE);
		prefixMap.put("dc", NS.DC.BASE);
		prefixMap.put("rdf", NS.RDF.BASE);
		prefixMap.put("edm", NS.EDM.BASE);
		prefixMap.put("edm", NS.EDM.BASE);
		List<String> pageLinks = new ArrayList<>();
		{
			ParameterizedSparqlString sb = new ParameterizedSparqlString();
			sb.setNsPrefixes(prefixMap);
			sb.setParam("parentCHO", getProvidedCHO_Resource());
			sb.append("SELECT DISTINCT ?pageCHO WHERE {  \n");
			sb.append("   ?cho dc:isPartOf* ?parentCHO .  \n");
			sb.append("   ?pageCHO dc:type dm2e:Page .  \n");
			sb.append(" } ");
			Query query = QueryFactory.create(sb.toString());
			QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
			try {
				ResultSet results = qexec.execSelect() ;
				for ( ; results.hasNext() ; ) {
					QuerySolution soln = results.nextSolution() ;
					pageLinks.add(soln.get("pageCHO").toString());
//					log.debug("Page found: " + soln.get("pageCHO"));
				}
			} finally { qexec.close() ; }
		}
		Collections.sort(pageLinks);
		String firstPageLink = null;
		if (pageLinks.size() > 0) firstPageLink = pageLinks.get(0);
		log.debug("First Page: " + firstPageLink);
		return firstPageLink;
	}

	public String getThumbnailLink() {
		Map<String,String> prefixMap = new HashMap<>();
		prefixMap.put("dm2e", NS.DM2E.BASE);
		prefixMap.put("dc", NS.DC.BASE);
		prefixMap.put("rdf", NS.RDF.BASE);
		prefixMap.put("edm", NS.EDM.BASE);
		prefixMap.put("edm", NS.EDM.BASE);
		List<Query> queries = new ArrayList<>();
		{
			ParameterizedSparqlString sb = new ParameterizedSparqlString();
			sb.setNsPrefixes(prefixMap);
			sb.setParam("parentCHO", getProvidedCHO_Resource());
			sb.append("SELECT ?thumbnail WHERE {  \n");
			sb.append("   ?cho dc:isPartOf* ?parentCHO .  \n");
			sb.append("   ?cho dc:type dm2e:Page .  \n");
			sb.append("   ?agg edm:object ?thumbnail .  \n");
			sb.append(" } ");
			queries.add(QueryFactory.create(sb.toString()));
		}
		{
			ParameterizedSparqlString sb = new ParameterizedSparqlString();
			sb.setNsPrefixes(prefixMap);
			sb.setParam("parentCHO", getProvidedCHO_Resource());
			sb.append("SELECT ?thumbnail WHERE {  \n");
			sb.append("   ?cho dc:isPartOf* ?parentCHO .  \n");
			sb.append("   ?cho dc:type dm2e:Page .  \n");
			sb.append("   ?agg dm2e:hasAnnotatableVersionAt ?thumbnail .  \n");
			sb.append(" } ");
			queries.add(QueryFactory.create(sb.toString()));
		}
		{
			ParameterizedSparqlString sb = new ParameterizedSparqlString();
			sb.setNsPrefixes(prefixMap);
			sb.setParam("parentCHO", getProvidedCHO_Resource());
			sb.append("SELECT ?thumbnail WHERE {  \n");
			sb.append("   ?cho dc:isPartOf* ?parentCHO .  \n");
			sb.append("   ?cho dc:type dm2e:Page .  \n");
			sb.append("   ?agg edm:isShownBy ?thumbnail .  \n");
			sb.append(" } ");
			queries.add(QueryFactory.create(sb.toString()));
		}
		
		String thumbnail = null;
		QUERY_LOOP:
		for (Query query : queries ) {
			QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
			try {
				ResultSet results = qexec.execSelect() ;
				for ( ; results.hasNext() ; ) {
					QuerySolution soln = results.nextSolution() ;
					thumbnail = soln.get("thumbnail").toString();
					log.debug("Thumbnail found: " + soln.get("?thumbnail"));
					break QUERY_LOOP;
				}
			} finally { qexec.close() ; }
		}
		return thumbnail;
	}
}
