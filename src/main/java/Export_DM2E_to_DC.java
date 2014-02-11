

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

import eu.dm2e.NS;

public class Export_DM2E_to_DC {
	
	private String endpoint;
	private String outputDirectory;
	private Set<String> exportedTypes = new HashSet<String>();
	private static String sparqlDm2eOaiSelect;
	private static String sparqlDm2eOaiConstruct;
	private static String sparqlListMatchingAggregations;
	private static javax.xml.validation.Validator validator;
	HashMap<String, Namespace> ns = new HashMap<String,Namespace>();
	FastDateFormat dateStampFmt = FastDateFormat.getInstance("yyyy-MM-dd");
	
	static {
		Logger slog = LoggerFactory.getLogger(Export_DM2E_to_DC.class.getName());
		try {
			// SPARQL SELECT Query
			URL sparqlDm2eOaiSelectURL = Export_DM2E_to_DC.class.getResource("/sparql/dm2e-oai-select.rq");
			sparqlDm2eOaiSelect = IOUtils.toString(sparqlDm2eOaiSelectURL.openStream());
			URL sparqlDm2eOaiConstructURL = Export_DM2E_to_DC.class.getResource("/sparql/dm2e-oai-construct.rq");
			sparqlDm2eOaiConstruct = IOUtils.toString(sparqlDm2eOaiConstructURL.openStream());
			URL sparqlListMatchingAggregationsURL = Export_DM2E_to_DC.class.getResource("/sparql/list-matching-aggregations.rq");
			sparqlListMatchingAggregations = IOUtils.toString(sparqlListMatchingAggregationsURL.openStream());
			URL schemaFile = Export_DM2E_to_DC.class.getResource("/xsd/OAI-PMH-patched.xsd");
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = schemaFactory.newSchema(schemaFile);
			validator = schema.newValidator();
		} catch (IOException e) {
			slog.error(e.getMessage());
			// here we should just fail but cannot X(
			e.printStackTrace();
		}
		catch (SAXException e) {
			slog.error(e.getMessage());
			// here we should just fail but cannot X(
			e.printStackTrace();
		}
	}

	/**
	 * Creates oai:record documents for CHOs/Aggregations in our store
	 * to be used in an OAI-PMH endpoint
	 * 
	 * Identifier: URI of the Aggregation
	 * @param endpoint
	 * @param outputDirectory
	 */
	public Export_DM2E_to_DC(String endpoint, String outputDirectory) {
		@SuppressWarnings("unused")
		Logger log = LoggerFactory.getLogger(getClass().getName());
		this.endpoint =  endpoint;
		this.outputDirectory = outputDirectory;
		this.exportedTypes.add(NS.DM2E.CLASS_MANUSCRIPT);
		this.exportedTypes.add(NS.FABIO.CLASS_ARTICLE);
		ns.put("", Namespace.getNamespace("", NS.OAI.BASE));
		ns.put("oai", Namespace.getNamespace("oai", NS.OAI.BASE));
		ns.put(NS.OAI.BASE, Namespace.getNamespace("", NS.OAI.BASE));
		ns.put("oai_dc", Namespace.getNamespace("oai_dc", NS.OAI_DC.BASE));
		ns.put(NS.OAI_DC.BASE, Namespace.getNamespace("oai_dc", NS.OAI_DC.BASE));
		ns.put("rights", Namespace.getNamespace("rights", NS.OAI_RIGHTS.BASE));
		ns.put(NS.OAI_RIGHTS.BASE, Namespace.getNamespace("rights", NS.OAI_RIGHTS.BASE));
		ns.put("dcterms", Namespace.getNamespace("dcterms", NS.DCTERMS.BASE));
		ns.put(NS.DCTERMS.BASE, Namespace.getNamespace("dcterms", NS.DCTERMS.BASE));
		ns.put("dc", Namespace.getNamespace("dc", NS.DC.BASE));
		ns.put(NS.DC.BASE, Namespace.getNamespace("dc", NS.DC.BASE));
		ns.put("edm", Namespace.getNamespace("edm", NS.EDM.BASE));
		ns.put(NS.EDM.BASE, Namespace.getNamespace("edm", NS.EDM.BASE));
		ns.put("rdf", Namespace.getNamespace("rdf", NS.RDF.BASE));
		ns.put(NS.RDF.BASE, Namespace.getNamespace("rdf", NS.RDF.BASE));
		ns.put("dm2e", Namespace.getNamespace("dm2e", NS.DM2E.BASE));
		ns.put(NS.DM2E.BASE, Namespace.getNamespace("dm2e", NS.DM2E.BASE));
		ns.put("pro", Namespace.getNamespace("pro", NS.PRO.BASE));
		ns.put(NS.PRO.BASE, Namespace.getNamespace("pro", NS.PRO.BASE));
	}
	
