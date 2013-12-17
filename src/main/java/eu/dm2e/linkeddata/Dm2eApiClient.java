package eu.dm2e.linkeddata;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import eu.dm2e.linkeddata.model.Dataset;
import eu.dm2e.linkeddata.model.ResourceMap;
import eu.dm2e.ws.NS;

public class Dm2eApiClient {

	private String	apiBase;
	private HashMap<String, Namespace>	jdomNS	= new HashMap<String, Namespace>();
	public DateTimeFormatter iso8601formatter = ISODateTimeFormat.dateTime();

	public Dm2eApiClient(String apiBase) {
		this.apiBase = apiBase;
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
	}

	public String dumpModel(Model model) {
		StringWriter strwriter = new StringWriter();
		model.write(strwriter, "N3");
		return strwriter.toString();
	}
	
	public Document resourceMapToOaiRecord(ResourceMap resMap, String metadataPrefix) {
		if (metadataPrefix.equals("oai_dc")) {
			return resourceMapToOaiRecord_oai_dc(resMap);
		}
		throw new RuntimeException("Unhandled metadataPrefix '" + metadataPrefix + "'.");
	}

	private Document resourceMapToOaiRecord_oai_dc(ResourceMap resMap) {
		Logger log = LoggerFactory.getLogger(getClass().getName());

		// open the file
		String baseName = resMap.getAggregation().getURI();
		baseName = baseName.replaceAll("[^a-zA-Z0-9]+", "");
		
        // build the record
		Document doc = new Document();
		
		// root element and namespaces
		Element oaiPmh = new Element("record", jdomNS.get("oai"));
		for (String prefix : jdomNS.keySet()) {
			if (prefix.equals("") || prefix.startsWith("http")) {
				continue;
			}
			log.debug(prefix);
			oaiPmh.addNamespaceDeclaration(jdomNS.get(prefix));
		}

		// header, about, metadata
		Element oaiHeader = new Element("header", jdomNS.get("oai"));
		Element oaiMetadata = new Element("metadata", jdomNS.get("oai"));
		Element oaiDcDc = new Element("dc", jdomNS.get("oai_dc"));
		oaiMetadata.addContent(oaiDcDc);
		Element oaiAbout = new Element("about", jdomNS.get("oai"));
		oaiPmh.addContent(oaiHeader);
		oaiPmh.addContent(oaiMetadata);
		oaiPmh.addContent(oaiAbout);
		doc.addContent(oaiPmh);
		
//		log.debug(dumpModel(resMap.getModel()));
		
//		// about
		{
			Resource licenseURI = resMap.getAggregation().getPropertyResourceValue(resMap.getModel().createProperty(NS.EDM.PROP_RIGHTS));
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
		// header
		{
			String id = 
					resMap.getDatasetId().replace("/", "__")
					+ "___" +
					resMap.getResourceMapId().replace("/", "__");
			Element identifier = new Element("identifier", jdomNS.get("oai"));
			identifier.addContent(id);
			oaiHeader.addContent(identifier);
			Element dateStamp = new Element("datestamp", jdomNS.get("oai"));
			Statement aggDateStmt = resMap.getAggregation().getProperty(resMap.getModel().createProperty(NS.DCTERMS.PROP_CREATED));
			if (null != aggDateStmt) {
				dateStamp.addContent(iso8601formatter.print(DateTime.parse(aggDateStmt
					.getObject()
					.asLiteral()
					.getValue()
					.toString())));
			} else {
				dateStamp.addContent(iso8601formatter.print(DateTime.now()));

			}
			oaiHeader.addContent(dateStamp);
		}
		
		// metadata : RDF -> XML
		
		// Handle ProvidedCHO
		StmtIterator choIter = resMap.getModel().listStatements(resMap.getProvidedCHO(), null, (RDFNode)null);
		while (choIter.hasNext()) {
			Statement stmt = choIter.next();
			Property pred = stmt.getPredicate();
			RDFNode obj = stmt.getObject();
			Namespace thisElemNs;
			Element thisElem = null;
			switch (pred.getURI().toString()) {
			case NS.RDF.PROP_TYPE:
				String objUri = obj.asResource().getURI();
				if (objUri.equals(NS.EDM.CLASS_PROVIDED_CHO)) {
					continue;
				}
				thisElem = new Element("type", jdomNS.get("rdf"));
				thisElem.setAttribute(new Attribute("resource", obj.asResource().getURI(), jdomNS.get("rdf")));
				break;
			case NS.DC.PROP_TYPE:
				if (obj.isResource()) {
					thisElem = new Element("type", jdomNS.get("rdf"));
					thisElem.setAttribute(new Attribute("resource", obj.asResource().getURI(), jdomNS.get("rdf")));
				}
				break;
			// Fall-thru cases:
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
		}

		StmtIterator aggIter = resMap.getModel().listStatements(resMap.getAggregation(), null, (RDFNode)null);
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
	 * Return the latest version of a Dataset
	 * 
	 * @param datasetId
	 * @return Latest Version of a Dataset
	 */
	public Dataset getDataset(String datasetId) {
		String latestVersionId = findLatestVersion(datasetId);
		return getDataset(datasetId, latestVersionId);
	}

	/**
	 * Return a dataset, defined by the dataset id and a version id
	 * 
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
	 * 
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
	 * 
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
		StmtIterator iter = model.listStatements(model.createResource(uri), model
			.createProperty(NS.DM2E_UNOFFICIAL.PROP_HAS_VERSION), (Resource) null);
		while (iter.hasNext()) {
			Resource res = iter.next().getObject().asResource();
			String id = res.toString().replace(uri + "/", "");
			set.add(id);
		}
		return set;
	}

	/**
	 * List the collections on a server
	 * 
	 * @return Set of collection IDs
	 */
	public Set<String> listDatasets() {
		Logger log = LoggerFactory.getLogger(getClass().getName());
		String uri = apiBase + "/list";
		HashSet<String> set = new HashSet<>();
		Model model = ModelFactory.createDefaultModel();
		model.read(uri);
		log.trace("list collections response: " + dumpModel(model));
		StmtIterator collectionIter = model.listStatements(model.createResource(uri), model
			.createProperty(NS.DM2E_UNOFFICIAL.PROP_HAS_COLLECTION), (Resource) null);
		while (collectionIter.hasNext()) {
			Resource collectionRes = collectionIter.next().getObject().asResource();
			String datasetId = collectionRes.toString().replace(apiBase + "/dataset/", "");
			set.add(datasetId);
		}
		return set;
	}

	public Set<String> listResourceMaps(Dataset ds) {
		Logger log = LoggerFactory.getLogger(getClass().getName());
		StmtIterator resMapIter = ds.getModel().listStatements(ds.getResource(),
				ds.getModel().createProperty(NS.DM2E_UNOFFICIAL.PROP_CONTAINS_CHO),
				(Resource) null);
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
		return getResourceMap(ds.getDatasetId(), resourceMapId);
	}

	public ResourceMap getResourceMap(String datasetId, String resourceMapId) {
		Model model = ModelFactory.createDefaultModel();
		Resource choRes = model.createResource(apiBase + "/item/" + datasetId + "/" + resourceMapId);
		model.read(choRes.getURI());
		Resource aggRes = model.createResource(choRes.getURI().replace("/item/", "/aggregation/"));
		return new ResourceMap(model, choRes, aggRes, datasetId, resourceMapId);
	}

}