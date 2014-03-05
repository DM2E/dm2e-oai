package eu.dm2e.linkeddata.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;

import eu.dm2e.NS;

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

	private static final HashMap<String, Set<String>> dcToDm2eProperties = new HashMap<>();
	static {
		final Set<String> dcSpatialPropertyUrls = new HashSet<>();
		dcSpatialPropertyUrls.add(NS.DM2E_UNVERSIONED.PROP_PRINTED_AT);
		dcSpatialPropertyUrls.add(NS.DM2E_UNVERSIONED.PROP_PUBLISHED_AT);
		dcSpatialPropertyUrls.add(NS.DM2E_UNVERSIONED.PROP_WRITTEN_AT);
		dcToDm2eProperties.put(NS.DCTERMS.PROP_SPATIAL, dcSpatialPropertyUrls);

		final Set<String> dcCreatorPropertyUrls = new HashSet<>();
		dcCreatorPropertyUrls.add(NS.DC.PROP_CREATOR);
		dcCreatorPropertyUrls.add(NS.PRO.PROP_AUTHOR);
		dcCreatorPropertyUrls.add(NS.DM2E_UNVERSIONED.PROP_WRITER);
		dcToDm2eProperties.put(NS.DC.PROP_CREATOR, dcCreatorPropertyUrls);
		
		final Set<String> dcContributorPropertyUrls = new HashSet<String>();
		dcContributorPropertyUrls.add(NS.DC.PROP_CONTRIBUTOR);
		dcContributorPropertyUrls.add(NS.DC.PROP_PUBLISHER);
		dcContributorPropertyUrls.add(NS.BIBO.PROP_EDITOR);
		dcContributorPropertyUrls.add(NS.PRO.PROP_PRINTER);
		dcContributorPropertyUrls.add(NS.PRO.PROP_TRANSLATOR);
		dcContributorPropertyUrls.add(NS.PRO.PROP_ILLUSTRATOR);
		dcToDm2eProperties.put(NS.DC.PROP_CONTRIBUTOR, dcContributorPropertyUrls);

		final Set<String> dcSubjectPropertyUrls = new HashSet<String>();
		dcSubjectPropertyUrls.add(NS.DC.PROP_SUBJECT);
		dcToDm2eProperties.put(NS.DC.PROP_SUBJECT, dcSubjectPropertyUrls);
	}

	Logger log = LoggerFactory.getLogger(getClass().getName());

	private final VersionedDataset versionedDataset;
	public VersionedDataset getVersionedDataset() { return versionedDataset; }
	private final String providerId;
	public String getProviderId() { return providerId; }
	private final String datasetId;
	public final String getDatasetId() { return datasetId; }
	private String versionId;
	public final String getVersionId() { return versionId; }
	private final String	itemId;
	public String getItemId() { return this.itemId; }

