package eu.dm2e.oai.protocol;

/**
 * OAI-PMH error codes
 */
public enum OaiError {
	badArgument,
	badResumptionToken,
	badVerb,
	cannotDisseminateFormat,
	idDoesNotExist,
	noRecordsMatch,
	noMetadataFormats,
	noSetHierarchy
}