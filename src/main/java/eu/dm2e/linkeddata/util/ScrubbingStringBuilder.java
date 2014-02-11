package eu.dm2e.linkeddata.util;

public class ScrubbingStringBuilder {
	
	private final StringBuilder delegate = new StringBuilder();

	private String cleanString(String in) {
		return in.replaceAll("[<>&]", "");
	}
	
	public ScrubbingStringBuilder append(Object obj) {
		if (obj != null) {
			final String cleanS = cleanString(obj.toString());
			delegate.append(cleanS);
		}
		return this;
	}
	
	public int length() {
		return delegate.length();
	}
	
	@Override
	public String toString() {
		return delegate.toString();
	}

}