//	private ResourceMap(FileManager fileManager, String apiBase, Model model, String providerId, String datasetId, String itemId, String versionId) {
//		super(fileManager);
//		this.apiBase = apiBase;
//		this.model = null  != model ? model : ModelFactory.createDefaultModel();
//		this.providerId = providerId;
//		this.datasetId = datasetId;
//		this.itemId = itemId;
//		this.versionId = versionId;
//	}
	
	public static ResourceMap fromIdentifier(FileManager fileManager, String apiBase, String fromUri, IdentifierType type) {
		final String providerId;
		final String datasetId;
		final String itemId;
		final String versionId;
		if (type.equals(IdentifierType.OAI_IDENTIFIER)) {
			// Parse as oai identifier
			// oai:dm2e:bbaw:dta:20863:1386762086592
			fromUri = fromUri.replace("__", "/");
			String[] s = fromUri.split(":");
			if (s.length != 6) {
				throw new IllegalArgumentException("Identifier '" + fromUri +"' is not a valid OAI identifier");
			}
			providerId = s[2];
			datasetId = s[3];
			itemId = s[4];
			versionId = s[5];
		}  else if (type.equals(IdentifierType.URL)) {
			// Parse as URI
			//	http://lelystad.informatik.uni-mannheim.de:3000/direct/item/bbaw/dta/20863/1386762086592
			fromUri = fromUri.replace(apiBase + "/", "");
			String[] s = fromUri.split("/", 3); // ['item', 'bbaw', 'dta/20863/1386762086592']
			providerId = s[1];	 			// 'bbaw'
			s = s[2].split("/", 2);				// ['dta', '20863/1386762086592']
			datasetId = s[0]; 			// 'dta'
			itemId = s[1].substring(0, s[1].lastIndexOf('/')); // '20863'
			versionId = s[1].substring(s[1].lastIndexOf('/') + 1); // '1386762086592'
		} else {
			throw new NotImplementedException();
		}
		VersionedDataset versionedDataset = new VersionedDataset(fileManager, apiBase, null, providerId, datasetId, versionId);
		versionedDataset.read();
		return new ResourceMap(versionedDataset, itemId);
	}
	
	public ResourceMap(VersionedDataset versionedDataset, String resMapId) {
		super(versionedDataset.fileManager);
		this.model = ModelFactory.createDefaultModel();
		this.versionedDataset = versionedDataset;
		this.versionId = versionedDataset.getVersionId();
		this.apiBase = versionedDataset.apiBase;
		this.datasetId = versionedDataset.getCollectionId();
		this.providerId = versionedDataset.getProviderId();
		this.itemId = resMapId;
	}

	public String getResourceMapUri()  { return String.format("%s/resourcemap/%s/%s/%s/%s",	apiBase, providerId, datasetId, itemId, versionId); }
	public String getProvidedCHO_Uri() { return String.format("%s/item/%s/%s/%s", 	    apiBase, providerId, datasetId, itemId); }
	public String getAggregationUri()  { return String.format("%s/aggregation/%s/%s/%s",	apiBase, providerId, datasetId, itemId); }
	public Resource getResourceMapResource() { return this.getModel().createResource(getResourceMapUri()); }
	public Resource getProvidedCHO_Resource() { return this.getModel().createResource(getProvidedCHO_Uri()); }
	public Resource getAggregationResource() { return this.getModel().createResource(getAggregationUri()); }

	@Override
	public String toString() {
		return "ResourceMap " + getResourceMapUri();
	}

	@Override
	public String getRetrievalUri() { return getResourceMapUri(); }
	
	public String getDcTitle() {
		return getLiteralPropValue(getProvidedCHO_Resource(), NS.DC.PROP_TITLE);
	}

	public String getFirstPageLink() {
		Map<String, String> prefixMap = buildPrefixes();
		List<String> pageLinks = new ArrayList<>();
		{
			ParameterizedSparqlString sb = new ParameterizedSparqlString();
			sb.setNsPrefixes(prefixMap);
			sb.setParam("parentCHO", getProvidedCHO_Resource());
			sb.append("SELECT DISTINCT ?cho WHERE {  \n");
			sb.append("   ?cho dcterms:isPartOf* ?parentCHO .  \n");
			sb.append("   FILTER(?cho != ?parentCHO)   \n");
// TODO dirty data
//			sb.append("   ?agg edm:aggregatedCHO ?cho .   \n")
//			sb.append("   ?agg dc:type dm2e:Page   \n")
			sb.append(" } ORDER BY STR(?cho)   \n");
			sb.append("   LIMIT 1  \n ");
			Query query = QueryFactory.create(sb.toString());
			QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
			try {
				ResultSet results = qexec.execSelect() ;
				for ( ; results.hasNext() ; ) {
					QuerySolution soln = results.nextSolution() ;
					pageLinks.add(soln.get("cho").toString());
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
		Map<String, String> prefixMap = buildPrefixes();

		ParameterizedSparqlString sb = new ParameterizedSparqlString();
		sb.setNsPrefixes(prefixMap);
		sb.setParam("agg", getAggregationResource());
		sb.append("SELECT ?thumbnail WHERE {  \n");
		sb.append("   { ?agg edm:object ?thumbnail .}  \n");
		sb.append("   UNION  \n");
		sb.append("   { ?agg dm2e:hasAnnotatableVersionAt ?thumbnail .}  \n");
		sb.append("   UNION  \n");
		sb.append("   { ?agg dm2e11:hasAnnotatableVersionAt ?thumbnail .}  \n");
		sb.append("   UNION  \n");
		sb.append("   { ?agg edm:isShownBy ?thumbnail .}  \n");

// <TODO> dirty data, better leave this even though it's wrong
		sb.append("   ?thumbnail dc:format ?mime_type .  \n");
		sb.append("   FILTER STRSTARTS(STR(?mime_type), \"image\")  \n");
// </TODO>
		sb.append(" } LIMIT 1 ");
		Query query = QueryFactory.create(sb.toString());
		
		String thumbnail = null;
		QueryExecution qexec = QueryExecutionFactory.create(query, getModel()) ;
		try {
			ResultSet results = qexec.execSelect() ;
			for ( ; results.hasNext() ; ) {
				QuerySolution soln = results.nextSolution() ;
				thumbnail = soln.get("thumbnail").toString();
			}
		} finally { qexec.close(); }
		return thumbnail;
	}

//	public List<String> getLiteralSubjects(String... subjectTypes) {
//		
//		List<String> ret = new ArrayList<>();
//
//		Map<String, String> prefixMap = buildPrefixes();
//
//		ParameterizedSparqlString sb = new ParameterizedSparqlString();
//		sb.setNsPrefixes(prefixMap);
//		sb.setParam("cho", getProvidedCHO_Resource());
//		sb.append("SELECT ?subject WHERE {  \n");
//		sb.append("   ?cho dc:subject ?subject .  \n");
//		sb.append(" }  ");
//		Query query = QueryFactory.create(sb.toString());
//		
//		QueryExecution qexec = QueryExecutionFactory.create(query, getModel()) ;
//		try {
//			ResultSet results = qexec.execSelect() ;
//			for ( ; results.hasNext() ; ) {
//				QuerySolution soln = results.nextSolution() ;
//				String subjectUri;
//				String literalSubject = null;
//				try {
//					final Resource subjectResource = soln.get("subject").asResource();
//					subjectUri = subjectResource.getURI();
//
//					// dereference if necessary and get skos:prefLabel
//					ThingWithPrefLabel subjectThing = new ThingWithPrefLabel(fileManager, apiBase, model, subjectUri);
//					if (!model.contains(subjectResource, model.createProperty(NS.RDF.PROP_TYPE), (Resource)null)) {
//						subjectThing.read();
//					}
//					for (String subjectType : subjectTypes) {
//						if (subjectType.equals(subjectThing.getRdfType())) {
//							literalSubject = subjectThing.getPrefLabel();
//							if (null == literalSubject) {
//								literalSubject = subjectUri.substring(subjectUri.lastIndexOf('/') + 1);
//							}
//						}
//					}
//				} catch (JenaException e) {
//					log.error(this + " contains a literal dc:subject!");
//					literalSubject = soln.get("subject").asLiteral().getLexicalForm();
//				}
//				if (null != literalSubject)
//					ret.add(literalSubject);
//			}
//		} finally { qexec.close(); }
//		return ret;
//	}

	public String getLanguage() {
		StmtIterator languageIter = getProvidedCHO_Resource().listProperties(model.createProperty(NS.DC.PROP_LANGUAGE));
		if (! languageIter.hasNext()) {
			return "unknown";
		}
		return languageIter.next().getObject().asLiteral().getLexicalForm();
	}

	public List<String> getDescriptions() {
		List<String> ret = new ArrayList<>();

		Map<String, String> prefixMap = buildPrefixes();

		ParameterizedSparqlString sb = new ParameterizedSparqlString();
		sb.setNsPrefixes(prefixMap);
		sb.setParam("cho", getProvidedCHO_Resource());
		sb.append("SELECT ?description WHERE {  \n");
		sb.append("   { ?cho dcterms:tableOfContents ?description . }  \n");
		sb.append("   UNION  \n");
		sb.append("   { ?cho dc:description ?description . }  \n");
		sb.append(" }  ");
		Query query = QueryFactory.create(sb.toString());
		
		QueryExecution qexec = QueryExecutionFactory.create(query, getModel()) ;
		try {
			ResultSet results = qexec.execSelect() ;
			for ( ; results.hasNext() ; ) {
				QuerySolution soln = results.nextSolution() ;
				if (null == soln.get("description")) {
					continue;
				}
				try {
					String description = soln.get("description").asLiteral().getLexicalForm();
					ret.add(description);
				} catch (JenaException e) {
					log.error(this + " contains a non-literal description!");
					continue;
				}
			}
		} finally { qexec.close(); }
		return ret;
	}
	
	public boolean isDisplayLevelTrue() {
		boolean ret = false;

		Map<String, String> prefixMap = buildPrefixes();

		ParameterizedSparqlString sb = new ParameterizedSparqlString();
		sb.setNsPrefixes(prefixMap);
		sb.setParam("agg", getProvidedCHO_Resource());
		sb.append("ASK {  \n");
		sb.append("   ?agg dm2e:displayLevel \"true\"^^xsd:boolean .  \n");
		sb.append(" }  ");
		Query query = QueryFactory.create(sb.toString());
		
		QueryExecution qexec = QueryExecutionFactory.create(query, getModel()) ;
		try {
			ret = qexec.execAsk();
		} finally { qexec.close(); }
		return ret;
	}

	private Map<String, String> buildPrefixes() {
		Map<String,String> prefixMap = new HashMap<>();
		prefixMap.put("dm2e11", NS.DM2E.BASE);
		prefixMap.put("dm2e", NS.DM2E_UNVERSIONED.BASE);
		prefixMap.put("dc", NS.DC.BASE);
		prefixMap.put("xsd", NS.XSD.BASE);
		prefixMap.put("dcterms", NS.DCTERMS.BASE);
		prefixMap.put("rdf", NS.RDF.BASE);
		prefixMap.put("edm", NS.EDM.BASE);
		prefixMap.put("edm", NS.EDM.BASE);
		return prefixMap;
	}

	public String getDcType() {
		String ret = "Unknown";
		Statement stmt = getProvidedCHO_Resource().getProperty(getModel().createProperty(NS.DC.PROP_TYPE));
		if (stmt.getObject().isResource()) {
			ret = stmt.getObject().asResource().getURI();
		}
		return ret;
	}
	
	public Set<String> getLiteralValues(String dcProperty, String rdfType) {
		if (! dcToDm2eProperties.keySet().contains(dcProperty)) {
			throw new IllegalArgumentException("Unknown dcProperty " + dcProperty);
		}
		Set<Resource> thingResources = new HashSet<Resource>();
		for (String thisPropUrl : dcToDm2eProperties.get(dcProperty)) {
			Property thisProp = model.createProperty(thisPropUrl);
			StmtIterator iter = getProvidedCHO_Resource().listProperties(thisProp);
			while (iter.hasNext()) {
				final RDFNode object = iter.next().getObject();
				if (object.isURIResource()) {
					thingResources.add(object.asResource());
				}
			}
		}
		HashSet<String> ret = new HashSet<String>();
		for (Resource thisThing : thingResources) {
			ThingWithPrefLabel placeThing = new ThingWithPrefLabel(fileManager, apiBase, model, thisThing.getURI());
//			if (! model.contains(thisThing, model.createProperty(NS.RDF.PROP_TYPE))) {
//				placeThing.read();
//			}
			final String prefLabel = placeThing.getPrefLabel();
			if (prefLabel != null) {
				if (null == rdfType || placeThing.getRdfType().equals(rdfType)) {
					ret.add(prefLabel);
				}
			}
		}
		return ret;
	}
}
