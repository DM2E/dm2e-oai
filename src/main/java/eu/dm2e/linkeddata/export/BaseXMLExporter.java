package eu.dm2e.linkeddata.export;

import java.io.OutputStream;
import java.io.Writer;

import javanet.staxutils.IndentingXMLStreamWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import eu.dm2e.NS;
import eu.dm2e.linkeddata.model.ResourceMap;

public abstract class BaseXMLExporter extends BaseExporter {

	private static final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
	public static XMLOutputFactory getXMLOutputFactory() {
		return xmlOutputFactory;
	}
	public static XMLStreamWriter getIndentingXMLStreamWriter(Writer writer) throws XMLStreamException {
		return new IndentingXMLStreamWriter(getXMLStreamWriter(writer));
	}
	public static XMLStreamWriter getIndentingXMLStreamWriter(OutputStream out) throws XMLStreamException {
		return new IndentingXMLStreamWriter(getXMLStreamWriter(out));
	}
	public static XMLStreamWriter getXMLStreamWriter(OutputStream out) throws XMLStreamException {
		final XMLStreamWriter xml = getXMLOutputFactory().createXMLStreamWriter(out);
		setNamespaces(xml);
		return xml;
	}
	public static XMLStreamWriter getXMLStreamWriter(Writer writer) throws XMLStreamException {
		final XMLStreamWriter xml = getXMLOutputFactory().createXMLStreamWriter(writer);
		setNamespaces(xml);
		return xml;
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
	 * Write out the record to the provided streaming XML writer.
	 * @param resourceMap the {@link ResourceMap} to convert
	 * @param xml the {@link XMLStreamWriter} to write to
	 * @throws XMLStreamException
	 */
	public abstract void writeResourceMapToXML(ResourceMap resourceMap, XMLStreamWriter xml) throws XMLStreamException;

}
