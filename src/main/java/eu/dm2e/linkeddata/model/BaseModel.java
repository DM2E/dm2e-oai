package eu.dm2e.linkeddata.model;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.atlas.web.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;

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

	public boolean	isRead;

	public BaseModel() {
		super();
	}

	public void read(Cache cache) {
		log.debug("List of cached objects: ");
		for (Object x : cache.getKeys()) {
			log.debug("  * " + x);
		}
		if (isRead) {
			log.debug("Already read, return right away.");
			return;
		}
		final String uri = getRetrievalUri();
		if (cache != null) {
			Element cachedObj = cache.get(uri);
			if (cachedObj != null) {
				log.debug("Found in cache: '" + uri + "'.");
				model.read(new StringReader((String) cachedObj.getObjectValue()), "", "RDF/XML");
				log.debug("Return from read(cache) right away");
				isRead = true;
				return;
			} else {
				log.debug("NOT Found in cache: '" + uri + "'.");
				isRead = false;
			}
		}
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet get = new HttpGet(uri);
		get.setHeader("Accept", "application/rdf+xml");
		CloseableHttpResponse resp;
		try {
			long t0 = System.currentTimeMillis();
			resp = httpClient.execute(get);
			if (resp.getStatusLine().getStatusCode() >= 400) {
				throw new HttpException("HTTP Error: " + resp.getStatusLine() );
			}
			assert(resp.getEntity() != null);
			InputStream is = resp.getEntity().getContent();
			StringWriter sw = new StringWriter();
			IOUtils.copy(is, sw, "UTF-8");
			String modelSerialized = sw.toString();
			long t1 = System.currentTimeMillis();
			model.read(new StringReader(modelSerialized), "", "RDF/XML");
			log.debug(String.format("Reading the VersionedDataset '%s' took %sms", uri, (t1-t0)));
			if (cache != null) {
				log.debug("Caching response under '" + uri + "'.");
				cache.put(new Element(uri, modelSerialized));
			}
			isRead = true;
		} catch (Exception e) {
			log.debug("Error retrieving '" + uri + "': ", e);
			isRead = false;
		}
	}

	public void read() { read(null); }

	abstract public String getRetrievalUri();

	@Override
	public int compareTo(BaseModel o) {
		return getRetrievalUri().compareTo(o.getRetrievalUri());
	}

}