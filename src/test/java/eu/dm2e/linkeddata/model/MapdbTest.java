package eu.dm2e.linkeddata.model;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import com.hp.hpl.jena.rdf.model.ModelFactory;


public class MapdbTest {

//	@Test
//	public void testMapdb() {
//        //init off-heap store with 2GB size limit
//        DB db = DBMaker
//                .newDirectMemoryDB()    //use off-heap memory, on-heap is `.newMemoryDB()`
//                .sizeLimit(2)           //limit store size to 2GB
//                .transactionDisable()   //better performance
//                .make();
//
//        //create map, entries are expired if not accessed (get,iterate) for 10 seconds or 30 seconds after 'put'
//        //There is also maximal size limit to prevent OutOfMemoryException
//        HTreeMap map = db
//                .createHashMap("cache")
//                .expireMaxSize(1000000)
//                .expireAfterWrite(30, TimeUnit.SECONDS)
//                .expireAfterAccess(10, TimeUnit.SECONDS)
//                .make();
//        for(int i = 0;i<100;i++){
//            map.put(i, ModelFactory.createDefaultModel());
//        }
//
//	}
}
