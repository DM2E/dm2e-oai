package eu.dm2e.oai; 
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.eclipse.jetty.http.HttpException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Resources;

import eu.dm2e.linkeddata.Config;
import eu.dm2e.linkeddata.Dm2eApiClient;
import eu.dm2e.linkeddata.model.BaseModel.IdentifierType;
import eu.dm2e.linkeddata.model.Collection;
import eu.dm2e.linkeddata.model.ResourceMap;
import eu.dm2e.linkeddata.model.VersionedDataset;

@Path("oai")
public class Dm2eOaiService {
	
	Logger log = LoggerFactory.getLogger(getClass().getName());

//	private String	baseURI = "http://localhost:7777/oai";
	@Context UriInfo uriInfo;
	private String tplIdentify,
	                tplListMetadataFormats,
	                tplListSets,
	                tplGetRecord,
	                tplListIdentifiers,
	                tplError,
	                tplListRecords;
	XMLOutputter xmlOutput = new XMLOutputter();
	
	/**
	 * Allowed keys for key=value pairs in GET/POST requests
	 */
	enum OaiKey {
		verb,
		identifier,
		resumptionToken,
		from,
		until,
		set,
		metadataPrefix,
	}
	enum OaiVerb {
		Identify,
		ListMetadataFormats,
		ListIdentifiers,
		ListRecords,
		ListSets,
		GetRecord
	}
	
	/**
	 * OAI-PMH error codes
	 */
	enum OaiError {
		badArgument,
		badResumptionToken,
		badVerb,
		cannotDisseminateFormat,
		idDoesNotExist,
		noRecordsMatch,
		noMetadataFormats,
		noSetHierarchy
	}

	// Caching client, hence static (so it's instantiated only once)
	private static final String apiBase = Config.API_BASE;
	private static Dm2eApiClient api = new Dm2eApiClient(apiBase, true);

