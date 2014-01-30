package eu.dm2e.linkeddata;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.riot.RiotNotFoundException;
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

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

import eu.dm2e.jena.MapdbFileManager;
import eu.dm2e.linkeddata.model.BaseModel;
import eu.dm2e.linkeddata.model.BaseModel.IdentifierType;
import eu.dm2e.linkeddata.model.Collection;
import eu.dm2e.linkeddata.model.ResourceMap;
import eu.dm2e.linkeddata.model.ThingWithPrefLabel;
import eu.dm2e.linkeddata.model.VersionedDataset;
import eu.dm2e.ws.NS;

public class Dm2eApiClient {

	Logger log = LoggerFactory.getLogger(getClass().getName());

//	private Map<String,ResourceMap> resourceMapCache = new HashMap<>();
//	private Map<String,VersionedDataset> datasetCache = new HashMap<>();
	private String	apiBase;
	private HashMap<String, Namespace>	jdomNS	= new HashMap<String, Namespace>();
	public DateTimeFormatter oaiDateFormatter = DateTimeFormat.forPattern("YYYY-MM-dd'T'HH:mm:ss'Z'");

	private FileManager fileManager;

	public Dm2eApiClient(String apiBase) {
		this(apiBase, false);
	}
	public Dm2eApiClient(String apiBase, boolean useCaching) {
		this.apiBase = apiBase;
		this.fileManager = Dm2eApiClient.setupFileManager();
		setNamespaces();
	}
	
