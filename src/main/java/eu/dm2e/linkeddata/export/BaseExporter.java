package eu.dm2e.linkeddata.export;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.dm2e.NS;

public abstract class BaseExporter {
	
	private static final Logger log = LoggerFactory.getLogger(BaseExporter.class);

	public static DateTimeFormatter oaiDateFormatter = DateTimeFormat.forPattern("YYYY-MM-dd'T'HH:mm:ss'Z'");

	private static List<String> aggregationDateProperties = new ArrayList<>();
	private static List<String> choContribuorProperties = new ArrayList<>();
	private static List<String> choCreatorProperties = new ArrayList<>();
	private static List<String> choDateProperties = new ArrayList<>();
	static {
		aggregationDateProperties.add(NS.DCTERMS.PROP_CREATED);
	}
	static {
		choContribuorProperties.add(NS.DM2E.PROP_ARTIST);
		choContribuorProperties.add(NS.DC.PROP_CONTRIBUTOR);
		choContribuorProperties.add(NS.BIBO.PROP_EDITOR);
		choContribuorProperties.add(NS.DM2E.PROP_PAINTER);
		choContribuorProperties.add(NS.DM2E.PROP_WRITER);
		choContribuorProperties.add(NS.PRO.PROP_TRANSLATOR);
	}
	static {
		choCreatorProperties.add(NS.DC.PROP_CREATOR);
		choCreatorProperties.add(NS.PRO.PROP_AUTHOR);
		choCreatorProperties.add(NS.DM2E.PROP_WRITER);
	}
	static {
		choDateProperties.add(NS.DCTERMS.PROP_TEMPORAL);
		choDateProperties.add(NS.DCTERMS.PROP_ISSUED);
		choDateProperties.add(NS.DCTERMS.PROP_CREATED);
		choDateProperties.add(NS.EDM.PROP_CURRENT_LOCATION);
	}

	public static List<String> getAggregationDateProperties() {
		return aggregationDateProperties;
	}

	public static List<String> getChoContributorProperties() {
		return choContribuorProperties;
	}

	public static List<String> getChoCreatorProperties() {
		return choCreatorProperties;
	}

	public static List<String> getChoDateProperties() {
		return choDateProperties;
	}
	public static DateTimeFormatter getOaiDateFormatter() {
		return oaiDateFormatter;
	}
	/**
	 * @return current time in the format required by OAI-PMH
	 */
	public static String nowOaiFormatted() {
		return oaiDateFormatter.print(DateTime.now());
	}
	

}
