package eu.dm2e.linkeddata.model;

import org.apache.jena.riot.RiotNotFoundException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.FileManager;

import eu.dm2e.NS;
import eu.dm2e.linkeddata.util.ScrubbingStringBuilder;

public abstract class BaseModel implements Comparable<BaseModel>{
	
	public enum IdentifierType {
		URL,
		OAI_IDENTIFIER,
		OAI_SET_SPEC
	}

	protected static final Logger log = LoggerFactory.getLogger(BaseModel.class);
	protected Model	model;
	protected String	apiBase;

	public Model getModel() { return model; }

	public boolean	isRead = false;
	protected FileManager fileManager;
	
	@SuppressWarnings("unused")
	private BaseModel() { }

	public BaseModel(FileManager fm) {
		this.fileManager = fm;
	}
	
	public void read() {
		if (isRead) return;
		if (fileManager.hasCachedModel(getRetrievalUri())){
			this.model = fileManager.getFromCache(getRetrievalUri());
		} else {
			Model retModel = fileManager.readModel(model, getRetrievalUri());
			if (null != retModel) {
				this.model = retModel;
			}
			fileManager.addCacheModel(getRetrievalUri(), this.model);
		}
		isRead = true;
	}

//	public void read() { read(null); }

	abstract public String getRetrievalUri();

	@Override
	public int compareTo(BaseModel o) {
		return getRetrievalUri().compareTo(o.getRetrievalUri());
	}

	public String getLiteralPropValue(final Resource theResource, final String theProp) {
		Statement stmt = theResource.getProperty(getModel().createProperty(theProp));
		ScrubbingStringBuilder ret = new ScrubbingStringBuilder();
		if (null != stmt && stmt.getObject().isLiteral()) {
			ret.append(stmt.getObject().asLiteral().getLexicalForm());
		}
		return ret.length() > 0 ? ret.toString() : null;
	}
	
	public String dereferenceAndGetPrefLabel(final Resource theResource, final String theProp) {
		Statement stmt = theResource.getProperty(getModel().createProperty(theProp));
		ScrubbingStringBuilder ret = new ScrubbingStringBuilder();
		if (null != stmt && stmt.getObject().isURIResource()) {
			// derefrence
			ThingWithPrefLabel thingWithPrefLabel = new ThingWithPrefLabel(fileManager, apiBase, null, stmt.getObject().asResource().getURI());
			try {
				thingWithPrefLabel.read();
			} catch (RiotNotFoundException e) {
				log.error("Undereferencable thing '{}', skipping", thingWithPrefLabel.getRetrievalUri());
			}
			ret.append(thingWithPrefLabel.getPrefLabel());
		}
		return ret.length() > 0 ? ret.toString() : null;
	}
	
	public DateTime getDateTimeForProp(final Resource theResource, final String theProp) {
		Statement stmt = theResource.getProperty(getModel().createProperty(theProp));
		if (null == stmt)
			return null;

		RDFNode obj = stmt.getObject();
		String toParse = null;
		if (obj.isLiteral()) {
			toParse = obj.asLiteral().getLexicalForm();
		} else if (obj.isURIResource()) {
			try {
				getModel().read(obj.asResource().getURI());
				Statement beginStmt = obj.asResource().getProperty(getModel().createProperty(NS.EDM.PROP_BEGIN));
				if (null != beginStmt && beginStmt.getObject().isLiteral()) {
					toParse = beginStmt.getObject().asLiteral().getLexicalForm();
				}
			} catch (Exception e) {
				log.error("Error reading " + obj.asResource());
			}
		}
		DateTime ret = null;
		try {
			ret = DateTime.parse(toParse);
		} catch (Exception e) {
			log.debug("Error parsing date '{}'", toParse);
		}
		return ret;
	}
			

}