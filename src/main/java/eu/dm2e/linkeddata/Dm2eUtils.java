package eu.dm2e.linkeddata;

public class Dm2eUtils {

	public static String lastUriSegment(String uri) {
		uri = uri.replaceAll("\\?.*$", "");
		final int lastSlashIndex = uri.lastIndexOf('/') + 1;
		return lastSlashIndex > 0 ? uri.substring(lastSlashIndex)  : "";
	}

}
