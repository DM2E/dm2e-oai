package eu.dm2e.linkeddata;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.sf.ehcache.CacheManager;

import org.eclipse.jetty.http.HttpException;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import eu.dm2e.linkeddata.model.BaseModel;
import eu.dm2e.linkeddata.model.BaseModel.IdentifierType;
import eu.dm2e.linkeddata.model.Collection;
import eu.dm2e.linkeddata.model.ResourceMap;
import eu.dm2e.linkeddata.model.VersionedDataset;
import eu.dm2e.ws.NS;

public class Dm2eApiClient {

	private static final String	CACHE_NAME	= "dm2e-oai";

	Logger log = LoggerFactory.getLogger(getClass().getName());


//	private Map<String,ResourceMap> resourceMapCache = new HashMap<>();
//	private Map<String,VersionedDataset> datasetCache = new HashMap<>();
	private String	apiBase;
	private HashMap<String, Namespace>	jdomNS	= new HashMap<String, Namespace>();
	public DateTimeFormatter oaiDateFormatter = DateTimeFormat.forPattern("YYYY-MM-dd'T'HH:mm:ss'Z'");
	private boolean useCaching = false;
	private static CacheManager cacheMgr = CacheManager.create();

	public Dm2eApiClient(String apiBase) {
		this.apiBase = apiBase;
		setNamespaces();
	}
	public Dm2eApiClient(String apiBase, boolean useCaching) {
		this.apiBase = apiBase;
		this.useCaching = useCaching;
		setNamespaces();
	}

	public String nowOaiFormatted() {
		return oaiDateFormatter.print(DateTime.now());
	}
	public void setNamespaces() {
		jdomNS.put("", Namespace.getNamespace("", NS.OAI.BASE));
		jdomNS.put(NS.OAI.BASE, Namespace.getNamespace("", NS.OAI.BASE));
		jdomNS.put("oai", Namespace.getNamespace("oai", NS.OAI.BASE));
		jdomNS.put("oai_dc", Namespace.getNamespace("oai_dc", NS.OAI_DC.BASE));
		jdomNS.put(NS.OAI_DC.BASE, Namespace.getNamespace("oai_dc", NS.OAI_DC.BASE));
		jdomNS.put("rights", Namespace.getNamespace("rights", NS.OAI_RIGHTS.BASE));
		jdomNS.put(NS.OAI_RIGHTS.BASE, Namespace.getNamespace("rights", NS.OAI_RIGHTS.BASE));
		jdomNS.put("dcterms", Namespace.getNamespace("dcterms", NS.DCTERMS.BASE));
		jdomNS.put(NS.DCTERMS.BASE, Namespace.getNamespace("dcterms", NS.DCTERMS.BASE));
		jdomNS.put("dc", Namespace.getNamespace("dc", NS.DC.BASE));
		jdomNS.put(NS.DC.BASE, Namespace.getNamespace("dc", NS.DC.BASE));
		jdomNS.put("edm", Namespace.getNamespace("edm", NS.EDM.BASE));
		jdomNS.put(NS.EDM.BASE, Namespace.getNamespace("edm", NS.EDM.BASE));
		jdomNS.put("rdf", Namespace.getNamespace("rdf", NS.RDF.BASE));
		jdomNS.put(NS.RDF.BASE, Namespace.getNamespace("rdf", NS.RDF.BASE));
		jdomNS.put("dm2e", Namespace.getNamespace("dm2e", NS.DM2E.BASE));
		jdomNS.put(NS.DM2E.BASE, Namespace.getNamespace("dm2e", NS.DM2E.BASE));
		jdomNS.put("pro", Namespace.getNamespace("pro", NS.PRO.BASE));
		jdomNS.put(NS.PRO.BASE, Namespace.getNamespace("pro", NS.PRO.BASE));
		jdomNS.put("bibo", Namespace.getNamespace("bibo", NS.BIBO.BASE));
		jdomNS.put(NS.BIBO.BASE, Namespace.getNamespace("bibo", NS.BIBO.BASE));
		jdomNS.put("owl", Namespace.getNamespace("owl", NS.OWL.BASE));
		jdomNS.put(NS.OWL.BASE, Namespace.getNamespace("owl", NS.OWL.BASE));
	}

