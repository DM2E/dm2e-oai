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
			
			// <dc:identifier linktype="thumbnail">
			{
				xml.writeStartElement(NS.DC.BASE, "identifier");
				xml.writeAttribute("linktype", "thumbnail");
				String thumbnailLink = resMap.getThumbnailLink();
				xml.writeCharacters(String.format(Config.PUNDIT_FMT_STRING, thumbnailLink));
				xml.writeEndElement();
			}
	
			// PunditLink
			// http://demo.feed.thepund.it/?dm2e=http://lelystad.informatik.uni-mannheim.de:3000/direct/item/bbaw/dta/16821/f0001&conf=timeline-demo.js
			// <dc:identifier linktype="annotate">
			{
				String firstPageLink = resMap.getFirstPageLink();
				if (null != firstPageLink) {
					xml.writeStartElement(NS.DC.BASE, "identifier");
					xml.writeAttribute("linktype", "annotate");
					xml.writeCharacters(String.format(Config.PUNDIT_FMT_STRING, firstPageLink));
					xml.writeEndElement();
				}
			}
			
			// <edm:dataProvider>
			{
				xml.writeStartElement(NS.EDM.BASE, "dataProvider");
				xml.writeCharacters(resMap.getProviderId());
				xml.writeEndElement();
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
					xml.writeStartElement(NS.DC.BASE, "title");
					xml.writeCharacters(sb.toString());
					xml.writeEndElement();
				}
			}
			
			// <dc:creator>
			for (String theProp : getChoCreatorProperties()) {
				log.debug(resMap.getProvidedCHO_Uri());
				log.debug(theProp);
				final String prefLabel = resMap.dereferenceAndGetPrefLabel(resMap.getProvidedCHO_Resource(), theProp);
				if (null == prefLabel)
					continue;
				xml.writeStartElement(NS.DC.BASE, "creator");
				xml.writeCharacters(prefLabel);
				xml.writeEndElement();
			}
	
			// <dc:contributor>
			for (String theProp : getChoContributorProperties()) {
				log.debug(resMap.getProvidedCHO_Uri());
				log.debug(theProp);
				final String prefLabel = resMap.dereferenceAndGetPrefLabel(resMap.getProvidedCHO_Resource(), theProp);
				if (null == prefLabel)
					continue;
				xml.writeStartElement(NS.DC.BASE, "creator");
				xml.writeCharacters(prefLabel);
				xml.writeEndElement();
			}
			
			// <dc:date>
			for (String theProp : getChoDateProperties()) {
				DateTime dateTime = resMap.getDateTimeForProp(resMap.getProvidedCHO_Resource(), theProp);
				if (null != dateTime) {
					xml.writeStartElement(NS.DC.BASE, "date");
					xml.writeCharacters(oaiDateFormatter.print(dateTime));
					xml.writeEndElement();
				}
			}
			
			// <dc:subject>
		
			
	//
	//
	////		log.debug(dumpModel(resMap.getModel()));
	//		StmtIterator choIter = resMap.getModel().listStatements(resMap.getProvidedCHO_Resource(), null, (RDFNode)null);
	//		log.debug("CHO Statements???? " + choIter.hasNext());
	//		while (choIter.hasNext()) {
	//			Statement stmt = choIter.next();
	//			Property pred = stmt.getPredicate();
	//			RDFNode obj = stmt.getObject();
	//
	//			Namespace thisElemNs;
	//			Element el = null;
	//			final String predUrl = pred.getURI().toString();
	//			boolean addGenericElement = false;
	//			if (NS.RDF.PROP_TYPE.equals(predUrl)) {
	//				String objUri = obj.asResource().getURI();
	//				if (NS.EDM.CLASS_PROVIDED_CHO.equals(objUri)) continue;
	//				el = new Element("type", jdomNS.get("rdf"));
	//				el.setAttribute(new Attribute("resource", obj.asResource().getURI(), jdomNS.get("rdf")));
	//				Element el2 = new Element("type", jdomNS.get("dc"));
	//				el2.addContent(obj.asResource().getURI());
	//				oaiDcDc.addContent(el2);
	//			} else if (NS.DC.PROP_TYPE.equals(predUrl)) {
	//				if (obj.isResource()) {
	//					el = new Element("type", jdomNS.get("rdf"));
	//					el.setAttribute(new Attribute("resource", obj.asResource().getURI(), jdomNS.get("rdf")));
	//				}
	//				addGenericElement = true;
	//			} else if (NS.DCTERMS.PROP_TITLE.equals(predUrl) || NS.DM2E.PROP_SUBTITLE.equals(predUrl)) {
	//				// NOTE don't take the title but create a title from all the subtitles above #create-title
	//			} else if (NS.DC.PROP_PUBLISHER.equals(predUrl)) {
	//				el = new Element("publisher", jdomNS.get("dc"));
	//				addContentOrPrefLabelToElement(obj, el);
	//			} else if (NS.DM2E.PROP_PRINTED_AT.equals(predUrl)) {
	//				el = new Element("coverage", jdomNS.get("dc"));
	//				addContentOrPrefLabelToElement(obj, el);
	//			} else if (NS.PRO.PROP_AUTHOR.equals(predUrl)) {
	//				el = new Element("creator", jdomNS.get("dc"));
	//				addContentOrPrefLabelToElement(obj, el);
	//			} else if (NS.BIBO.PROP_EDITOR.equals(predUrl)) {
	//				el = new Element("creator", jdomNS.get("dc"));
	//				addContentOrPrefLabelToElement(obj, el);
	//			} else if (NS.DC.PROP_SUBJECT.equals(predUrl)) {
	//				el = new Element("subject", jdomNS.get("dc"));
	//				addContentOrPrefLabelToElement(obj, el);
	//			} else if (NS.DCTERMS.PROP_ISSUED.equals(predUrl)) {
	//				el = new Element("date", jdomNS.get("dc"));
	//				if (obj.isLiteral())
	//					el.addContent(obj.asLiteral().toString());
	//				else {
	//					// TODO handle timespans
	//				}
	//				addGenericElement = true;
	//			} else {
	//				addGenericElement = true;
	//			}
	//			if (null != el && el.getContentSize() > 0 && el.getContent(0).getValue().length() > 0)  {
	//				oaiDcDc.addContent(el);
	//			}
	//
	//			if (addGenericElement) {
	////				 generic mapping
	////				log.warn("Unhandled Predicate: " + pred);
	////				continue;
	//				thisElemNs = jdomNS.get(pred.getNameSpace());
	//				if (null == thisElemNs) {
	//					log.warn("Unknown namespace: " + pred.getNameSpace());
	//				}
	//				Element genericElem = new Element(pred.getLocalName(), thisElemNs);
	//				if (obj.isLiteral()) {
	//					genericElem.setText(obj.asLiteral().getValue().toString());
	//				} else {
	//					genericElem.setText(obj.asResource().getURI());
	//				}
	//				if (null != genericElem) oaiDcDc.addContent(genericElem);
	//			}
	//		}
	//
	//		StmtIterator aggIter = resMap.getModel().listStatements(resMap.getAggregationResource(), null, (RDFNode)null);
	//		while (aggIter.hasNext()) {
	//			Statement stmt = aggIter.next();
	//			Property pred = stmt.getPredicate();
	//			RDFNode obj = stmt.getObject();
	//			// manual mapping
	//			Element el = null;
	//			final String predUrl = pred.getURI().toString();
	//			if (NS.EDM.PROP_IS_SHOWN_BY.equals(predUrl)) {
	//				el = new Element("identifier", jdomNS.get("dc"));
	//				el.setText(obj.asResource().getURI());
	//				el.setAttribute("linktype", "fulltext");
	////			} else if (NS.EDM.PROP_IS_SHOWN_AT.equals(predUrl)) {
	////				el = new Element("identifier", jdomNS.get("dc"));
	////				el.setText(obj.asResource().getURI());
	////				el.setAttribute("linktype", "thumbnail");
	////			} else if (NS.EDM.PROP_OBJECT.equals(predUrl)) {
	////				el = new Element("identifier", jdomNS.get("dc"));
	////				el.setText(obj.asResource().getURI());
	////				el.setAttribute("linktype", "thumbnail");
	////			} else if (NS.DM2E.PROP_HAS_ANNOTABLE_VERSION_AT.equals(predUrl)) {
	////				el = new Element("identifier", jdomNS.get("dc"));
	////				el.setText(obj.asResource().getURI());
	////				el.setAttribute("linktype", "thumbnail");
	//			}
	//			if (null != el) oaiDcDc.addContent(el);
	//		}
	
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

}
