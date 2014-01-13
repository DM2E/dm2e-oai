package eu.dm2e.linkeddata.model;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.dm2e.linkeddata.Config;


public class ThingWithPrefLabelTest {
	
	Logger log = LoggerFactory.getLogger(getClass().getName());
	final String apiBase = Config.API_BASE;

	@Test
	public void testConc() {
		log.debug("from parts");
		{
			ThingWithPrefLabel c = new ThingWithPrefLabel(apiBase, null, "bbaw", "dta", "Belletristik");
			c.read();
			assertThat(c.getPrefLabel(), is("Belletristik"));
		}
		log.debug("from uri");
		{
			ThingWithPrefLabel c = new ThingWithPrefLabel(apiBase, null, apiBase + "/concept/bbaw/dta/Belletristik");
			c.read();
			assertThat(c.getPrefLabel(), is("Belletristik"));
		}
	}
}