	public Dm2eOaiService() {
		// pretty print xml
		final Format jdomFormat = Format.getPrettyFormat();
		jdomFormat.setOmitDeclaration(true);
		xmlOutput.setFormat(jdomFormat);
		try {
			tplIdentify = Resources.toString(
					Resources.getResource("/Identify.xml"),
					Charset.forName("UTF-8"));
			tplListMetadataFormats = Resources.toString(
					Resources.getResource("/ListMetadataFormats.xml"),
					Charset.forName("UTF-8"));
			tplListSets = Resources.toString(
					Resources.getResource("/ListSets.xml"),
					Charset.forName("UTF-8"));
			tplGetRecord = Resources.toString(
					Resources.getResource("/GetRecord.xml"),
					Charset.forName("UTF-8"));
			tplListIdentifiers = Resources.toString(
					Resources.getResource("/ListIdentifiers.xml"),
					Charset.forName("UTF-8"));
			tplListRecords = Resources.toString(
					Resources.getResource("/ListRecords.xml"),
					Charset.forName("UTF-8"));
			tplError = Resources.toString(
					Resources.getResource("/error.xml"),
					Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handle POST, take key-value pairs from www-url-encoded body
	 * @param form
	 */
	@POST
	public Response oaiPOST (Form form) {
		return oaiHandler(form.asMap());
	}

	/**
	 * Handle GET, take key-value from query parameters
	 * @return
	 */
	@GET
	public Response oaiGET (@Context UriInfo ui) {
		return oaiHandler(ui.getQueryParameters());
	}

	private Response oaiHandler(MultivaluedMap<String, String> multiValueMap) {
		List<String> illegalKeys = new ArrayList<String>();
		Map<OaiKey,String> kvPairs = new HashMap<OaiKey, String>();
		for (Entry<String, List<String>> entry : multiValueMap.entrySet()) {
			try {
				OaiKey oaiKey = OaiKey.valueOf(entry.getKey());
				kvPairs.put(oaiKey, entry.getValue().get(0));
			} catch (IllegalArgumentException e) {
				illegalKeys.add(entry.getKey());
			}
		}
		if (illegalKeys.size() > 0) {
			return errorIllegalparameter(kvPairs, StringUtils.join(illegalKeys.toArray(), ", "));
		}
		if (null == kvPairs.get(OaiKey.verb)) return errorMissingParameter(kvPairs, OaiKey.verb);

		// GetRecord, Identify, ListIdentifiers, ListMetadataFormats, ListRecords, ListSets
		OaiVerb selectedVerb = OaiVerb.valueOf(kvPairs.get(OaiKey.verb));
		switch(selectedVerb) {
			case Identify: return oaiIdentify(kvPairs);
			case ListMetadataFormats: return oaiListMetadataFormats(kvPairs);
			case ListIdentifiers: return oaiListIdentifiersOrRecords(kvPairs, true);
			case ListRecords: return oaiListIdentifiersOrRecords(kvPairs, false);
			case ListSets: return oaiListSets(kvPairs);
			case GetRecord: return oaiGetRecord(kvPairs);
			default: return errorUnknownVerb(kvPairs);
		}
	}
	public String oaiRequest(Map<OaiKey,String> kvPairs) {
		Element request = new Element("request");
		for (Entry<OaiKey, String> kv : kvPairs.entrySet()) {
			request.setAttribute(kv.getKey().name(), kv.getValue());
		}
		request.addContent(uriInfo.getBaseUri() + "/oai");
		return jdomDocumentToCleanString(request);
	}
	private Response oaiError(Map<OaiKey,String> kvPairs, Response.Status httpStatus, OaiError errorCode, String errorDescription) {
		Map<String,String> valuesMap = new HashMap<String, String>();
		valuesMap.put("request", oaiRequest(kvPairs));
		valuesMap.put("responseDate", api.nowOaiFormatted());
		valuesMap.put("errorCode", errorCode.name());
		valuesMap.put("errorDescription", errorDescription);
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		return Response
					.status(httpStatus)
					.entity(sub.replace(tplError))
					.type(MediaType.TEXT_XML)
					.build()
					;
		
	}
	private Response errorIllegalparameter(Map<OaiKey,String> kvPairs, String paramName){
		return oaiError(kvPairs, Response.Status.BAD_REQUEST, OaiError.badArgument, "Parameter '" + paramName + "' is not a valid OAI-PMH parameter.");
	}
	private Response errorMissingParameter(Map<OaiKey,String> kvPairs, OaiKey verb){
		return oaiError(kvPairs, Response.Status.BAD_REQUEST, OaiError.badArgument, "Missing required parameter '" + verb + "'.");
	}
	private Response errorUnknownVerb(Map<OaiKey,String> kvPairs) {
		return oaiError(kvPairs, Response.Status.BAD_REQUEST, OaiError.badVerb, "Unknown verb '" + kvPairs.get(OaiKey.verb) + "'.");
	}
	private Response errorBadIdentifier(Map<OaiKey,String> kvPairs) {
		return oaiError(kvPairs, Response.Status.BAD_REQUEST, OaiError.idDoesNotExist, 
				"Bad identifier '" + kvPairs.get(OaiKey.identifier) + "'." +
				"\n" + 
				"Identifiers must be of the form 'oai:dm2e:PROVIDER:DATASET:ITEM'"
        );
	}
	private Response errorBadSet(Map<OaiKey,String> kvPairs) {
		return oaiError(kvPairs, Response.Status.BAD_REQUEST, OaiError.idDoesNotExist, 
				"Bad setSpec '" + kvPairs.get(OaiKey.set) + "'." +
				"\n" + 
				"Set specs must be of the form 'provider:PROVIDER' \n" +
				"or\n" +
				"Set specs must be of the form 'collection:PROVIDER:DATASET' \n"
        );
	}
	private Response errorBadResumptionToken(Map<OaiKey,String> kvPairs) {
		return oaiError(kvPairs, Response.Status.BAD_REQUEST, OaiError.badResumptionToken,
				"Bad resumptionToken '" + kvPairs.get(OaiKey.resumptionToken) + "'." +
				"\n" + 
				"resumptionTokens are of the form setSpec__start__limit (though this should not concern harvesters)"
				);
	}
	private Response errorNotFound(Map<OaiKey,String> kvPairs) {
		return oaiError(kvPairs, Response.Status.NOT_FOUND, OaiError.idDoesNotExist, "Record not found: '" + kvPairs.get(OaiKey.identifier) + "'.");
	}
	private Response errorUnsupportedMetadataPrefix(Map<OaiKey,String> kvPairs) {
		return oaiError(kvPairs, Response.Status.BAD_REQUEST, OaiError.cannotDisseminateFormat, "Unsupported metadataPrefix '" + kvPairs.get(OaiKey.metadataPrefix) + "'.");
	}

	
	
	private Response oaiListMetadataFormats(Map<OaiKey,String> kvPairs) {
		Map<String,Object> valuesMap = new HashMap<String, Object>();
		valuesMap.put("responseDate", api.nowOaiFormatted());
		valuesMap.put("baseURI", uriInfo.getBaseUri() + "/oai");
		valuesMap.put("xsdBaseURI", uriInfo.getBaseUri() + "/static");
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		return Response
					.ok()
					.entity(sub.replace(tplListMetadataFormats))
					.type(MediaType.TEXT_XML)
					.build()
					;	}

	private Response oaiIdentify(Map<OaiKey,String> kvPairs) {
		Map<String,Object> valuesMap = new HashMap<String, Object>();
		valuesMap.put("responseDate", api.nowOaiFormatted());
		valuesMap.put("baseURI", uriInfo.getBaseUri() + "/oai");
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		return Response
					.ok()
					.entity(sub.replace(tplIdentify))
					.type(MediaType.TEXT_XML)
					.build()
					;
	}
	private Response oaiListSets(Map<OaiKey,String> kvPairs) {
		Map<String,Object> valuesMap = new HashMap<String, Object>();
		
		Element listSets = new Element("ListSets");
		listSets.setNamespace(Namespace.NO_NAMESPACE);
		// TODO
		for (Collection collection : api.listCollections()) {
			{
				String setId = String.format("provider:%s",
						collection.getProviderId());
				Element set = new Element("set");
				set.setNamespace(Namespace.NO_NAMESPACE);
				Element setSpec = new Element("setSpec");
				setSpec.setNamespace(Namespace.NO_NAMESPACE);
				Element setName = new Element("setName");
				setName.setNamespace(Namespace.NO_NAMESPACE);
				set.addContent(setSpec);
				set.addContent(setName);
				setSpec.addContent(setId);
				setName.addContent("Provider " + setId);
				listSets.addContent(set);
			}
			{
				String setId = String.format("collection:%s:%s",
						collection.getProviderId(),
						collection.getCollectionId());
				Element set = new Element("set");
				set.setNamespace(Namespace.NO_NAMESPACE);
				Element setSpec = new Element("setSpec");
				setSpec.setNamespace(Namespace.NO_NAMESPACE);
				Element setName = new Element("setName");
				setName.setNamespace(Namespace.NO_NAMESPACE);
				set.addContent(setSpec);
				set.addContent(setName);
				setSpec.addContent(setId);
				setName.addContent("Collection " + setId);
				listSets.addContent(set);
			}
		}
		
		valuesMap.put("responseDate", api.nowOaiFormatted());
		valuesMap.put("baseURI", uriInfo.getBaseUri() + "/oai");
		valuesMap.put("request", oaiRequest(kvPairs));
		valuesMap.put("ListSets", jdomDocumentToCleanString(listSets));
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		return Response
					.ok()
					.entity(sub.replace(tplListSets))
					.type(MediaType.TEXT_XML)
					.build()
					;
	}

	
	private Response oaiGetRecord(Map<OaiKey,String> kvPairs) {
		String identifier = kvPairs.get(OaiKey.identifier);
		String metadataPrefix = kvPairs.get(OaiKey.metadataPrefix);
		if (null == identifier) return errorMissingParameter(kvPairs, OaiKey.identifier);
		if (null == metadataPrefix) return errorMissingParameter(kvPairs, OaiKey.metadataPrefix);
		if (! metadataPrefix.equals("oai_dc")) { return errorUnsupportedMetadataPrefix(kvPairs); }
		
		ResourceMap rm = null;
		try {
			rm = api.createResourceMap(identifier, IdentifierType.OAI_IDENTIFIER);
			log.debug(rm.getProvidedCHO_Uri());
			log.debug("" + rm.getModel().size());
		} catch (IllegalArgumentException e) { return errorBadIdentifier(kvPairs);
		} catch (HttpException e) { return errorNotFound(kvPairs);
		} catch (Exception e) { return errorNotFound(kvPairs); }
		
		Document record = api.resourceMapToOaiRecord(rm, metadataPrefix);

		Map<String,Object> valuesMap = new HashMap<String, Object>();
		valuesMap.put("responseDate", api.nowOaiFormatted());
		valuesMap.put("request", oaiRequest(kvPairs));
		valuesMap.put("record", jdomDocumentToCleanString(record));
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		return Response
					.ok()
					.entity(sub.replace(tplGetRecord))
					.type(MediaType.TEXT_XML)
					.build()
					;
	}

	private String jdomDocumentToCleanString(Document el) { return cleanNamespaces(xmlOutput.outputString(el)); }
	private String jdomDocumentToCleanString(Element el) { return cleanNamespaces(xmlOutput.outputString(el)); }
	private String cleanNamespaces(String s) { 
		return s.replaceAll(" xmlns:[a-zA-Z0-9]+=\"[^\"]+\">", ">");
	}

	// TODO Resumption Token Magic to improve performance
	private Response oaiListIdentifiersOrRecords(Map<OaiKey,String> kvPairs, boolean headersOnly) {
		int limit = 10;		// TODO this is for testing
		String set = kvPairs.get(OaiKey.set);
		String resumptionToken = kvPairs.get(OaiKey.resumptionToken);
		if (null == set)
			log.debug("No setSpec argument to ListIdentifiers.");
		else 
			if (set.equals("") || ( ! set.startsWith("provider:") && ! set.startsWith("collection:")))
				return errorBadSet(kvPairs);
		if (null == resumptionToken) {
			log.debug("No resumptionToken argument to ListIdentifiers.");
			resumptionToken="__0"; 	// this means: empty 'setSpec', 'cursor' at 0
		}
		
		// parse resumptionToken
		String[] resumptionTokenSegments = resumptionToken.split("__");
		if (resumptionTokenSegments.length != 2) {
			return errorBadResumptionToken(kvPairs);
		}
		int start = Integer.parseInt(resumptionTokenSegments[1]);

		// Handle setSpec, if not provided use all datasets
		Set<VersionedDataset> datasets = new HashSet<VersionedDataset>();
		if (null==set) {
			log.debug("Iterating all collections");
			for (Collection coll : api.listCollections()) {
				log.debug("Adding all datasets in collection " + coll.getCollectionUri());
				datasets.add(api.createVersionedDataset(coll.getLatestVersion()));
			}
		} else {
			// TODO
			String setType = set.split(":")[0];
			if (setType.equals("collection")) {
				// collection:onb:codices
				Collection createCollection;
				try {
					createCollection = api.createCollection(set, IdentifierType.OAI_SET_SPEC);
				} catch (Exception e) {
					return errorBadSet(kvPairs);
				}
				log.debug(createCollection.getProviderId());
				log.debug(createCollection.getCollectionId());
				datasets.add(api.createVersionedDataset(createCollection.getLatestVersion()));
			} else {
				// provider:onb
				String provider = set.split(":")[1];
				Set<Collection> collectionList = api.listCollections();
				for (Collection collection:collectionList) {
					if (collection.getProviderId().equals(provider)) {
						datasets.add(api.createVersionedDataset(collection.getLatestVersion()));
					}
				}
			}
		}
		
		// Determine resource maps
		List<ResourceMap> resourceMaps = new ArrayList<ResourceMap>();
		StringBuilder headersSB = new StringBuilder();
		for (VersionedDataset dummyDs : datasets) {
			log.debug("Retrieving dataset " + dummyDs.getVersionedDatasetUri());
			VersionedDataset versionedDataset = api.createVersionedDataset(dummyDs);	// so we can cache
			log.debug("Retrieved dataset " + versionedDataset.getVersionedDatasetUri());
			for (ResourceMap resourceMap : versionedDataset.listResourceMaps()) {
				resourceMaps.add(resourceMap);
			}
		}
		// Sort the list by URI so this remains stable
		Collections.sort(resourceMaps);
		
		int completeListSize = resourceMaps.size();
		boolean isFinished = false;
		log.debug("Listing from " + start + " to " + (start + limit));
		for (int i = start ; i < start + limit ; i++ ) {
			log.debug("Retrieving dataset/resourcemap tuple #" + i);
			if (i >= resourceMaps.size()) {
				log.debug("#" + i + ": We're finished");
				isFinished = true;
				break;
			}

			// NOTE
			ResourceMap resourceMap;
			try {
				resourceMap = api.createResourceMap(resourceMaps.get(i));
			} catch (HttpException e) {
				log.error("Error retrieving resource map " + resourceMaps.get(i).getRetrievalUri());
				return errorNotFound(kvPairs);
			}
			log.debug("getUri matches: " + (resourceMaps.get(i).getRetrievalUri().equals(resourceMap.getRetrievalUri())));
			if (headersOnly) {
				headersSB.append(jdomDocumentToCleanString(api.resourceMapToOaiHeader(resourceMap)));
			} else {
				headersSB.append(jdomDocumentToCleanString(api.resourceMapToOaiRecord(resourceMap, "oai_dc")));
			}
		}

		// resumptiontoken
		Element newResumptionToken = new Element("resumptionToken");
		if (! isFinished) {
			newResumptionToken.setAttribute("cursor", Integer.toString(start));
			newResumptionToken.setAttribute("completeListSize", Integer.toString(completeListSize));
			newResumptionToken.addContent(set + "__" + (start + limit));
		}

		log.debug("All dataset/resourcemap id tuples retrieved retrieved");
		Map<String,Object> valuesMap = new HashMap<String, Object>();
		valuesMap.put("responseDate", api.nowOaiFormatted());
		valuesMap.put("request", oaiRequest(kvPairs));
		valuesMap.put("list", headersSB.toString());
		valuesMap.put("resumptionToken", jdomDocumentToCleanString(newResumptionToken));
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		String tplToUse = headersOnly ? tplListIdentifiers : tplListRecords;
		return Response
					.ok()
					.entity(sub.replace(tplToUse))
					.type(MediaType.TEXT_XML)
					.build()
					;
	}
}
