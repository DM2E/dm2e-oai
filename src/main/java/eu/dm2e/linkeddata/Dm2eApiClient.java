package eu.dm2e.linkeddata;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
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
import eu.dm2e.linkeddata.export.OaiDublinCoreRecord;
import eu.dm2e.linkeddata.model.BaseModel.IdentifierType;
import eu.dm2e.linkeddata.model.Collection;
import eu.dm2e.linkeddata.model.ResourceMap;
import eu.dm2e.linkeddata.model.VersionedDataset;



public class Dm2eApiClient {

	private static final Logger log = LoggerFactory.getLogger(Dm2eApiClient.class);

	private String	apiBase;

	private FileManager fileManager;
	
	// ---------
	// Exporters
	// ---------
	
	private static Map<Class<? extends BaseXMLExporter>, BaseXMLExporter>	exporters;	
	static {
		exporters = new HashMap<Class<? extends BaseXMLExporter>, BaseXMLExporter>();
		exporters.put(OaiDublinCoreHeader.class, new OaiDublinCoreHeader());
		exporters.put(OaiDublinCoreRecord.class, new OaiDublinCoreRecord());
	}
	
	private static BaseXMLExporter getExporter(Class<? extends BaseXMLExporter> clazz) {
		return exporters.get(clazz);
	}

	/**
	 * @see {@link BaseXMLExporter#getXMLOutputFactory()}
	 */
	public static XMLOutputFactory getXMLOutputFactory() {
		return BaseXMLExporter.getXMLOutputFactory();
	}

	/**
	 * @see {@link BaseXMLExporter#getIndentingXMLStreamWriter(Writer)}
	 */
	public static XMLStreamWriter getIndentingXMLStreamWriter(Writer writer) throws XMLStreamException {
		return BaseXMLExporter.getIndentingXMLStreamWriter(writer);
	}

	/**
	 * @see {@link BaseXMLExporter#getXMLStreamWriter(Writer)}
	 */
	public static XMLStreamWriter getXMLStreamWriter(Writer writer) throws XMLStreamException {
		return BaseXMLExporter.getXMLStreamWriter(writer);
	}

	/**
	 * @see {@link BaseXMLExporter#setNamespaces(XMLStreamWriter)}
	 * @throws XMLStreamException 
	 */
	public static void setNamespaces(XMLStreamWriter xml) throws XMLStreamException {
		BaseXMLExporter.setNamespaces(xml);
	}

	// -------
	// Utility
	// -------

	/**
	 * @see BaseExporter#nowOaiFormatted()
	 */
	public String nowOaiFormatted() {
		return BaseExporter.nowOaiFormatted();
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
	
	// ------------
	// Constructors
	// ------------

	/**
	 * @param apiBase the base URI for the DM2E Linked Data API
	 */
	public Dm2eApiClient(String apiBase) {
		this(apiBase, true);
	}

	/**
	 * @param apiBase the base URI for the DM2E Linked Data API
	 * @param useCaching whether to use caching
	 */
	public Dm2eApiClient(String apiBase, boolean useCaching) {
		this.apiBase = apiBase;
		if (useCaching) {
			this.fileManager = Dm2eApiClient.setupFileManager();
		}
	}
	
	/**
	 * @return a {@link MapdbFileManager} backed Jena {@link FileManager}
	 */
	public static FileManager setupFileManager() {
		FileManager fm = new MapdbFileManager();
		fm.setModelCaching(true);
		return fm;
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

	// -------------
	// Create Models
	// -------------

	/**
	 * Create a VersionedDataset and read it from apiBase
	// http://lelystad.informatik.uni-mannheim.de:3000/direct/dataset/bbaw/dta/1386762086592
	 */
	public VersionedDataset createVersionedDataset(String providerId, String datasetId, String versionId) {
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

	// --------------------
	// Convert ResourceMaps
	// --------------------

	/**
	 * Convert a ResourceMap to a oai:record
	 * @param resMap	ResourceMap to convert
	 * @param metadataPrefix	Which type of metadata (currently: only 'oai_dc')
	 * @return
	 * @throws XMLStreamException 
	 */
	public void resourceMapToOaiRecord(ResourceMap resMap, String metadataPrefix, XMLStreamWriter writer) throws XMLStreamException {
		if (metadataPrefix.equals("oai_dc")) {
			exporters.get(OaiDublinCoreRecord.class).writeResourceMapToXML(resMap, writer);
		} else {
			throw new RuntimeException("Unhandled metadataPrefix '" + metadataPrefix + "'.");
		}
	}

	/**
	 * Convert a ResourceMap to a oai:header
	 * @param resMap	ResourceMap to convert
	 * @param metadataPrefix	Which type of metadata (currently: only 'oai_dc')
	 * @return
	 * @throws XMLStreamException 
	 */
	public void resourceMapToOaiHeader(ResourceMap resMap, String metadataPrefix, XMLStreamWriter writer) throws XMLStreamException {
		if (metadataPrefix.equals("oai_dc")) {
			getExporter(OaiDublinCoreHeader.class).writeResourceMapToXML(resMap, writer);
		} else {
			throw new RuntimeException("Unhandled metadataPrefix '" + metadataPrefix + "'.");
		}
	}


}