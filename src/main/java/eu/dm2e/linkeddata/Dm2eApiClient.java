package eu.dm2e.linkeddata;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.eclipse.jetty.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

import eu.dm2e.NS;
import eu.dm2e.grafeo.jena.MapdbFileManager;
import eu.dm2e.linkeddata.export.BaseExporter;
import eu.dm2e.linkeddata.export.BaseXMLExporter;
import eu.dm2e.linkeddata.export.OaiDublinCoreHeader;
import eu.dm2e.linkeddata.export.OaiDublinCoreMetadata;
import eu.dm2e.linkeddata.model.BaseModel;
import eu.dm2e.linkeddata.model.BaseModel.IdentifierType;
import eu.dm2e.linkeddata.model.Collection;
import eu.dm2e.linkeddata.model.ResourceMap;
import eu.dm2e.linkeddata.model.VersionedDataset;
//import org.jdom2.Element;
//import org.jdom2.Namespace;


/**
 * 
 * Only if:
 * - ?cho dm2e:displayLevel "true" (will be ?agg)
 * 
 * Fields that reference Places (throw away if not is-a edm:Place):
 * - dm2e:isPrintedAt : edm:Place
 * - dcterms:spatial : edm:Place
 * - dcterms:publishedAt : edm:Place | edm:WebResource
 * - edm:currentLocation : edm:Place | Literal 
 * - dc:subject : edm:Place
 * 
 * Fields that reference people (throw away not is-a foaf:Person)
 * - dc:creator
 * - dm2e:refersTo foaf:Person
 * - dm2e:wasStudiedBy / dm2e:wasTaughBy foaf:Person
 * - dm2e:mentioned : edm:Agent (foaf:Person / foaf:Organization)
 * - dc:subject : edm:Agent foaf:Person
 * - [dc:publisher : edm:Agent => don't know whether foaf:Person or foaf:Organization, skip for now]
 * - dm2e:artist
 * - pro:author
 * - dm2e:composer
 * - dc:contributor
 * - bibo:editor 
 * - dm2e:honoree 
 * - pro:illustrator 
 * - dm2e:mentioned 
 * - dm2e:portrayed 
 * - dm2e:misattributed 
 * - dm2e:painter
 * - dm2e:patron
 * - bibo:recipient
 * - pro:translator
 * - dm2e:writer
 * 
 * Title fields
 * - dc:title
 * - dm2e:subTitle
 * - dcterms:alternative
 * 
 * Format / Medium
 * - dc:format
 * - dcterms:medium
 * 
 * Timespan / Dates: 
 * - dcterms:created	}		ignore		  parse			edm:begin -> year
 * - dcterms:issued		} -> [xsd:string], xsd:dateTime oder edm:TimeSpan
 * - dcterms:temporal	}
 * - dc:subject (nur wenn is-a edm:Timespan
 * 
 * Genre:
 * - dm2e:genre : skos:Concept
 * 
 * Table Of Contents
 * - dcterms:tableOfContents
 * 
 * Language:
 * - dc:language -> dc:language
 * 
 * Links:
 * - Pundit: dm2e:hasAnnotatableVersionAt
 * - Thumbnail: edm:object ; edm:isShownBy
 * - Erste Seite: SPARQL (Liste alle Seiten, sortiere nach URL, nimm erste)
 * - 
 * 
 */

public class Dm2eApiClient {

	private static final Logger log = LoggerFactory.getLogger(Dm2eApiClient.class);

	private String	apiBase;

	private FileManager fileManager;
	
	private static Map<Class<? extends BaseXMLExporter>, BaseXMLExporter>	exporters;	
	static {
		exporters = new HashMap<Class<? extends BaseXMLExporter>, BaseXMLExporter>();
		exporters.put(OaiDublinCoreHeader.class, new OaiDublinCoreHeader());
		exporters.put(OaiDublinCoreMetadata.class, new OaiDublinCoreMetadata());
	}

	public Dm2eApiClient(String apiBase) {
		this(apiBase, false);
	}
	public Dm2eApiClient(String apiBase, boolean useCaching) {
		this.apiBase = apiBase;
		this.fileManager = Dm2eApiClient.setupFileManager();
	}
	
	public static FileManager setupFileManager() {
		FileManager fm = new MapdbFileManager();
		fm.setModelCaching(true);
		return fm;
	}

	/**
	 * @param xml the {@link XMLStreamWriter} to set prefixes for
	 * @throws XMLStreamException
	 */
	public static void setNamespaces(XMLStreamWriter xml) throws XMLStreamException {
		xml.setPrefix("oai", NS.OAI.BASE);
		xml.setPrefix("oai_dc", NS.OAI_DC.BASE);
		xml.setPrefix("rights", NS.OAI_RIGHTS.BASE);
		xml.setPrefix("dcterms", NS.DCTERMS.BASE);
		xml.setPrefix("dc", NS.DC.BASE);
		xml.setPrefix("edm", NS.EDM.BASE);
		xml.setPrefix("rdf", NS.RDF.BASE);
		xml.setPrefix("dm2e", NS.DM2E.BASE);
		xml.setPrefix("pro", NS.PRO.BASE);
		xml.setPrefix("bibo", NS.BIBO.BASE);
		xml.setPrefix("owl", NS.OWL.BASE);
	}

	/**
	 * @param model the {@link Model} to dump as a sting
	 * @return N3 Serialized version of the model
	 */
	public String dumpModel(Model model) {
		StringWriter strwriter = new StringWriter();
		model.write(strwriter, "N3");
		return strwriter.toString();
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
	// http://lelystad.informatik.uni-mannheim.de:3000/direct/dataset/bbaw/dta/1386762086592
	 */
	public BaseModel createVersionedDataset(String providerId, String datasetId, String versionId) {
		final VersionedDataset vds = new VersionedDataset(fileManager, apiBase, null, providerId, datasetId, versionId);
		return createVersionedDataset(vds);
	}
	public VersionedDataset createVersionedDataset(final VersionedDataset dataset) {
		dataset.read();
		return dataset;
	}
	public BaseModel createResourceMap(String providerId, String datasetId, String itemId, String versionId) throws IllegalArgumentException, HttpException {
		final ResourceMap resourceMap = new ResourceMap(fileManager, apiBase, null, providerId, datasetId, itemId, versionId);
		return createResourceMap(resourceMap);
	}
	public ResourceMap createResourceMap(final ResourceMap resourceMap) throws IllegalArgumentException, HttpException {
		resourceMap.read();
		if (! resourceMap.isRead) {
			throw new HttpException(404, "404 NOT FOUND");
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
	/**
	 * Convert a ResourceMap to a oai:record
	 * @param resMap	ResourceMap to convert
	 * @param metadataPrefix	Which type of metadata (currently: only 'oai_dc')
	 * @return
	 * @throws XMLStreamException 
	 */
	public void resourceMapToOaiRecord(ResourceMap resMap, String metadataPrefix, XMLStreamWriter writer) throws XMLStreamException {
		if (metadataPrefix.equals("oai_dc")) {
			exporters.get(OaiDublinCoreMetadata.class).writeResourceMapToXML(resMap, writer);
		} else {
			throw new RuntimeException("Unhandled metadataPrefix '" + metadataPrefix + "'.");
		}
	}

	/**
	 * @see BaseExporter#nowOaiFormatted()
	 */
	public String nowOaiFormatted() {
		return BaseExporter.nowOaiFormatted();
	}
}