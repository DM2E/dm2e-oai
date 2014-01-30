package eu.dm2e.jena;

import java.util.Map;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;

public class MapdbFileManager extends FileManager {
	
	private DB	mapdb;
	private Map<String,Model> cache;

	public MapdbFileManager() {
		this.mapdb = DBMaker.newTempFileDB().make();
		this.cache = mapdb.createHashMap("MapdbFileManager-Cache")
				.valueSerializer(new JenaModelMapdbSerializer())
				.make();
	}
	
	@Override
	public boolean isCachingModels() { return true; }
	
	@Override
	public void addCacheModel(String uri, Model m) {
		this.cache.put(uri, m);
	}

	@Override
	public void resetCache() {
		this.cache.clear();
	}

	@Override
	public boolean getCachingModels() {
		return isCachingModels();
	}

	@Override
	public Model getFromCache(String filenameOrURI) {
		return cache.get(filenameOrURI);
	}

	@Override
	public boolean hasCachedModel(String filenameOrURI) {
		return cache.containsKey(filenameOrURI);
	}

	@Override
	public void removeCacheModel(String uri) {
		cache.remove(uri);
	}

}
