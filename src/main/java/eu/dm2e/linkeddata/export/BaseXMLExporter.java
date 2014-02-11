package eu.dm2e.linkeddata.export;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import eu.dm2e.linkeddata.model.ResourceMap;

public abstract class BaseXMLExporter extends BaseExporter {
	
	/**
	 * Write out the record to the provided streaming XML writer.
	 * @param resourceMap the {@link ResourceMap} to convert
	 * @param xml the {@link XMLStreamWriter} to write to
	 * @throws XMLStreamException
	 */
	public abstract void writeResourceMapToXML(ResourceMap resourceMap, XMLStreamWriter xml) throws XMLStreamException;

}
