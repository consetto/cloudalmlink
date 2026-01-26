package com.consetto.adt.cloudalmlink.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for atom link elements from XML responses.
 * Extracted from AdtObjectContext for standalone testing.
 */
public class AtomLinkParser {

	/**
	 * Simple representation of an atom link.
	 */
	public static class AtomLink {
		private final String rel;
		private final String href;

		public AtomLink(String rel, String href) {
			this.rel = rel;
			this.href = href;
		}

		public String getRel() {
			return rel;
		}

		public String getHref() {
			return href;
		}
	}

	/**
	 * Parses atom link elements from an XML response.
	 *
	 * @param xmlResponse The XML response string
	 * @return List of parsed atom links
	 */
	public static List<AtomLink> parseAtomLinks(String xmlResponse) {
		List<AtomLink> links = new ArrayList<>();

		if (xmlResponse == null || xmlResponse.isEmpty()) {
			return links;
		}

		// Pattern to match atom:link or link elements with rel and href attributes
		// Handles both orders: rel before href and href before rel
		Pattern linkPattern = Pattern.compile(
				"<(?:atom:)?link[^>]*\\s(?:rel=[\"']([^\"']*)[\"'][^>]*href=[\"']([^\"']*)[\"']|href=[\"']([^\"']*)[\"'][^>]*rel=[\"']([^\"']*)[\"'])[^>]*/?>",
				Pattern.CASE_INSENSITIVE);

		Matcher matcher = linkPattern.matcher(xmlResponse);
		while (matcher.find()) {
			String rel, href;
			if (matcher.group(1) != null) {
				// rel before href
				rel = matcher.group(1);
				href = matcher.group(2);
			} else {
				// href before rel
				href = matcher.group(3);
				rel = matcher.group(4);
			}

			if (rel != null && href != null) {
				links.add(new AtomLink(rel, href));
			}
		}

		return links;
	}

	/**
	 * Extracts the ADT path from a raw URI string.
	 *
	 * @param rawUri The raw URI string
	 * @return The extracted ADT path, or null if not found
	 */
	public static String extractPathFromRawUri(String rawUri) {
		if (rawUri == null) {
			return null;
		}

		// Format: adt://DEST/sap/bc/adt/...
		int adtIndex = rawUri.indexOf("/sap/bc/adt/");
		if (adtIndex != -1) {
			// Find the end of the path (before any query params)
			int endIndex = rawUri.indexOf("?", adtIndex);
			if (endIndex == -1) {
				endIndex = rawUri.length();
			}
			return rawUri.substring(adtIndex, endIndex);
		}
		return null;
	}
}