	public void fromEndpoint() throws JAXBException, IOException {
		Logger log = LoggerFactory.getLogger(getClass().getName());
		
		// get list of matching Chos/Aggregations
		Set<Resource> aggSet = new HashSet<Resource>();
		{
			ParameterizedSparqlString sb = new ParameterizedSparqlString();
			sb.append(sparqlListMatchingAggregations);
			Query query = sb.asQuery();
			log.debug("Query endpoint: " + this.endpoint);
			log.debug("Query to be executed: " + query.toString());
			QueryEngineHTTP qExec = QueryExecutionFactory.createServiceRequest(this.endpoint, query);
			long t0 = System.currentTimeMillis();
			log.debug("Executing query ... [at " + t0 + "]");
			ResultSet rs = qExec.execSelect();
			long t1 = System.currentTimeMillis();
			log.debug("DONE Executing query ... [at " + t1 + "]");
			log.debug("Query took " + (t1-t0) + "ms.");
			log.debug("Query got results: " + rs.hasNext());
			while (rs.hasNext()) {
				QuerySolution sol = rs.next();
				log.debug("Aggregation: " + sol.get("agg"));
				aggSet.add(sol.getResource("agg"));
			}
//			if (true) return;
		}
		
		// construct shit
		for (Resource agg:aggSet) {
			log.debug("Aggregation to CONSTRUCT: " + agg);
			ParameterizedSparqlString sb = new ParameterizedSparqlString();
			sb.append(sparqlDm2eOaiConstruct);
			sb.setParam("agg", agg);
			Query query = sb.asQuery();
			log.trace("Query endpoint: " + this.endpoint);
			log.trace("Query to be executed: " + query.toString());
			QueryEngineHTTP qExec = QueryExecutionFactory.createServiceRequest(this.endpoint, query);
			long t0 = System.currentTimeMillis();
			log.trace("Executing query ... [at " + t0 + "]");
			Model model = qExec.execConstruct();
			long t1 = System.currentTimeMillis();
			log.trace("DONE Executing query ... [at " + t1 + "]");
			log.debug("Query took " + (t1-t0) + "ms.");
			log.debug("Query got results: " + model.size());
			
			// So agg belongs to model now
			agg = model.getResource(agg.getURI());
			try {
				writeOaiRecord(model, agg);
			} catch (SAXException e) {
				log.error("Could not transform " + agg + " to OAI-PMH because of validation errors");
			}
		}

	}

