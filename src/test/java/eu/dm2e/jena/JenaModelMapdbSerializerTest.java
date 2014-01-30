package eu.dm2e.jena;

import java.io.StringWriter;
import java.util.Map;

import org.junit.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


public class JenaModelMapdbSerializerTest {
	
	Logger log = LoggerFactory.getLogger(getClass().getName());

	@Test
	public void testRoundtrip() {
		Model m0 = ModelFactory.createDefaultModel();
		m0.add(m0.createResource("http://a"), m0.createProperty("http://b"), m0.createProperty("http://c"));

        Serializer<Model> serializer = new JenaModelMapdbSerializer();

        DB db2 = DBMaker.newTempFileDB().make();

        Map<String,Model> map2 = db2.createHashMap("map").valueSerializer(serializer).make();
        map2.put("foo", m0);
        db2.commit();
        Model m1 = map2.get("foo");
        StringWriter sw1 = new StringWriter();
        m1.write(sw1, "N-TRIPLES");
        log.debug(sw1.toString());
	}
}
