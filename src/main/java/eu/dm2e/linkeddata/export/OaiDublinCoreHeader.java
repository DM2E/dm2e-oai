package eu.dm2e.linkeddata.export;

import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.dm2e.linkeddata.model.ResourceMap;

public class OaiDublinCoreHeader extends BaseXMLExporter {
	
	private static final Logger log = LoggerFactory.getLogger(OaiDublinCoreHeader.class);

	public void writeResourceMapToXML(ResourceMap resMap, XMLStreamWriter xml) throws XMLStreamException {
		xml.writeStartElement("header");
		String id = String.format("oai:dm2e:%s:%s:%s",
				resMap.getProviderId(),
				resMap.getCollectionId(),
				resMap.getItemId()
				);
		xml.writeStartElement("identifier");
		xml.writeCharacters(id);
		xml.writeEndElement();
		List<String> theProps = getAggregationDateProperties();
		for (String theProp : theProps) {
			DateTime datetimeParsed = resMap.getDateTimeForProp(resMap.getAggregationResource(), theProp);
			log.debug("" + datetimeParsed);
			if (null != datetimeParsed) {
				xml.writeStartElement("datestamp");
				xml.writeCharacters(oaiDateFormatter.print(datetimeParsed));
				xml.writeEndElement();
			}
		}
	
		// <setSpec> Provider
		xml.writeStartElement("setSpec");
		xml.writeCharacters(String.format("provider:%s", resMap.getProviderId()));
		xml.writeEndElement();
	
		// <setSpec> Collection
		xml.writeStartElement("setSpec");
		xml.writeCharacters(String.format("collection:%s:%s", resMap.getProviderId(), resMap.getCollectionId()));
		xml.writeEndElement();
		
		// </oai:header>
		xml.writeEndElement();
	}

}
