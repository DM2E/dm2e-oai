package eu.dm2e.linkeddata;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Resources;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import eu.dm2e.linkeddata.model.Collection;
import eu.dm2e.linkeddata.model.ResourceMap;
import eu.dm2e.linkeddata.model.VersionedDataset;




public class Dm2eApiClientTest {
	
	final String apiBase = "http://lelystad.informatik.uni-mannheim.de:3000/direct";
	Dm2eApiClient api;
	Collection randomCollection;
	Logger log = LoggerFactory.getLogger(getClass().getName());
	XMLOutputter xmlOutput = new XMLOutputter();
	
	private ResourceMap createSampleResourceMap() throws IOException {
		URL dinglerURL = Resources.getResource("dingler_example.ttl");
		Model m = ModelFactory.createDefaultModel();
		m.read(dinglerURL.openStream(), "", "TURTLE");
		log.debug("Statements read: " + m.size());
		return new ResourceMap(apiBase, m, "uber", "dingler", "issue/pj001", "0");
	}
	
	private void setRandomDatasetId() {
		Set<Collection> colSet = api.listCollections();
		List<Collection> colList = new ArrayList<>(colSet);
		Collections.shuffle(colList);
		randomCollection = colList.get(0);
	}
	
	public Dm2eApiClientTest() {
		api = new Dm2eApiClient(apiBase);
		xmlOutput.setFormat(Format.getPrettyFormat());
	}
	
	@Test
	public void testListDatasets() {
		Set<Collection> set = api.listCollections();
		assertThat(set.size(), greaterThan(0));
	}
	
	@Test 
	public void testListVersions() { 
		setRandomDatasetId();
		Set<String> set = randomCollection.listVersionIds();
		assertThat(set.size(), greaterThan(0));
		log.info("Versions: " + set);
		String newestVersionId = randomCollection.getLatestVersionId();
		log.info("Latest Version: " + newestVersionId);
		assertNotNull(newestVersionId);
		assertThat(set, hasItem(newestVersionId));
	}
	
	@Test
	public void testDataset() {
		setRandomDatasetId();
		String latestVersionId = randomCollection.getLatestVersionId();
		VersionedDataset ds1 = randomCollection.getLatestVersion();
		VersionedDataset ds2 = randomCollection.getVersion(latestVersionId);
		assertEquals(ds1.getCollectionId(), ds2.getCollectionId());
		assertEquals(ds1.getVersionId(), ds2.getVersionId());
		assertEquals(ds1.getModel().size(), ds2.getModel().size());
	}
	
	@Test
	public void testListResourceMaps() {
		setRandomDatasetId();
		VersionedDataset ds1 = randomCollection.getLatestVersion();
		Set<ResourceMap> set = ds1.listResourceMaps();
		log.debug("Number of ResourceMaps: " + set.size());
		assertThat(set.size(), greaterThan(0));
	}
	
	@Test
	public void testGetResourceMap() {
		setRandomDatasetId();
		VersionedDataset ds1 = randomCollection.getLatestVersion();
		Set<ResourceMap> set = ds1.listResourceMaps();
		ResourceMap resMap = new ArrayList<ResourceMap>(set).get(0);
		log.debug("ResourceMap aggregation " + resMap.getAggregationUri());
		log.debug("ResourceMap size " + resMap.getModel().size());
	}

	@Test
	public void testResourceMapToOaiRecord_oai_dc() throws Exception {
		ResourceMap resMap = createSampleResourceMap();
		log.debug(resMap.getItemId());
		Document el = api.resourceMapToOaiRecord(resMap, "oai_dc");
		StringWriter strwriter = new StringWriter();
		xmlOutput.output(el, strwriter);
		log.debug(strwriter.toString());
	}

	@Test
	public void testResourceMapModel() {
		{
			log.debug("Test fromUri");
			final String testUri1 = "http://lelystad.informatik.uni-mannheim.de:3000/direct/item/bbaw/dta/20863/1386762086592";
			ResourceMap rm1 = new ResourceMap(apiBase, testUri1, false);
			assertEquals(rm1.getProvidedCHO_Uri(), testUri1);
			final String testUri2 = "http://lelystad.informatik.uni-mannheim.de:3000/direct/item/bbaw/dta/20863/foo/bar/1386762086592";
			ResourceMap rm2 = new ResourceMap(apiBase, testUri2, false);
			assertEquals(rm2.getProvidedCHO_Uri(), testUri2);
		}
	}
	
	@Test
	public void testCaching() {
		// TODO
	}
}