	public static FileManager setupFileManager() {
		FileManager fm = new MapdbFileManager();
		fm.setModelCaching(true);
		return fm;
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
			DateTime parsedDate;
			try {
				parsedDate = DateTime.parse(aggDateStmt
						.getObject()
						.asLiteral()
						.getValue()
						.toString());
				dateStamp.addContent(oaiDateFormatter.print(parsedDate));
			} catch (IllegalArgumentException e) {
				log.error("ResourceMap " + resMap + " has an illegal Date: ", e);
			}
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
		
		// First page Link (Thumbnail)
		{
			Element el = new Element("identifier", jdomNS.get("dc"));
			el.setAttribute("linktype", "thumbnail");
			el.setText(String.format(Config.PUNDIT_FMT_STRING, resMap.getThumbnailLink()));
			oaiDcDc.addContent(el);
		}

		// PunditLink
		// http://demo.feed.thepund.it/?dm2e=http://lelystad.informatik.uni-mannheim.de:3000/direct/item/bbaw/dta/16821/f0001&conf=timeline-demo.js
		{
			String firstPageLink = resMap.getFirstPageLink();
			if (null != firstPageLink) {
				Element el = new Element("identifier", jdomNS.get("dc"));
				el.setAttribute("linktype", "annotate");
				el.setText(String.format(Config.PUNDIT_FMT_STRING, resMap.getFirstPageLink()));
				oaiDcDc.addContent(el);
			}
		}
		
		// Provider ID
		{
			Element el = new Element("dataProvider", jdomNS.get("edm"));
			el.setText(resMap.getProviderId());
			oaiDcDc.addContent(el);
		}
		
		// #create-title
		{
			QuerySolution titles = resMap.getAllTitles();
			if (null == titles) {
				log.error("Couldn't determine title for " + resMap + ". Bailing out.");
				return null;
			}
			final RDFNode dcterms_title_node = titles.get("dcterms_title");
			final RDFNode dc_title_node = titles.get("dc_title");

			StringBuilder sb = new StringBuilder();
			if (null != dcterms_title_node) {
				sb.append(dcterms_title_node.toString());
			} else if (null != dc_title_node) {
				sb.append(dc_title_node.toString());
			} else {
				log.error("Couldn't determine dcterms:title or dc:title for " + resMap + ". Bailing out.");
				return null;
			}
			if (null != titles.get("dm2e_subtitle")) {
				sb.append(" -- ");
				sb.append(titles.get("dm2e_subtitle"));
			}
			Element el = new Element("title", jdomNS.get("dc"));
			el.setText(sb.toString());
			oaiDcDc.addContent(el);
		}


//		log.debug(dumpModel(resMap.getModel()));
		StmtIterator choIter = resMap.getModel().listStatements(resMap.getProvidedCHO_Resource(), null, (RDFNode)null);
		log.debug("CHO Statements???? " + choIter.hasNext());
		while (choIter.hasNext()) {
			Statement stmt = choIter.next();
			Property pred = stmt.getPredicate();
			RDFNode obj = stmt.getObject();

			Namespace thisElemNs;
			Element el = null;
			final String predUrl = pred.getURI().toString();
			boolean addGenericElement = false;
			if (NS.RDF.PROP_TYPE.equals(predUrl)) {
				String objUri = obj.asResource().getURI();
				if (NS.EDM.CLASS_PROVIDED_CHO.equals(objUri)) continue;
				el = new Element("type", jdomNS.get("rdf"));
				el.setAttribute(new Attribute("resource", obj.asResource().getURI(), jdomNS.get("rdf")));
				Element el2 = new Element("type", jdomNS.get("dc"));
				el2.addContent(obj.asResource().getURI());
				oaiDcDc.addContent(el2);
			} else if (NS.DC.PROP_TYPE.equals(predUrl)) {
				if (obj.isResource()) {
					el = new Element("type", jdomNS.get("rdf"));
					el.setAttribute(new Attribute("resource", obj.asResource().getURI(), jdomNS.get("rdf")));
				}
				addGenericElement = true;
			} else if (NS.DCTERMS.PROP_TITLE.equals(predUrl) || NS.DM2E.PROP_SUBTITLE.equals(predUrl)) {
				// NOTE don't take the title but create a title from all the subtitles above #create-title
			} else if (NS.DC.PROP_PUBLISHER.equals(predUrl)) {
				el = new Element("publisher", jdomNS.get("dc"));
				addContentOrPrefLabelToElement(obj, el);
			} else if (NS.DM2E.PROP_PRINTED_AT.equals(predUrl)) {
				el = new Element("coverage", jdomNS.get("dc"));
				addContentOrPrefLabelToElement(obj, el);
			} else if (NS.PRO.PROP_AUTHOR.equals(predUrl)) {
				el = new Element("creator", jdomNS.get("dc"));
				addContentOrPrefLabelToElement(obj, el);
			} else if (NS.BIBO.PROP_EDITOR.equals(predUrl)) {
				el = new Element("creator", jdomNS.get("dc"));
				addContentOrPrefLabelToElement(obj, el);
			} else if (NS.DC.PROP_SUBJECT.equals(predUrl)) {
				el = new Element("subject", jdomNS.get("dc"));
				addContentOrPrefLabelToElement(obj, el);
			} else if (NS.DCTERMS.PROP_ISSUED.equals(predUrl)) {
				el = new Element("date", jdomNS.get("dc"));
				if (obj.isLiteral())
					el.addContent(obj.asLiteral().toString());
				else {
					// TODO handle timespans
				}
				addGenericElement = true;
			} else {
				addGenericElement = true;
			}
			if (null != el && el.getContentSize() > 0 && el.getContent(0).getValue().length() > 0)  {
				oaiDcDc.addContent(el);
			}

			if (addGenericElement) {
//				 generic mapping
//				log.warn("Unhandled Predicate: " + pred);
//				continue;
				thisElemNs = jdomNS.get(pred.getNameSpace());
				if (null == thisElemNs) {
					log.warn("Unknown namespace: " + pred.getNameSpace());
				}
				Element genericElem = new Element(pred.getLocalName(), thisElemNs);
				if (obj.isLiteral()) {
					genericElem.setText(obj.asLiteral().getValue().toString());
				} else {
					genericElem.setText(obj.asResource().getURI());
				}
				if (null != genericElem) oaiDcDc.addContent(genericElem);
			}
		}

		StmtIterator aggIter = resMap.getModel().listStatements(resMap.getAggregationResource(), null, (RDFNode)null);
		while (aggIter.hasNext()) {
			Statement stmt = aggIter.next();
			Property pred = stmt.getPredicate();
			RDFNode obj = stmt.getObject();
			// manual mapping
			Element el = null;
			final String predUrl = pred.getURI().toString();
			if (NS.EDM.PROP_IS_SHOWN_BY.equals(predUrl)) {
				el = new Element("identifier", jdomNS.get("dc"));
				el.setText(obj.asResource().getURI());
				el.setAttribute("linktype", "fulltext");
//			} else if (NS.EDM.PROP_IS_SHOWN_AT.equals(predUrl)) {
//				el = new Element("identifier", jdomNS.get("dc"));
//				el.setText(obj.asResource().getURI());
//				el.setAttribute("linktype", "thumbnail");
//			} else if (NS.EDM.PROP_OBJECT.equals(predUrl)) {
//				el = new Element("identifier", jdomNS.get("dc"));
//				el.setText(obj.asResource().getURI());
//				el.setAttribute("linktype", "thumbnail");
//			} else if (NS.DM2E.PROP_HAS_ANNOTABLE_VERSION_AT.equals(predUrl)) {
//				el = new Element("identifier", jdomNS.get("dc"));
//				el.setText(obj.asResource().getURI());
//				el.setAttribute("linktype", "thumbnail");
			}
			if (null != el) oaiDcDc.addContent(el);
		}

		return doc;
	}
	private void addContentOrPrefLabelToElement(RDFNode obj, Element el) {
		if (obj.isLiteral()) {
			el.addContent(obj.asLiteral().getValue().toString());
		} else {
			// derefrence
			ThingWithPrefLabel thingWithPrefLabel = new ThingWithPrefLabel(fileManager, apiBase, null, obj.asResource().toString());
			try {
				thingWithPrefLabel.read();
			} catch (RiotNotFoundException e) {
				log.error("Undereferencable thing '{}', skipping", thingWithPrefLabel.getRetrievalUri());
			}
			el.addContent(thingWithPrefLabel.getPrefLabel());
		}
	}
	/**
	 * List the collections on a server
	 * 
	 * @return Set of collections
	 */
	public Set<Collection> listCollections() {
		String uri = apiBase + "/list";
		HashSet<Collection> set = new HashSet<Collection>();
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
			Collection coll = createCollection(new Collection(fileManager, apiBase, null, idStrSegments[0], idStrSegments[1]));
			set.add(coll);
		}
		return set;
	}
	

	/**
	 * Create a VersionedDataset and read it from apiBase
	 */
	// http://lelystad.informatik.uni-mannheim.de:3000/direct/dataset/bbaw/dta/1386762086592
	public BaseModel createVersionedDataset(String providerId, String datasetId, String versionId) {
		final VersionedDataset vds = new VersionedDataset(fileManager, apiBase, null, providerId, datasetId, versionId);
		return createVersionedDataset(vds);
	}
	public VersionedDataset createVersionedDataset(final VersionedDataset dataset) {
		dataset.read();
		return dataset;
	}
	public ResourceMap createResourceMap(String providerId, String datasetId, String itemId, String versionId) throws IllegalArgumentException, HttpException {
		final ResourceMap resourceMap = new ResourceMap(fileManager, apiBase, null, providerId, datasetId, itemId, versionId);
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
		resourceMap.read();
		if (! resourceMap.isRead) {
			throw new HttpException(404);
		}
		log.debug("Model size: " + resourceMap.getModel().size());
		return resourceMap;
	}
	public ResourceMap createResourceMap(String fromUri, IdentifierType type) throws Exception { 
		Collection coll = createCollection(fromUri, type);
		String versionId = coll.getLatestVersionId();
		return createResourceMap(new ResourceMap(fileManager, apiBase, fromUri, type, versionId));
	}
	public Collection createCollection(final Collection collection) {
		collection.read();
		return collection;
	}
	public Collection createCollection(String fromUri, IdentifierType type) throws Exception {
		Collection newCollection;
		try {
			newCollection = new Collection(fileManager, apiBase, fromUri, type);
		} catch (Exception e) {
			throw e;
		}
		return createCollection(newCollection);
	}
}