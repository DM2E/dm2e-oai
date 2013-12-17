package eu.dm2e.oai; 
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
import org.apache.jena.riot.RiotNotFoundException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Resources;

import eu.dm2e.linkeddata.Dm2eApiClient;
import eu.dm2e.linkeddata.model.Dataset;
import eu.dm2e.linkeddata.model.ResourceMap;

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

	// Caching client, hence static
	private static Dm2eApiClient api = new Dm2eApiClient("http://lelystad.informatik.uni-mannheim.de:3000/direct", true);

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

	private Response oaiError(Map<OaiKey,String> kvPairs, Response.Status httpStatus, OaiError errorCode, String errorDescription) {
		Map<String,String> valuesMap = new HashMap<>();
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
				"Identifiers are of the form DATASETID___RESOURCEMAPID"
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

	
	@POST
	public Response oaiPOST (Form form) {
		return oaiHandler(form.asMap());
	}

	@GET
	public Response oaiGET (@Context UriInfo ui) {
		return oaiHandler(ui.getQueryParameters());
	}

	private Response oaiHandler(MultivaluedMap<String, String> multiValueMap) {
		List<String> illegalKeys = new ArrayList<>();
		Map<OaiKey,String> kvPairs = new HashMap<>();
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
		switch(kvPairs.get(OaiKey.verb)) {
			case "Identify": return oaiIdentify(kvPairs);
			case "ListMetadataFormats": return oaiListMetadataFormats(kvPairs);
			case "ListIdentifiers": return oaiListIdentifiersOrRecords(kvPairs, true);
			case "ListRecords": return oaiListIdentifiersOrRecords(kvPairs, false);
			case "ListSets": return oaiListSets(kvPairs);
			case "GetRecord": return oaiGetRecord(kvPairs);
			default: return errorUnknownVerb(kvPairs);
		}
	}
	
	private Response oaiListMetadataFormats(Map<OaiKey,String> kvPairs) {
		Map<String,Object> valuesMap = new HashMap<>();
		valuesMap.put("responseDate", api.nowOaiFormatted());
		valuesMap.put("baseURI", uriInfo.getBaseUri() + "/oai");
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		return Response
					.ok()
					.entity(sub.replace(tplListMetadataFormats))
					.type(MediaType.TEXT_XML)
					.build()
					;	}

	private Response oaiIdentify(Map<OaiKey,String> kvPairs) {
		Map<String,Object> valuesMap = new HashMap<>();
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
		Map<String,Object> valuesMap = new HashMap<>();
		
		Element listSets = new Element("ListSets");
		for (String datasetId : api.listDatasets()) {
			datasetId = api.oaifyId(datasetId);
			Element set = new Element("set");
			Element setSpec = new Element("setSpec");
			Element setName = new Element("setName");
			set.addContent(setSpec);
			set.addContent(setName);
			setSpec.addContent("dataset:" + api.oaifyId(datasetId));
			setName.addContent("Dataset " + api.oaifyId(datasetId) + " (actually it's" + datasetId + " but OAI-PMH forbids that)");
			listSets.addContent(set);
		}
		
		valuesMap.put("responseDate", api.nowOaiFormatted());
		valuesMap.put("baseURI", uriInfo.getBaseUri() + "/oai");
		valuesMap.put("request", oaiRequest(kvPairs));
		valuesMap.put("ListSets", xmlOutput.outputString(listSets));
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		return Response
					.ok()
					.entity(sub.replace(tplListSets))
					.type(MediaType.TEXT_XML)
					.build()
					;
	}

	public String oaiRequest(Map<OaiKey,String> kvPairs) {
		Element request = new Element("request");
		for (Entry<OaiKey, String> kv : kvPairs.entrySet()) {
			request.setAttribute(kv.getKey().name(), kv.getValue());
		}
		request.addContent(uriInfo.getBaseUri() + "/oai");
		return xmlOutput.outputString(request);
	}
	
	private Response oaiGetRecord(Map<OaiKey,String> kvPairs) {
		String identifier = kvPairs.get(OaiKey.identifier);
		String metadataPrefix = kvPairs.get(OaiKey.metadataPrefix);
		if (null == identifier) return errorMissingParameter(kvPairs, OaiKey.identifier);
		if (null == metadataPrefix) return errorMissingParameter(kvPairs, OaiKey.metadataPrefix);
		if (! metadataPrefix.equals("oai_dc")) {
			return errorUnsupportedMetadataPrefix(kvPairs);
		}
		
		String[] idSegments;
		try {
			idSegments = api.unoaifyId(identifier);
		} catch (IllegalArgumentException e) {
			return errorBadIdentifier(kvPairs);
		}
		String datasetId = idSegments[0];
		String resourceMapId = idSegments[1];

		ResourceMap rm = null;
		try { rm = api.getResourceMap(datasetId, resourceMapId);
		} catch (RiotNotFoundException e) { return errorNotFound(kvPairs); }
		
		Document record = api.resourceMapToOaiRecord(rm, metadataPrefix);

		Map<String,Object> valuesMap = new HashMap<>();
		valuesMap.put("responseDate", api.nowOaiFormatted());
		valuesMap.put("request", oaiRequest(kvPairs));
		valuesMap.put("record", xmlOutput.outputString(record));
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		return Response
					.ok()
					.entity(sub.replace(tplGetRecord))
					.type(MediaType.TEXT_XML)
					.build()
					;
	}

	// TODO Resumption Token Magic to improve performance
	private Response oaiListIdentifiersOrRecords(Map<OaiKey,String> kvPairs, boolean headersOnly) {
		int limit = 10;		// TODO this is for testing
		String setSpec = kvPairs.get(OaiKey.set);
		String resumptionToken = kvPairs.get(OaiKey.resumptionToken);
		if (null == setSpec)
			log.debug("No setSpec argument to ListIdentifiers.");
		if (null == resumptionToken) {
			log.debug("No resumptionToken argument to ListIdentifiers.");
			resumptionToken="__0"; 	// this means: empty 'setSpec', 'cursor' at 0
		}
		
		// parse resumptionToken
		String[] resumptionTokenSegments = resumptionToken.split("__");
		if (resumptionTokenSegments.length != 2) {
			return errorBadResumptionToken(kvPairs);
		}
		setSpec = resumptionTokenSegments[0];
		int start = Integer.parseInt(resumptionTokenSegments[1]);

		StringBuilder headersSB = new StringBuilder();
		List<List<String>> datasetResourceMapTuples = new ArrayList<>();
		
		// Handle setSpec, if not provided use all datasets
		Set<String> datasetIds = new HashSet<>();
		if (setSpec.equals("")) {
			datasetIds.addAll(api.listDatasets());
		} else {
			datasetIds.add(api.unoaifyId(setSpec)[0]);
		}
		
		// generate datasetId / resourceMapId tuples
		for (String datasetId : datasetIds) {
			log.debug("Retrieving dataset " + datasetId);
			Dataset dataset = api.getDataset(datasetId);
			log.debug("Retrieved dataset " + dataset);
			for (String resourceMapId : api.listResourceMaps(dataset)) {
				List<String> datasetResourceMapTuple = new ArrayList<>();
				datasetResourceMapTuple.add(datasetId);
				datasetResourceMapTuple.add(resourceMapId);
				datasetResourceMapTuples.add(datasetResourceMapTuple);
			}
		}
		
		int completeListSize = datasetResourceMapTuples.size();
		boolean isFinished = false;
		log.debug("Listing from " + start + " to " + (start + limit));
		for (int i = start ; i < start + limit ; i++ ) {
			log.debug("Retrieving dataset/resourcemap tuple #" + i);
			if (i >= datasetResourceMapTuples.size()) {
				log.debug("We're finished");
				isFinished = true;
				break;
			}
			List<String> datasetResourceMapTuple = datasetResourceMapTuples.get(i);
			String datasetId = datasetResourceMapTuple.get(0);
			String resourceMapId = datasetResourceMapTuple.get(1);
			
			// NOTE
			// This is the heavy work
			ResourceMap resourceMap = api.getResourceMap(datasetId, resourceMapId);
			if (headersOnly) {
				headersSB.append(xmlOutput.outputString(api.resourceMapToOaiHeader(resourceMap)));
			} else {
				headersSB.append(xmlOutput.outputString(api.resourceMapToOaiRecord(resourceMap, "oai_dc")));
			}
		}

		// resumptiontoken
		Element newResumptionToken = new Element("resumptionToken");
		if (! isFinished) {
			newResumptionToken.setAttribute("cursor", Integer.toString(start));
			newResumptionToken.setAttribute("completeListSize", Integer.toString(completeListSize));
			newResumptionToken.addContent(setSpec + "__" + (start + limit));
		}

		log.debug("All dataset/resourcemap id tuples retrieved retrieved");
		Map<String,Object> valuesMap = new HashMap<>();
		valuesMap.put("responseDate", api.nowOaiFormatted());
		valuesMap.put("request", oaiRequest(kvPairs));
		valuesMap.put("list", headersSB.toString());
		valuesMap.put("resumptionToken", xmlOutput.outputString(newResumptionToken));
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
