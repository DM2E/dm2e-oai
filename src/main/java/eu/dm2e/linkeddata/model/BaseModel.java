package eu.dm2e.linkeddata.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;

public abstract class BaseModel implements Comparable<BaseModel>{
	
	public enum IdentifierType {
		URL,
		OAI_IDENTIFIER,
		OAI_SET_SPEC
	}

	protected Logger	log	= LoggerFactory.getLogger(getClass().getName());
	protected Model	model;
	protected String	apiBase;

	public Model getModel() { return model; }

	public boolean	isRead = false;
	protected FileManager fileManager;
	
	private BaseModel() { }

	public BaseModel(FileManager fm) {
		this.fileManager = fm;
	}
	
	public void read() {
		if (isRead) return;
		if (fileManager.hasCachedModel(getRetrievalUri())){
			model = fileManager.getFromCache(getRetrievalUri());
		} else {
			fileManager.readModel(model, getRetrievalUri());
			fileManager.addCacheModel(getRetrievalUri(), model);
		}
	}

//	public void read() { read(null); }

	abstract public String getRetrievalUri();

	@Override
	public int compareTo(BaseModel o) {
		return getRetrievalUri().compareTo(o.getRetrievalUri());
	}

}