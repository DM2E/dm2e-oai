package eu.dm2e.linkeddata.model;

import static org.fest.assertions.Assertions.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.util.FileManager;

import eu.dm2e.NS;
import eu.dm2e.linkeddata.Config;
import eu.dm2e.linkeddata.Dm2eApiClient;


public class ThingWithPrefLabelTest {
	
	Logger log = LoggerFactory.getLogger(getClass().getName());
	final String apiBase = Config.API_BASE;
	final FileManager testFM = Dm2eApiClient.setupFileManager();

	@Test
	public void testConc1() {
		log.debug("from parts");
		{
			ThingWithPrefLabel c = new ThingWithPrefLabel(testFM, apiBase, null, "bbaw", "dta", "Belletristik");
			c.read();
			assertThat(c.getPrefLabel(), is("Belletristik"));
		}
		log.debug("from uri");
		{
			ThingWithPrefLabel c = new ThingWithPrefLabel(testFM, apiBase, null, apiBase + "/concept/bbaw/dta/Belletristik");
			c.read();
			assertThat(c.getPrefLabel(), is("Belletristik"));
		}
	}

	@Test
	public void testConc2() {
		log.debug("from parts");
		{
			ThingWithPrefLabel c = new ThingWithPrefLabel(testFM, apiBase, null, "bbaw", "dta", "Berlin");
			c.read();
			assertThat(c.getPrefLabel(), is("Berlin"));
		}
		log.debug("from uri");
		{
			ThingWithPrefLabel c = new ThingWithPrefLabel(testFM, apiBase, null, apiBase + "/concept/bbaw/dta/Berlin");
			c.read();
			assertThat(c.getPrefLabel(), is("Berlin"));
		}
	}

	@Test
	public void testGetRdfType() throws Exception {
		ThingWithPrefLabel c = new ThingWithPrefLabel(testFM, apiBase, null, apiBase + "/concept/bbaw/dta/Berlin");
		c.read();
		assertThat(c.getRdfType()).isEqualTo(NS.SKOS.CLASS_CONCEPT);
	}
}
