package eu.dm2e.linkeddata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;

import eu.dm2e.grafeo.jena.MergingCacheFileManager;
import eu.dm2e.linkeddata.export.BaseXMLExporter;
import eu.dm2e.linkeddata.export.OaiDublinCoreRecord;
import eu.dm2e.linkeddata.model.AbstractDataset;
import eu.dm2e.linkeddata.model.ResourceMap;
import eu.dm2e.linkeddata.model.VersionedDataset;

public class Dm2eOaiCLI {
	
	private static final Logger log = LoggerFactory.getLogger(Dm2eOaiCLI.class);
	private static final Dm2eApiClient api = new Dm2eApiClient(Config.API_BASE, new MergingCacheFileManager());

	public static void main(String[] args) {
		
		String outFileName = "/tmp/DM2E_OAI.xml";
		final File outFile = new File(outFileName);
		FileOutputStream outFileStream = null;
		XMLStreamWriter xmlWriter = null;
		try {
			outFileStream = new FileOutputStream(outFile);
			xmlWriter = BaseXMLExporter.getIndentingXMLStreamWriter(outFileStream);
			if (null == xmlWriter) {
				throw new XMLStreamException("xmlWriter is still null this is weird and bad.");
			}
		} catch (FileNotFoundException | XMLStreamException e) {
			dieHelpfully(e);
		}

		List<ResourceMap> resourceMapList = getResourceMapList();
		OaiDublinCoreRecord recordExporter = new OaiDublinCoreRecord();
		int i = 0;
		for (ResourceMap resourceMap : resourceMapList) {
			String progressString = String.format("[%5d/%5d]", i++, resourceMapList.size());
			try {
				System.out.print(progressString);
				System.out.print(" ");
				System.out.print(resourceMap.getResourceMapUri());
				System.out.print(" - ");
				resourceMap.read();
				recordExporter.writeResourceMapToXML(resourceMap, xmlWriter);
				System.out.println("DONE");
			} catch (XMLStreamException e) {
				System.out.println("FAIL");
				dieHelpfully(e);
			}
		}

		try {
			xmlWriter.close();
			outFileStream.close();
		} catch (XMLStreamException | IOException e) {
			dieHelpfully(e);
		}

		
	}

	private static List<ResourceMap> getResourceMapList() {
		Set<VersionedDataset> datasets = new HashSet<VersionedDataset>();
		log.debug("*****************");
		log.debug("* Read Datasets *");
		log.debug("*****************");
		for (AbstractDataset aSet : api.listAbstractDatasets()) {
			long t0 = System.nanoTime();
			
			// TODO HACK NOTE TODO JUST FOR TESTING !!!!!!!!!
			if (aSet.getCollectionUri().contains("dingler") || aSet.getCollectionUri().contains("abo")) {
				continue;
			}
			// TODO HACK NOTE TODO JUST FOR TESTING !!!!!!!!!

			log.debug("Adding latest VersionedDataset in AbstractDataset {}", aSet.getCollectionUri());
			final VersionedDataset latestVersion = api.createVersionedDataset(aSet.getLatestVersion());
			if (null != latestVersion) {
				datasets.add(latestVersion);
			}
			log.debug("Adding latest VersionedDataset in AbstractDataset {} took {} ms", aSet.getCollectionUri(), (System.nanoTime() - t0) / 1_000_000);
		}

		List<ResourceMap> resourceMaps = new ArrayList<ResourceMap>();
		for (VersionedDataset versionedDataset : datasets) {
			long t0 = System.nanoTime();
			ParameterizedSparqlString pss = new ParameterizedSparqlString();
			pss.append("CONSTRUCT { ?s ?p ?o } WHERE { GRAPH ?g { ?s ?p ?o } }");
			pss.setParam("g", versionedDataset.getVersionedDatasetResource());
			System.out.print(pss.toString());
			Query query = QueryFactory.create(pss.toString());
			QueryExecution exec = QueryExecutionFactory.createServiceRequest(Config.ENDPOINT_SELECT, query);
			exec.execConstruct(versionedDataset.getModel());
			for (ResourceMap resourceMap : versionedDataset.listResourceMaps()) {
				resourceMaps.add(resourceMap);
			}
			System.out.print(" took ");
			System.out.print((System.nanoTime() - t0)/1_000_000);
			System.out.println(" ms");
		}

//		log.debug("**********************");
//		log.debug("* Read Resource Maps *");
//		log.debug("**********************");
//		for (VersionedDataset versionedDataset : datasets) {
//			log.debug("Retrieving VersionedDataset {}", versionedDataset.getVersionedDatasetUri());
////			VersionedDataset versionedDataset = api.createVersionedDataset(dummyDs);	// so we can cache
//			log.debug("Retrieved dataset " + versionedDataset.getVersionedDatasetUri());
//		}
		return resourceMaps;
	}

	private static void dieHelpfully(Exception e) {
		System.out.println("!!! ERROR !!!");
		System.out.println(e);
		e.printStackTrace();
		System.exit(1);
	}
}