	private void writeOaiRecord(Model model, Resource agg) throws JAXBException, IOException, SAXException {
		Logger log = LoggerFactory.getLogger(getClass().getName());

		// open the file
		String baseName = agg.getURI();
		baseName = baseName.replaceAll("[^a-zA-Z0-9]+", "");
		
		// determine CHO
		NodeIterator choUriIter = model.listObjectsOfProperty(agg, model.createProperty(NS.EDM.PROP_AGGREGATED_CHO));
		if (! choUriIter.hasNext()) {
			throw new RuntimeException("Couldn find CHO for " + agg);
		}
		Resource cho = (Resource) choUriIter.next();
		log.debug("CHO: " + cho);

		
        // build the record
		Document doc = new Document();
		
		// root element and namespaces
		Element oaiPmh = new Element("record", ns.get("oai"));
		for (String prefix : ns.keySet()) {
			if (prefix.equals("") || prefix.startsWith("http")) {
				continue;
			}
			log.debug(prefix);
			oaiPmh.addNamespaceDeclaration(ns.get(prefix));
		}

		// header, about, metadata
		Element oaiHeader = new Element("header", ns.get("oai"));
		Element oaiMetadata = new Element("metadata", ns.get("oai"));
		Element oaiDcDc = new Element("dc", ns.get("oai_dc"));
		oaiMetadata.addContent(oaiDcDc);
		Element oaiAbout = new Element("about", ns.get("oai"));
		oaiPmh.addContent(oaiHeader);
		oaiPmh.addContent(oaiMetadata);
		oaiPmh.addContent(oaiAbout);
		doc.addContent(oaiPmh);
		
		StringWriter strwriter = new StringWriter();
		model.write(strwriter, "N3");
		final String modelSerialized = strwriter.toString();
		log.debug(modelSerialized);
		
//		// about
		{
			Resource licenseURI = agg.getPropertyResourceValue(model.createProperty(NS.EDM.PROP_RIGHTS));
			Element rights = new Element("rights", ns.get("rights"));
			if (null != licenseURI) { 
				Element rightsReference = new Element("rightsReference", ns.get("rights"));
				Attribute rightsReferenceRef = new Attribute("ref", licenseURI.getURI());
				rights.addContent(rightsReference);
				rightsReference.setAttribute(rightsReferenceRef);
//				rightsReference.addContent(licenseURI.getURI());
				oaiAbout.addContent(rights);
			}
		}
		// header
		{
			Element identifier = new Element("identifier", ns.get("oai"));
			identifier.addContent(baseName);
			oaiHeader.addContent(identifier);
			Element dateStamp = new Element("datestamp", ns.get("oai"));
			dateStamp.addContent(dateStampFmt.format(new Date()));
			oaiHeader.addContent(dateStamp);
		}
		
		// metadata : RDF -> XML
		
		// Handle ProvidedCHO
		StmtIterator choIter = model.listStatements(cho, null, (RDFNode)null);
		while (choIter.hasNext()) {
			Statement stmt = choIter.next();
			Property pred = stmt.getPredicate();
			RDFNode obj = stmt.getObject();
			Namespace thisElemNs;
			Element thisElem = null;
			final String predUrl = pred.getURI().toString();
			if (NS.RDF.PROP_TYPE.equals(predUrl)) {
				String objUri = obj.asResource().getURI();
				if (objUri.equals(NS.EDM.CLASS_PROVIDED_CHO)) {
					continue;
				}
				thisElem = new Element("type", ns.get("rdf"));
				thisElem.setAttribute(new Attribute("resource", obj.asResource().getURI(), ns.get("rdf")));
			}else if(NS.DC.PROP_TYPE.equals(predUrl)) {
				if (obj.isResource()) {
					thisElem = new Element("type", ns.get("rdf"));
					thisElem.setAttribute(new Attribute("resource", obj.asResource().getURI(), ns.get("rdf")));
				}
			} else if (NS.PRO.PROP_AUTHOR.equals(predUrl)) {
			// Fall-thru cases:
				// TODO this is not fall-thru anymore
				Element addElem = new Element("creator", ns.get("dcterms"));
				addElem.addContent(obj.asResource().getURI());
				oaiDcDc.addContent(addElem);
			} else {
//				 generic mapping
//				log.warn("Unhandled Predicate: " + pred);
//				continue;
				thisElemNs = ns.get(pred.getNameSpace());
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

		StmtIterator aggIter = model.listStatements(agg, null, (RDFNode)null);
		while (aggIter.hasNext()) {
			Statement stmt = aggIter.next();
			Property pred = stmt.getPredicate();
			RDFNode obj = stmt.getObject();
			// manual mapping
			Namespace thisElemNs;
			Element thisElem = null;
			final String predUrl = pred.getURI().toString();
			if (NS.EDM.PROP_IS_SHOWN_BY.equals(predUrl)) {
				thisElem = new Element("identifier", ns.get("dc"));
				thisElem.setText(obj.asResource().getURI());
				thisElem.setAttribute("linktype", "fulltext");
			} else if (NS.EDM.PROP_IS_SHOWN_AT.equals(predUrl)) {
				thisElem = new Element("identifier", ns.get("dc"));
				thisElem.setText(obj.asResource().getURI());
				thisElem.setAttribute("linktype", "thumbnail");
			} else if (NS.DM2E.PROP_HAS_ANNOTABLE_VERSION_AT.equals(predUrl)) {
				thisElem = new Element("identifier", ns.get("dc"));
				thisElem.setText(obj.asResource().getURI());
				thisElem.setAttribute("linktype", "thumbnail");
			}
			if (null != thisElem) oaiDcDc.addContent(thisElem);
		}

		// open the file
		File f = new File(String.format("%s/%s.xml", this.outputDirectory, baseName));
        if (!f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
        }
		// write the record
//		StringWriter writer;
//		writer = new StringWriter();
        FileOutputStream fos = new FileOutputStream(f);
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(doc, fos);
		
		// validate the record
		Source xmlFile = new StreamSource(f.getAbsoluteFile());
		try {
			validator.validate(xmlFile);
		} catch (SAXException e) {
			log.debug("Not valid: " + e.getMessage());
//			e.printStackTrace();
			throw e;
		}
	}

	public static void main(String[] args) throws JAXBException, IOException {
		Logger log = LoggerFactory.getLogger("main");
		Export_DM2E_to_DC exporter = new Export_DM2E_to_DC(
				//				Config.get(ConfigProp.ENDPOINT_QUERY),
				//				"http://lelystad.informatik.uni-mannheim.de:3030/ds/sparql",
				"http://lelystad.informatik.uni-mannheim.de:3040/ds/sparql",
				"/tmp/dm2e-oai");
		//		exporter.convert();
		exporter.fromEndpoint();

	}

}
