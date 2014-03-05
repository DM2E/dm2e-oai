package eu.dm2e.oai;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.dm2e.grafeo.jaxrs.GrafeoMessageBodyWriter;

public class Dm2eOaiApplication extends Application {

	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(Dm2eOaiApplication.class);
	
	static {
//		GrafeoImpl.addStaticNamespace("fff", NS.FFF.BASE);
	}
	
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> s = new HashSet<Class<?>>();
        s.add(Dm2eOaiService.class);

        s.add(MultiPartFeature.class);
        s.add(GrafeoMessageBodyWriter.class);
        return s;
    }
}