	public String dumpModel(Model model) {
		StringWriter strwriter = new StringWriter();
		model.write(strwriter, "N3");
		return strwriter.toString();
	}
	
//	public String oaifyId(String dm2eId) {
//		return dm2eId.replaceAll("/", "__");
//	}
//	public String oaifyId(String datasetId, String itemId) {
//		return oaifyId(datasetId) + "___" + oaifyId(itemId);
//	}
//	public String[] unoaifyId(String... oaiIds) throws IllegalArgumentException {
//		String[] idSegments = oaiId.split("___");
//		if (idSegments.length != 2) { throw new IllegalArgumentException("oaiId must contain '___'"); }
//		idSegments[0] = idSegments[0].replace("__", "/");
//		idSegments[1] = idSegments[1].replace("__", "/");
//		return idSegments;
//	}

	public Element resourceMapToOaiHeader(ResourceMap resMap) {
		Element oaiHeader = new Element("header", jdomNS.get("oai"));
		oaiHeader.setNamespace(Namespace.NO_NAMESPACE);
		String id = String.format("oai:dm2e:%s:%s:%s",
				resMap.getProviderId(),
				resMap.getCollectionId(),
				resMap.getItemId()
				);
		Element identifier = new Element("identifier", jdomNS.get("oai"));
		identifier.addContent(id);
		identifier.setNamespace(Namespace.NO_NAMESPACE);
		oaiHeader.addContent(identifier);
		Element dateStamp = new Element("datestamp", jdomNS.get("oai"));
		dateStamp.setNamespace(Namespace.NO_NAMESPACE);
		Statement aggDateStmt = resMap.getAggregationResource().getProperty(resMap.getModel().createProperty(NS.DCTERMS.PROP_CREATED));
		if (null != aggDateStmt) {
			dateStamp.addContent(oaiDateFormatter.print(DateTime.parse(aggDateStmt
					.getObject()
					.asLiteral()
					.getValue()
					.toString())));
		} else {
			dateStamp.addContent(oaiDateFormatter.print(DateTime.now()));

		}
		oaiHeader.addContent(dateStamp);
		// TODO
		Element setSpecProvider = new Element("setSpec", jdomNS.get("oai"));
		setSpecProvider.setNamespace(Namespace.NO_NAMESPACE);
		setSpecProvider.addContent(String.format("provider:%s", resMap.getProviderId()));
		oaiHeader.addContent(setSpecProvider);
		Element setSpecCollection = new Element("setSpec", jdomNS.get("oai"));
		setSpecCollection.setNamespace(Namespace.NO_NAMESPACE);
		setSpecCollection.addContent(String.format("collection:%s:%s", resMap.getProviderId(), resMap.getCollectionId()));
		oaiHeader.addContent(setSpecCollection);
		return oaiHeader;
	}
	
	/**
	 * Convert a ResourceMap to a oai:record
	 * @param resMap	ResourceMap to convert
	 * @param metadataPrefix	Which type of metadata (currently: only 'oai_dc')
	 * @return
	 */
	public Document resourceMapToOaiRecord(ResourceMap resMap, String metadataPrefix) {
		if (metadataPrefix.equals("oai_dc")) {
			return resourceMapToOaiRecord_oai_dc(resMap);
		}
		throw new RuntimeException("Unhandled metadataPrefix '" + metadataPrefix + "'.");
	}

