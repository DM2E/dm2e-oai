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

import eu.dm2e.linkeddata.model.Dataset;
import eu.dm2e.linkeddata.model.ResourceMap;




public class Dm2eApiClientTest {
	
	Dm2eApiClient api;
	String randomDatasetId;
	Logger log = LoggerFactory.getLogger(getClass().getName());
	XMLOutputter xmlOutput = new XMLOutputter();
	
	private ResourceMap createSampleResourceMap() throws IOException {
		URL dinglerURL = Resources.getResource("dingler_example.ttl");
		Model m = ModelFactory.createDefaultModel();
		m.read(dinglerURL.openStream(), "", "TURTLE");
		log.debug("Statements read: " + m.size());
		String cho = "http://data.dm2e.eu/data/item/uber/dingler/issue/pj001";
		String agg = "http://data.dm2e.eu/data/aggregation/uber/dingler/issue/pj001";
		return new ResourceMap(m, m.createResource(cho), m.createResource(agg), "uber/dingler", "issue/pj001");
	}
	
	private void setRandomDatasetId() {
		Set<String> colSet = api.listDatasets();
		List<String> colList = new ArrayList<>(colSet);
		Collections.shuffle(colList);
		randomDatasetId = colList.get(0);
	}
	
	public Dm2eApiClientTest() {
		api = new Dm2eApiClient("http://lelystad.informatik.uni-mannheim.de:3000/direct");
		xmlOutput.setFormat(Format.getPrettyFormat());
	}
	
	@Test
	public void testListDatasets() {
		Set<String> set = api.listDatasets();
		assertThat(set.size(), greaterThan(0));
	}
	
	@Test 
	public void testListVersions() { 
		setRandomDatasetId();
		Set<String> set = api.listVersions(randomDatasetId);
		assertThat(set.size(), greaterThan(0));
		log.info("Versions: " + set);
		String newestVersionId = api.findLatestVersion(randomDatasetId);
		log.info("Latest Version: " + api.findLatestVersion(randomDatasetId));
		assertNotNull(newestVersionId);
		assertThat(set, hasItem(newestVersionId));
	}
	
	@Test
	public void testDataset() {
		setRandomDatasetId();
		String latestVersionId = api.findLatestVersion(randomDatasetId);
		Dataset ds1 = api.getDataset(randomDatasetId);
		Dataset ds2 = api.getDataset(randomDatasetId, latestVersionId);
		assertEquals(ds1.getDatasetId(), ds2.getDatasetId());
		assertEquals(ds1.getVersionId(), ds2.getVersionId());
		assertEquals(ds1.getModel().size(), ds2.getModel().size());
	}
	
	@Test
	public void testListResourceMaps() {
		setRandomDatasetId();
		Dataset ds1 = api.getDataset(randomDatasetId);
		Set<String> set = api.listResourceMaps(ds1);
		log.debug("Number of ResourceMaps: " + set.size());
		assertThat(set.size(), greaterThan(0));
	}
	
	@Test
	public void testGetResourceMap() {
		setRandomDatasetId();
		Dataset ds1 = api.getDataset(randomDatasetId);
		Set<String> set = api.listResourceMaps(ds1);
		String rndResourceMapId = new ArrayList<>(set).get(0);
		ResourceMap resMap = api.getResourceMap(ds1, rndResourceMapId);
		log.debug("ResourceMap aggregation " + resMap.getAggregation());
		log.debug("ResourceMap size " + resMap.getModel().size());
	}

	@Test
	public void testResourceMapToOaiRecord_oai_dc() throws Exception {
		ResourceMap resMap = createSampleResourceMap();
		log.debug(resMap.getResourceMapId());
		Document el = api.resourceMapToOaiRecord(resMap, "oai_dc");
		StringWriter strwriter = new StringWriter();
		xmlOutput.output(el, strwriter);
		log.debug(strwriter.toString());
	}

}