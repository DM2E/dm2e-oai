package eu.dm2e.oai.protocol;

/**
 * Allowed keys for key=value pairs in GET/POST requests
 */
public enum OaiKey {
	verb,
	identifier,
	resumptionToken,
	from,
	until,
	set,
	metadataPrefix,
}