	private Document resourceMapToOaiRecord_oai_dc(ResourceMap resMap) {
		Logger log = LoggerFactory.getLogger(getClass().getName());

		// open the file
//		String baseName = resMap.getAggregationUri();
//		baseName = baseName.replaceAll("[^a-zA-Z0-9]+", "");
		
        // build the record
		Document doc = new Document();
		
		// root element and namespaces
		Element oaiRecord = new Element("record", jdomNS.get(""));
		oaiRecord.setNamespace(Namespace.NO_NAMESPACE);
		for (String prefix : jdomNS.keySet()) {
			if (prefix.equals("") || prefix.startsWith("http")) {
				continue;
			}
			log.debug(prefix);
		}

		// header, about, metadata
		Element oaiHeader = resourceMapToOaiHeader(resMap);
		Element oaiMetadata = new Element("metadata", jdomNS.get(""));
		oaiMetadata.setNamespace(Namespace.NO_NAMESPACE);
		Element oaiAbout = new Element("about", jdomNS.get(""));
		oaiAbout.setNamespace(Namespace.NO_NAMESPACE);
		oaiRecord.addContent(oaiHeader);
		oaiRecord.addContent(oaiMetadata);
		oaiRecord.addContent(oaiAbout);

		doc.addContent(oaiRecord);
		
//		log.debug(dumpModel(resMap.getModel()));
		
//		// about
		{
			Resource licenseURI = resMap.getAggregationResource().getPropertyResourceValue(resMap.getModel().createProperty(NS.EDM.PROP_RIGHTS));
			Element rights = new Element("rights", jdomNS.get("rights"));
			if (null != licenseURI) { 
				Element rightsReference = new Element("rightsReference", jdomNS.get("rights"));
				Attribute rightsReferenceRef = new Attribute("ref", licenseURI.getURI());
				rights.addContent(rightsReference);
				rightsReference.setAttribute(rightsReferenceRef);
//				rightsReference.addContent(licenseURI.getURI());
				oaiAbout.addContent(rights);
			}
		}
		
		// metadata : RDF -> XML
		
		// Handle ProvidedCHO
		Element oaiDcDc = new Element("dc", jdomNS.get("oai_dc"));
		oaiMetadata.addContent(oaiDcDc);
//		log.debug(dumpModel(resMap.getModel()));
		StmtIterator choIter = resMap.getModel().listStatements(resMap.getProvidedCHO_Resource(), null, (RDFNode)null);
		log.debug("CHO Statements???? " + choIter.hasNext());
		while (choIter.hasNext()) {
			Statement stmt = choIter.next();
			Property pred = stmt.getPredicate();
			RDFNode obj = stmt.getObject();
			Namespace thisElemNs;
			Element thisElem = null;
			Element thisElem2 = null;
			switch (pred.getURI().toString()) {
			case NS.RDF.PROP_TYPE:
				String objUri = obj.asResource().getURI();
				if (objUri.equals(NS.EDM.CLASS_PROVIDED_CHO)) {
					continue;
				}
				thisElem = new Element("type", jdomNS.get("rdf"));
				thisElem.setAttribute(new Attribute("resource", obj.asResource().getURI(), jdomNS.get("rdf")));
				thisElem2 = new Element("type", jdomNS.get("dc"));
				thisElem2.addContent(obj.asResource().getURI());
				break;
			// Fall-thru cases:
			case NS.DC.PROP_TYPE:
				if (obj.isResource()) {
					thisElem = new Element("type", jdomNS.get("rdf"));
					thisElem.setAttribute(new Attribute("resource", obj.asResource().getURI(), jdomNS.get("rdf")));
				}
			case NS.PRO.PROP_AUTHOR:
				Element addElem = new Element("creator", jdomNS.get("dcterms"));
				addElem.addContent(obj.asResource().getURI());
				oaiDcDc.addContent(addElem);
			default:
//				 generic mapping
//				log.warn("Unhandled Predicate: " + pred);
//				continue;
				thisElemNs = jdomNS.get(pred.getNameSpace());
				if (null == thisElemNs) {
					log.warn("Unknown namespace: " + pred.getNameSpace());
				}
				thisElem = new Element(pred.getLocalName(), thisElemNs);
				if (obj.isLiteral()) {
					thisElem.setText(obj.asLiteral().getValue().toString());
				} else {
					thisElem.setText(obj.asResource().getURI());
				}
				break;
			}
			if (null != thisElem) oaiDcDc.addContent(thisElem);
			if (null != thisElem2) oaiDcDc.addContent(thisElem2);
		}

		StmtIterator aggIter = resMap.getModel().listStatements(resMap.getAggregationResource(), null, (RDFNode)null);
		while (aggIter.hasNext()) {
			Statement stmt = aggIter.next();
			Property pred = stmt.getPredicate();
			RDFNode obj = stmt.getObject();
			// manual mapping
			Element thisElem = null;
			switch (pred.getURI().toString()) {
			case NS.EDM.PROP_IS_SHOWN_BY:
				thisElem = new Element("identifier", jdomNS.get("dc"));
				thisElem.setText(obj.asResource().getURI());
				thisElem.setAttribute("linktype", "fulltext");
				break;
			case NS.EDM.PROP_IS_SHOWN_AT:
				thisElem = new Element("identifier", jdomNS.get("dc"));
				thisElem.setText(obj.asResource().getURI());
				thisElem.setAttribute("linktype", "thumbnail");
				break;
			case NS.DM2E.PROP_HAS_ANNOTABLE_VERSION_AT:
				thisElem = new Element("identifier", jdomNS.get("dc"));
				thisElem.setText(obj.asResource().getURI());
				thisElem.setAttribute("linktype", "thumbnail");
				break;
			}
			if (null != thisElem) oaiDcDc.addContent(thisElem);
		}

		return doc;
	}
	/**
	 * List the collections on a server
	 * 
	 * @return Set of collections
	 */
	public Set<Collection> listCollections() {
		String uri = apiBase + "/list";
		HashSet<Collection> set = new HashSet<>();
		Model model = ModelFactory.createDefaultModel();
		long t0 = System.currentTimeMillis();
		// TODO cache
		model.read(uri);
		long t1 = System.currentTimeMillis();
		log.debug(String.format("Reading the list of Datasets took %sms", (t1-t0)));
		log.trace("list collections response: " + dumpModel(model));
		StmtIterator collectionIter = model.listStatements(
				model.createResource(uri),
				model.createProperty(NS.DM2E_UNOFFICIAL.PROP_HAS_COLLECTION),
				(Resource) null);
		while (collectionIter.hasNext()) {
			Resource collectionRes = collectionIter.next().getObject().asResource();
			String idStr = collectionRes.toString().replace(apiBase + "/dataset/", "");
			String[] idStrSegments = idStr.split("/");
			Collection coll = createCollection(new Collection(apiBase, null, idStrSegments[0], idStrSegments[1]));
			set.add(coll);
		}
		return set;
	}
	

