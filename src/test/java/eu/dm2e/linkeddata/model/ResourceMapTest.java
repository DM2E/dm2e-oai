package eu.dm2e.linkeddata.model;

import static org.fest.assertions.Assertions.*;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import eu.dm2e.NS;
import eu.dm2e.linkeddata.Config;
import eu.dm2e.linkeddata.Dm2eApiClientTest;


public class ResourceMapTest extends Dm2eApiClientTest {
	
	private static final Logger log = LoggerFactory.getLogger(ResourceMapTest.class);
	private ResourceMap rmBBAW;
	private ResourceMap rmONB;
	
	public ResourceMapTest() throws IOException {
		super();
		{
			InputStream is = getClass().getResource("/bbaw_dta_16157.ttl").openStream();
			Model m = ModelFactory.createDefaultModel();
			m.read(is, "", "TURTLE");
			rmBBAW = new ResourceMap(testFM, Config.API_BASE, m, "bbaw", "dta", "16157", "1391002599515");
		}
		{
			InputStream is = getClass().getResource("/onb_abo_Z101315405_1391106413338.ttl").openStream();
			Model m = ModelFactory.createDefaultModel();
			m.read(is, "", "TURTLE");
			rmONB = new ResourceMap(testFM, Config.API_BASE, m, "onb", "abo", "%2BZ101315405", "1391106413338");
			log.debug("{}", rmONB);
		}
	}

	@Test
	public void testGetThumbnailLink() throws Exception {
		assertThat(rmBBAW.getThumbnailLink()).isEqualTo("http://media.dwds.de/dta/images/strouhal_tonerregung_1878/strouhal_tonerregung_1878_0007_1600px.jpg");
	}

	@Test
	public void testGetFirstPageLink() throws Exception {
		assertThat(rmBBAW.getFirstPageLink()).isEqualTo("http://data.dm2e.eu/data/item/bbaw/dta/16157_f0001");
	}

	@Test
	public void testGetLiteralSubjects() throws Exception {
		assertThat(rmBBAW.getLiteralSubjects(NS.SKOS.CLASS_CONCEPT)).contains("Physik");
	}

	@Test
	public void testGetLanguage() throws Exception {
		assertThat(rmBBAW.getLanguage()).isEqualTo("de");
	}

	@Test
	public void testGetDescriptions() throws Exception {
		assertThat(rmBBAW.getDescriptions()).isNotEmpty();
		assertThat(rmBBAW.getDescriptions().get(0)).contains("Gesetze bei 0");
	}

	@Test
	public void testIsDisplayLevelTrue() throws Exception {
		assertThat(rmBBAW.isDisplayLevelTrue()).isFalse();
		assertThat(rmONB.isDisplayLevelTrue()).isTrue();
	}

}
