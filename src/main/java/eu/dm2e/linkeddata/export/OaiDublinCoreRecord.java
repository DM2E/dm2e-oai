package eu.dm2e.linkeddata.export;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Resource;

import eu.dm2e.NS;
import eu.dm2e.linkeddata.Config;
import eu.dm2e.linkeddata.model.ResourceMap;
import eu.dm2e.linkeddata.util.ScrubbingStringBuilder;

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
public class OaiDublinCoreRecord extends BaseXMLExporter {

	private static final Logger log = LoggerFactory.getLogger(OaiDublinCoreRecord.class);
	
	private static OaiDublinCoreHeader oaiDublinCoreHeader = new OaiDublinCoreHeader();
	
	public void writeResourceMapToXML(ResourceMap resMap, XMLStreamWriter xml) throws XMLStreamException {
	
			// open the file
	//		String baseName = resMap.getAggregationUri();
	//		baseName = baseName.replaceAll("[^a-zA-Z0-9]+", "");
			
			// <oai:record>
			xml.writeStartElement("record");
			
			// <oai:header>
			oaiDublinCoreHeader.writeResourceMapToXML(resMap, xml);
			// </oai:header>
			
			// <oai:metadata>
			xml.writeStartElement("metadata");
			
			// <oai_dc:dc>
			xml.writeStartElement(NS.OAI_DC.BASE, "dc");

			// <dc:type>
			{
				// <dc:type>
				xml.writeStartElement(NS.DC.BASE, "type"); 
				xml.writeCharacters(lastUriSegment(resMap.getDcType()));
				// </dc:type>
				xml.writeEndElement(); // </dc:type>
			}

			// <dc:type>
			{
				// <dc:identifier>
				xml.writeStartElement(NS.DC.BASE, "identifier"); 
				xml.writeAttribute("linktype", "rdf");
				xml.writeCharacters(resMap.getResourceMapUri());
				// </dc:identifier>
				xml.writeEndElement(); // </dc:identifier>
			}
			
			// <dc:identifier linktype="thumbnail">
			{
				String thumbnailLink = resMap.getThumbnailLink();
				if (null != thumbnailLink) {
					// <dc:identifier>
					xml.writeStartElement(NS.DC.BASE, "identifier"); 
					xml.writeAttribute("linktype", "thumbnail");
					xml.writeCharacters(thumbnailLink);
					// </dc:identifier>
					xml.writeEndElement(); // </dc:identifier>
				}
			}
	
			// PunditLink
			// http://demo.feed.thepund.it/?dm2e=http://lelystad.informatik.uni-mannheim.de:3000/direct/item/bbaw/dta/16821/f0001&conf=timeline-demo.js
			// <dc:identifier linktype="annotate">
			{
				String firstPageLink = resMap.getFirstPageLink();
				if (null != firstPageLink) {
					// <dc:identifier>
					xml.writeStartElement(NS.DC.BASE, "identifier");
					xml.writeAttribute("linktype", "annotate");
					xml.writeCharacters(String.format(Config.PUNDIT_FMT_STRING, firstPageLink));
					// </dc:identifier>
					xml.writeEndElement();
				}
			}
			
			// 
			// <dc:publisher> 
			{
				// == <edm:dataProvider>
				xml.writeStartElement(NS.DC.BASE, "publisher");
				xml.writeCharacters(resMap.getProviderId());
				xml.writeEndElement();
				
				//
				for (String publisher : resMap.getLiteralValues(NS.DC.PROP_PUBLISHER, null)) {
					xml.writeStartElement(NS.DC.BASE, "publisher");
					xml.writeCharacters(publisher);
					xml.writeEndElement();
				}
			}
			
			// <dc:title>
			{
				ScrubbingStringBuilder sb = new ScrubbingStringBuilder();
				final String dcTitle = resMap.getLiteralPropValue(resMap.getProvidedCHO_Resource(), NS.DC.PROP_TITLE);
				final String dm2eSubTitle = resMap.getLiteralPropValue(resMap.getProvidedCHO_Resource(), NS.DM2E.PROP_SUBTITLE);
				final String dctermsAlternative = resMap.getLiteralPropValue(resMap.getProvidedCHO_Resource(), NS.DCTERMS.PROP_ALTERNATIVE);
				if (null != dcTitle)
					sb.append(dcTitle);
				if (null != dm2eSubTitle)
					sb.append(" -- ").append(dm2eSubTitle);
				if (null != dctermsAlternative)
					sb.append(" -- ").append(dctermsAlternative);
				if (sb.length() > 0) {
					// <dc:title>
					xml.writeStartElement(NS.DC.BASE, "title");
					xml.writeCharacters(sb.toString());
					// </dc:title>
					xml.writeEndElement();
				}
			}
			
			// <dc:creator>
			for (String value : resMap.getLiteralValues(NS.DC.PROP_CREATOR, null)) {
				// <dc:creator>
				xml.writeStartElement(NS.DC.BASE, "creator");
				xml.writeCharacters(value);
				// </dc:creator>
				xml.writeEndElement();
			}
	
			// <dc:contributor></dc:contributor>
			for (String value : resMap.getLiteralValues(NS.DC.PROP_CONTRIBUTOR, NS.FOAF.CLASS_PERSON)) {
				// <dc:contributor>
				xml.writeStartElement(NS.DC.BASE, "contributor");
				xml.writeCharacters(value);
				// </dc:contributor>
				xml.writeEndElement();
			}
			
			// <dc:date></dc:date>
			for (String theProp : getChoDateProperties()) {
				DateTime dateTime = resMap.getDateTimeForProp(resMap.getProvidedCHO_Resource(), theProp);
				if (null != dateTime) {
					// <dc:date>
					xml.writeStartElement(NS.DC.BASE, "date");
					xml.writeCharacters(oaiDateFormatter.print(dateTime));
					// </dc:date>
					xml.writeEndElement();
				}
			}
			
			// <dc:subject></dc:subject>
			// TODO dm2e:genre
			{
				for (String subject : resMap.getLiteralValues(NS.DC.PROP_SUBJECT, NS.SKOS.CLASS_CONCEPT)) {
					// <dc:subject>
					xml.writeStartElement(NS.DC.BASE, "subject");
					xml.writeCharacters(subject);
					// </dc:subject>
					xml.writeEndElement();
				}
			}
			
			// <dc:language></dc:language>
			{
				// <dc:language>
				xml.writeStartElement(NS.DC.BASE, "language");
				xml.writeCharacters(resMap.getLanguage());
				// </dc:language>
				xml.writeEndElement();
			}
			
			// <dc:description></dc:description>
			{
				// <dc:description>
				for (String desc : resMap.getDescriptions()) {
					xml.writeStartElement(NS.DC.BASE, "description");
					xml.writeCharacters(desc);
					// </dc:description>
					xml.writeEndElement();
				}
			}
			// <dc:coverage></dc:coverage>
			{
				// <dc:coverage>
				for (String place : resMap.getLiteralValues(NS.DC.PROP_COVERAGE, NS.EDM.CLASS_PLACE)) {
					xml.writeStartElement(NS.DC.BASE, "coverage");
					xml.writeCharacters(place);
					// </dc:coverage>
					xml.writeEndElement();
				}
			}
			
			
			// </oai_dc:dc>
			xml.writeEndElement(); 
	
			// </oai:metadata>
			xml.writeEndElement(); 
	
			// oai:about
			{
				xml.writeStartElement("about");
				Resource licenseURI = resMap.getAggregationResource().getPropertyResourceValue(resMap.getModel().createProperty(NS.EDM.PROP_RIGHTS));
				if (null != licenseURI) { 
					log.debug(NS.OAI_RIGHTS.BASE);
					xml.writeStartElement(NS.OAI_RIGHTS.BASE, "rights");
					xml.writeStartElement(NS.OAI_RIGHTS.BASE, "rightsReference");
					xml.writeAttribute("ref", licenseURI.getURI());
					xml.writeEndElement();
					xml.writeEndElement();
				}
				xml.writeEndElement();
			}
			
			// </oai:record>
			xml.writeEndElement();
		}

	public String lastUriSegment(String uri) {
		uri = uri.replaceAll("\\?.*$", "");
		final int lastSlashIndex = uri.lastIndexOf('/') + 1;
		return lastSlashIndex > 0 ? uri.substring(lastSlashIndex)  : "";
	}
}
