package eu.dm2e.oai; 
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

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

	@Context UriInfo ui;
	private String	baseURI = "http://localhost:7777/oai";
	private String tplIdentify,
	                tplListMetadataFormats,
	                tplListSets,
	                tplGetRecord,
	                tplListIdentifiers,
	                tplListRecords;
	XMLOutputter xmlOutput = new XMLOutputter();

	// Caching client, hence static
	private static Dm2eApiClient api = new Dm2eApiClient("http://lelystad.informatik.uni-mannheim.de:3000/direct", true);
	private String jdomElementToString(Element el) {
		StringWriter strwriter = new StringWriter();
		try {
			xmlOutput.output(el, strwriter);
		} catch (IOException e) {
			log.error("Exception writing XML: " + e);
//			log.debug("Input was " + el);
		}
		return strwriter.toString();
	}

	private Response errorMissingParameter(String paramName){
		return Response.status(Response.Status.BAD_REQUEST).entity("Missing '" + paramName + "' parameter.").build();
	}
	private Response errorUnknownVerb(String verb){
		return Response.status(Response.Status.BAD_REQUEST).entity("Unknown verb '" + verb + "'.").build();
	}
	private Response errorBadIdentifier(String identifier) {
		return Response.status(Response.Status.BAD_REQUEST).entity(
				"Bad identifier '" + identifier + "'." +
				"\n" + 
				"Identifiers are of the form DATASETID___RESOURCEMAPID"
				).build();
	}
	private Response errorBadResumptionToken(String resumptionToken) {
		return Response.status(Response.Status.BAD_REQUEST).entity(
				"Bad resumptionToken '" + resumptionToken + "'." +
				"\n" + 
				"resumptionTokens are of the form setSpec__start__limit (though this should not concern harvesters)"
				).build();
	}
	private Response errorNotFound(String identifier) {
		return Response.status(Response.Status.NOT_FOUND).entity("Unknown identifier '" + identifier + "'").build();
	}
	private Response errorUnsupportedMetadataPrefix(String metadataPrefix) {
		return Response.status(Response.Status.BAD_REQUEST).entity("Unsupported metadataPrefix '" + metadataPrefix + "'").build();
	}
	
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@GET
	public Response oaiHandler(
			// GetRecord, Identify, ListIdentifiers, ListMetadataFormats, ListRecords, ListSets
			@QueryParam("verb") String verb
			) {
		if (null == verb) return errorMissingParameter("verb");
		switch(verb) {
		case "Identify": return oaiIdentify();
		case "ListMetadataFormats": return oaiListMetadataFormats();
		case "ListIdentifiers": return oaiListIdentifiersOrRecords(true);
		case "ListRecords": return oaiListIdentifiersOrRecords(false);
		case "ListSets": return oaiListSets();
		case "GetRecord": return oaiGetRecord();
        default: return errorUnknownVerb(verb);
		}
	}
	
	private Response oaiListMetadataFormats() {
		Map<String,Object> valuesMap = new HashMap<>();
		valuesMap.put("responseDate", api.nowOaiFormatted());
		valuesMap.put("baseURI", baseURI);
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		return Response
					.ok()
					.entity(sub.replace(tplListMetadataFormats))
					.type(MediaType.TEXT_XML)
					.build()
					;	}

	private Response oaiIdentify() {
		Map<String,Object> valuesMap = new HashMap<>();
		valuesMap.put("responseDate", api.nowOaiFormatted());
		valuesMap.put("baseURI", baseURI);
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		return Response
					.ok()
					.entity(sub.replace(tplIdentify))
					.type(MediaType.TEXT_XML)
					.build()
					;
	}

	private Response oaiListSets() {
		Map<String,Object> valuesMap = new HashMap<>();
		
		Element request = httpRequestAsOaiRequest();

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
		valuesMap.put("baseURI", baseURI);
		valuesMap.put("request", jdomElementToString(request));
		valuesMap.put("ListSets", jdomElementToString(listSets));
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		return Response
					.ok()
					.entity(sub.replace(tplListSets))
					.type(MediaType.TEXT_XML)
					.build()
					;
	}

	public Element httpRequestAsOaiRequest() {
		Element request = new Element("request");
		for (String qParam : ui.getQueryParameters().keySet()) {
			request.setAttribute(qParam, ui.getQueryParameters().getFirst(qParam));
		}
		request.addContent(baseURI);
		return request;
	}
	
	private Response oaiGetRecord() {
		String identifier, metadataPrefix;
		try { identifier = ui.getQueryParameters().get("identifier").get(0);
		} catch (NullPointerException e) { return errorMissingParameter("identifier"); }
		try { metadataPrefix = ui.getQueryParameters().get("metadataPrefix").get(0);
		} catch (NullPointerException e) { return errorMissingParameter("metadataPrefix"); }
		if (! metadataPrefix.equals("oai_dc")) {
			return errorUnsupportedMetadataPrefix(metadataPrefix);
		}
		
		String[] idSegments;
		try {
			idSegments = api.unoaifyId(identifier);
		} catch (IllegalArgumentException e) {
			return errorBadIdentifier(identifier);
		}
		String datasetId = idSegments[0];
		String resourceMapId = idSegments[1];

		ResourceMap rm = null;
		try { rm = api.getResourceMap(datasetId, resourceMapId);
		} catch (RiotNotFoundException e) { return errorNotFound(identifier); }
		
		Document record = api.resourceMapToOaiRecord(rm, metadataPrefix);

		Map<String,Object> valuesMap = new HashMap<>();
		valuesMap.put("responseDate", api.nowOaiFormatted());
		valuesMap.put("request", xmlOutput.outputString(httpRequestAsOaiRequest()));
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
	private Response oaiListIdentifiersOrRecords(boolean headersOnly) {
		String setSpec, resumptionToken;
		int limit = 10;		// TODO this is for testing
		try { setSpec = ui.getQueryParameters().get("setSpec").get(0);
		} catch (NullPointerException e) { log.debug("No setSpec argument to ListIdentifiers."); }
		try {
			resumptionToken = ui.getQueryParameters().get("resumptionToken").get(0);
		} catch (NullPointerException e) {
			log.debug("No resumptionToken argument to ListIdentifiers.");
			resumptionToken="__0"; 	// this means: empty 'setSpec', 'cursor' at 0
		}
		
		// parse resumptionToken
		String[] resumptionTokenSegments = resumptionToken.split("__");
		if (resumptionTokenSegments.length != 2) {
			return errorBadResumptionToken(resumptionToken);
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
		valuesMap.put("request", xmlOutput.outputString(httpRequestAsOaiRequest()));
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
