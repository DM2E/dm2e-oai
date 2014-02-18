package eu.dm2e.linkeddata;
import static org.fest.assertions.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Resources;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

import eu.dm2e.linkeddata.model.BaseModel.IdentifierType;
import eu.dm2e.linkeddata.model.Collection;
import eu.dm2e.linkeddata.model.ResourceMap;
import eu.dm2e.linkeddata.model.ThingWithPrefLabel;
import eu.dm2e.linkeddata.model.VersionedDataset;

public class Dm2eApiClientTest {
	
	final String apiBase = Config.API_BASE;
	Dm2eApiClient api;
	Collection randomCollection;
	Logger log = LoggerFactory.getLogger(getClass().getName());
	FileManager testFM;

	public Dm2eApiClientTest() {
		api = new Dm2eApiClient(apiBase);
		testFM = Dm2eApiClient.setupFileManager();
	}
	
	private ResourceMap createSampleResourceMap(String testTurtle) throws IOException {
		Model m = ModelFactory.createDefaultModel();
		URL dinglerURL = Resources.getResource(testTurtle);
		final InputStream openStream = dinglerURL.openStream();
		String dinglerStr = IOUtils.toString(openStream);
//		dinglerStr = dinglerStr.replaceAll("http://data.dm2e.eu/data", Config.API_BASE);
		m.read(new StringReader(dinglerStr), "", "TURTLE");
		log.debug("Statements read: " + m.size());
		final ResourceMap resourceMap = new ResourceMap(testFM, apiBase, m, "uber", "dingler", "issue/pj001", "0");
		testFM.addCacheModel(resourceMap.getRetrievalUri(), m);
		return resourceMap;
	}
	
	private void setRandomDatasetId() {
//		Set<Collection> colSet = api.listCollections();
//		List<Collection> colList = new ArrayList<Collection>(colSet);
//		Collections.shuffle(colList);
//		randomCollection = colList.get(0);
		randomCollection = new Collection(testFM, apiBase, null, "ub-ffm", "sammlungen");
		randomCollection.read();
	}
	
	
	@Test
	public void testListDatasets() {
		Set<Collection> set = api.listCollections();
		assertThat(set.size()).isGreaterThan(0);
	}
	
	@Test 
	public void testListVersions() { 
		setRandomDatasetId();
		Set<String> set = randomCollection.listVersionIds();
		assertThat(set.size()).isGreaterThan(0);
		log.info("Versions: " + set);
		String newestVersionId = randomCollection.getLatestVersionId();
		log.info("Latest Version: " + newestVersionId);
		assertThat(newestVersionId).isNotNull();
		assertThat(set).contains(newestVersionId);
	}
	
	@Test
	public void testDataset() {
		setRandomDatasetId();
		String latestVersionId = randomCollection.getLatestVersionId();
		VersionedDataset ds1 = randomCollection.getLatestVersion();
		VersionedDataset ds2 = randomCollection.getVersion(latestVersionId);
		assertThat(ds1.getCollectionId()).isEqualTo(ds2.getCollectionId());
		assertThat(ds1.getVersionId()).isEqualTo(ds2.getVersionId());
		assertThat(ds1.getModel().size()).isEqualTo(ds2.getModel().size());
	}
	
	@Test
	public void testListResourceMaps() {
		setRandomDatasetId();
		VersionedDataset ds1 = randomCollection.getLatestVersion();
		ds1.read();
		log.debug(ds1.getVersionedDatasetUri());
		Set<ResourceMap> set = ds1.listResourceMaps();
		log.debug("Number of ResourceMaps: " + set.size());
		StringWriter sw = new StringWriter();
		ds1.getModel().write(sw);
		log.debug(sw.toString());
		assertThat(set.size()).isGreaterThan(0);
	}
	
	@Test
	public void testGetResourceMap() {
		setRandomDatasetId();
		VersionedDataset ds1 = randomCollection.getLatestVersion();
		ds1.read();
		log.debug(ds1.getCollectionId());
		log.debug(ds1.getVersionId());
		Set<ResourceMap> set = ds1.listResourceMaps();
		StringWriter sw = new StringWriter();
		ds1.getModel().write(sw);
		log.debug(sw.toString());

		ResourceMap resMap = new ArrayList<ResourceMap>(set).get(0);
		log.debug("ResourceMap aggregation " + resMap.getAggregationUri());
		log.debug("ResourceMap size " + resMap.getModel().size());
	}
	
	@Test
	public void testThumbnail() throws IOException {
		ResourceMap resMap = createSampleResourceMap("dingler_example.ttl");
		resMap.getThumbnailLink();
	}
	@Test
	public void testFirstPageLink() throws IOException {
		ResourceMap resMap = createSampleResourceMap("dingler_example.ttl");
		resMap.getFirstPageLink();
	}

	@Test
	public void testResourceMapToOaiRecord_oai_dc() throws Exception {
		ResourceMap resMap = createSampleResourceMap("dingler_example.ttl");
		StringWriter stringWriter = new StringWriter();
		XMLStreamWriter xml = Dm2eApiClient.getIndentingXMLStreamWriter(stringWriter);
		resMap.getThumbnailLink();
		log.debug(resMap.getItemId());
		log.debug(resMap.getProvidedCHO_Uri());
		log.debug("CHO Res: " + resMap.getProvidedCHO_Resource());
		log.debug(resMap.getProvidedCHO_Uri());
		api.resourceMapToOaiRecord(resMap, "oai_dc", xml);
		xml.close();
		stringWriter.close();
		log.debug(stringWriter.toString());
	}

	@Test
	public void testResourceMapModel() {
		{
			log.debug("Test fromUri");
			final String testUri1 = Config.API_BASE + "/item/bbaw/dta/20863/1386762086592";
			ResourceMap rm1 = new ResourceMap(testFM, apiBase, testUri1, IdentifierType.URL, "1386762086592");
			assertThat(rm1.getProvidedCHO_Uri()).isEqualTo(testUri1.replaceFirst("/1386762086592", ""));
			final String testUri2 = Config.API_BASE + "/item/bbaw/dta/20863/foo/bar/1386762086592";
			ResourceMap rm2 = new ResourceMap(testFM, apiBase, testUri2, IdentifierType.URL, "1386762086592");
			assertThat(rm2.getProvidedCHO_Uri()).isEqualTo(testUri2.replaceFirst("/1386762086592", ""));
		}
	}
	
	@Test
//	@Ignore("FIXME")
	public void testCaching() {
		// TODO
		final String uri = Config.API_BASE + "/place/bbaw/dta/Berlin";
		long withCaching, withoutCaching;
		{
			long t0 = System.currentTimeMillis();
			ThingWithPrefLabel it = new ThingWithPrefLabel(testFM, Config.API_BASE, null, uri);
			it.read();
			assertThat("Berlin").isEqualTo(it.getPrefLabel());
			withoutCaching = System.currentTimeMillis() - t0;
			log.debug("withoutCaching: " + withoutCaching);
		}
		{
			long t0 = System.currentTimeMillis();
			ThingWithPrefLabel it = new ThingWithPrefLabel(testFM, Config.API_BASE, null, uri);
			it.read();
			assertThat("Berlin").isEqualTo(it.getPrefLabel());
			withCaching = System.currentTimeMillis() - t0;
			log.debug("withCaching: " + withCaching);
		}
		assertThat(withCaching).isLessThan(withoutCaching);
	}
}