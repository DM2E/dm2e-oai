package eu.dm2e.oai; 
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.jena.riot.RiotNotFoundException;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Resources;

import eu.dm2e.linkeddata.Dm2eApiClient;
import eu.dm2e.linkeddata.model.ResourceMap;

@Path("oai")
public class Dm2eOaiService {
	
	Logger log = LoggerFactory.getLogger(getClass().getName());

	@Context UriInfo ui;
	private Object	baseURI = "http://localhost:7777/oai";
	private String tplIdentify;
	private String tplListMetadataFormats;
	private String tplListSets;
	DateTimeFormatter iso8601formatter = ISODateTimeFormat.dateTime();
	XMLOutputter xmlOutput = new XMLOutputter();

	private Dm2eApiClient api = new Dm2eApiClient("http://lelystad.informatik.uni-mannheim.de:3000/direct");

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

	private Response errorNotFound(String identifier) {
		return Response.status(Response.Status.NOT_FOUND).entity("Unknown identifier '" + identifier + "'").build();
	}
	
	public Dm2eOaiService() {
		// pretty print xml
		xmlOutput.setFormat(Format.getPrettyFormat());
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
		case "ListSets": return oaiListSets();
		case "GetRecord": return oaiGetRecord();
        default: return errorUnknownVerb(verb);
		}
	}
	
	private Response oaiListMetadataFormats() {
		Map<String,Object> valuesMap = new HashMap<>();
		String nowFormatted = iso8601formatter.print(DateTime.now());
		valuesMap.put("responseDate", nowFormatted);
		valuesMap.put("baseURI", baseURI);
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		return Response
					.ok()
					.entity(sub.replace(tplListMetadataFormats))
					.type(MediaType.APPLICATION_XML)
					.build()
					;	}

	private Response oaiIdentify() {
		Map<String,Object> valuesMap = new HashMap<>();
		String nowFormatted = iso8601formatter.print(DateTime.now());
		valuesMap.put("responseDate", nowFormatted);
		valuesMap.put("baseURI", baseURI);
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		return Response
					.ok()
					.entity(sub.replace(tplIdentify))
					.type(MediaType.APPLICATION_XML)
					.build()
					;
	}

	private Response oaiListSets() {
		Map<String,Object> valuesMap = new HashMap<>();
		
		Element request = new Element("request");
		request.setAttribute("verb", "ListSets");

		Element listSets = new Element("ListSets");
		for (String ds : api.listDatasets()) {
			ds = ds.replaceAll("/", "__");
			Element set = new Element("set");
			Element setSpec = new Element("setSpec");
			Element setName = new Element("setName");
			set.addContent(setSpec);
			set.addContent(setName);
			setSpec.addContent("dataset:" + ds);
			setName.addContent("Dataset " + ds);
			listSets.addContent(set);
		}
		
		String nowFormatted = iso8601formatter.print(DateTime.now());
		valuesMap.put("responseDate", nowFormatted);
		valuesMap.put("baseURI", baseURI);
		valuesMap.put("request", jdomElementToString(request));
		valuesMap.put("ListSets", jdomElementToString(listSets));
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		return Response
					.ok()
					.entity(sub.replace(tplListSets))
					.type(MediaType.APPLICATION_XML)
					.build()
					;
	}
	
	private Response oaiGetRecord() {
		String identifier, metadataPrefix;
		try { identifier = ui.getQueryParameters().get("identifier").get(0);
		} catch (NullPointerException e) { return errorMissingParameter("identifier"); }
		try { metadataPrefix = ui.getQueryParameters().get("metadataPrefix").get(0);
		} catch (NullPointerException e) { return errorMissingParameter("metadataPrefix"); }
		
		String[] idSegments = identifier.split("___");
		if (idSegments.length != 2) {
			return errorBadIdentifier(identifier);
		}
		String datasetId = idSegments[0].replace("__", "/");
		String resourceMapId = idSegments[1].replace("__", "/");
		ResourceMap rm = null;
		try {
			 rm = api.getResourceMap(datasetId, resourceMapId);
		} catch (RiotNotFoundException e) {
			return errorNotFound(identifier);
		}

		return Response.ok().entity(
				"datasetId: " + datasetId + "\n"
                + "resourceMapId: " + resourceMapId
				+ "\n"
				+ api.dumpModel(rm.getModel())
                ).build() ;
	}

	
}
