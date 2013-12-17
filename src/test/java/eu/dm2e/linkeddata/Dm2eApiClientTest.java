package eu.dm2e.linkeddata;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.dm2e.linkeddata.model.Dataset;
import eu.dm2e.linkeddata.model.ResourceMap;




public class Dm2eApiClientTest {
	
	Dm2eApiClient api;
	String rndDatasetId;
	Logger log = LoggerFactory.getLogger(getClass().getName());
	
	public Dm2eApiClientTest() {
		api = new Dm2eApiClient("http://lelystad.informatik.uni-mannheim.de:3000/direct");
		Set<String> colSet = api.listDatasets();
		List<String> colList = new ArrayList<>(colSet);
		Collections.shuffle(colList);
		rndDatasetId = colList.get(0);
	}
	
	@Test
	public void testListDatasets() {
		Set<String> set = api.listDatasets();
		assertThat(set.size(), greaterThan(0));
	}
	
	@Test 
	public void testListVersions() { 
		Set<String> set = api.listVersions(rndDatasetId);
		assertThat(set.size(), greaterThan(0));
		log.info("Versions: " + set);
		String newestVersionId = api.findLatestVersion(rndDatasetId);
		log.info("Latest Version: " + api.findLatestVersion(rndDatasetId));
		assertNotNull(newestVersionId);
		assertThat(set, hasItem(newestVersionId));
	}
	
	@Test
	public void testDataset() {
		String latestVersionId = api.findLatestVersion(rndDatasetId);
		Dataset ds1 = api.getDataset(rndDatasetId);
		Dataset ds2 = api.getDataset(rndDatasetId, latestVersionId);
		assertEquals(ds1.getDatasetId(), ds2.getDatasetId());
		assertEquals(ds1.getVersionId(), ds2.getVersionId());
		assertEquals(ds1.getModel().size(), ds2.getModel().size());
	}
	
	@Test
	public void testListResourceMaps() {
		Dataset ds1 = api.getDataset(rndDatasetId);
		Set<String> set = api.listResourceMaps(ds1);
		log.debug("Number of ResourceMaps: " + set.size());
		assertThat(set.size(), greaterThan(0));
	}
	
	@Test
	public void testGetResourceMap() {
		Dataset ds1 = api.getDataset(rndDatasetId);
		Set<String> set = api.listResourceMaps(ds1);
		String rndResourceMapId = new ArrayList<>(set).get(0);
		ResourceMap resMap = api.getResourceMap(ds1, rndResourceMapId);
		log.debug("ResourceMap aggregation " + resMap.getAggregation());
		log.debug("ResourceMap size " + resMap.getModel().size());
	}

}