	/**
	 * Create a VersionedDataset and read it from apiBase
	 */
	// http://lelystad.informatik.uni-mannheim.de:3000/direct/dataset/bbaw/dta/1386762086592
	public BaseModel createVersionedDataset(String providerId, String datasetId, String versionId) {
		final VersionedDataset vds = new VersionedDataset(apiBase, null, providerId, datasetId, versionId);
		return createVersionedDataset(vds);
	}
	public VersionedDataset createVersionedDataset(final VersionedDataset dataset) {
		if (useCaching) dataset.read(cacheMgr.getCache(CACHE_NAME));
		else dataset.read();
		return dataset;
	}
	public ResourceMap createResourceMap(String providerId, String datasetId, String itemId, String versionId) throws IllegalArgumentException, HttpException {
		final ResourceMap resourceMap = new ResourceMap(apiBase, null, providerId, datasetId, itemId, versionId);
		return createResourceMap(resourceMap);
	}
//	public VersionedDataset createVersionedDataset(String fromUri, IdentifierType type) throws IllegalArgumentException, HttpException { 
//		VersionedDataset newVersionedDataset;
//		try {
//			newVersionedDataset = new VersionedDataset(apiBase, fromUri, type);
//		} catch (Exception e) {
//			throw e;
//		}
//		return createVersionedDataset(newVersionedDataset);
//	}
	public ResourceMap createResourceMap(final ResourceMap resourceMap) throws IllegalArgumentException, HttpException {
		if (useCaching) resourceMap.read(cacheMgr.getCache(CACHE_NAME));
		else resourceMap.read();
		if (! resourceMap.isRead) {
			throw new HttpException(404);
		}
		log.debug("Model size: " + resourceMap.getModel().size());
		return resourceMap;
	}
	public ResourceMap createResourceMap(String fromUri, IdentifierType type) throws IllegalArgumentException, HttpException { 
		Collection coll = createCollection(fromUri, type);
		String versionId = coll.getLatestVersionId();
		return createResourceMap(new ResourceMap(apiBase, fromUri, type, versionId));
	}
	public Collection createCollection(final Collection collection) {
		if (useCaching) collection.read(cacheMgr.getCache(CACHE_NAME));
		else collection.read();
		return collection;
	}
	public Collection createCollection(String fromUri, IdentifierType type) {
		Collection newCollection;
		try {
			newCollection = new Collection(apiBase, fromUri, type);
		} catch (Exception e) {
			throw e;
		}
		return createCollection(newCollection);
	}
}