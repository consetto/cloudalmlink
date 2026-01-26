package com.consetto.adt.cloudalmlink.handlers;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class containing pattern matching and parsing methods.
 * Extracted from handler classes to enable standalone testing.
 */
public class PatternUtils {

	// Cloud ALM ID pattern: features (6-NNNN), tasks (3-NNNN), documents (7-NNNN), libraries (15-NNNN)
	private static final Pattern CALM_ID_PATTERN = Pattern.compile("(?:3|6|7|15)-\\d+");

	// Feature pattern for description parsing
	private static final Pattern FEATURE_PATTERN = Pattern.compile("^6-\\d+$");

	/**
	 * Extracts transport ID from transport response using multiple regex patterns.
	 *
	 * @param transportResponse The raw transport response
	 * @return The transport ID if found, null otherwise
	 */
	public static String extractTransportId(String transportResponse) {
		if (transportResponse == null) {
			return null;
		}

		// Pattern 1: Look for tm:request attribute
		Pattern pattern1 = Pattern.compile("tm:request=\"([A-Z0-9]+)\"");
		Matcher matcher1 = pattern1.matcher(transportResponse);
		if (matcher1.find()) {
			return matcher1.group(1);
		}

		// Pattern 2: Look for transport request element content
		Pattern pattern2 = Pattern.compile(">([A-Z][A-Z0-9]{2}K\\d{6})<");
		Matcher matcher2 = pattern2.matcher(transportResponse);
		if (matcher2.find()) {
			return matcher2.group(1);
		}

		// Pattern 3: General SAP transport pattern anywhere in response
		Pattern pattern3 = Pattern.compile("[A-Z][A-Z0-9]{2}K\\d{6}");
		Matcher matcher3 = pattern3.matcher(transportResponse);
		if (matcher3.find()) {
			return matcher3.group();
		}

		return null;
	}

	/**
	 * Extracts and decodes the uri= query parameter from a link href.
	 *
	 * @param href The link href containing a uri= parameter
	 * @return The decoded URI value, or null if not found
	 */
	public static String extractUriParameter(String href) {
		if (href == null) {
			return null;
		}

		int uriStart = href.indexOf("uri=");
		if (uriStart == -1) {
			return null;
		}
		uriStart += 4; // Skip "uri="
		int uriEnd = href.indexOf("&", uriStart);
		if (uriEnd == -1) {
			uriEnd = href.length();
		}
		String encodedUri = href.substring(uriStart, uriEnd);
		return URLDecoder.decode(encodedUri, StandardCharsets.UTF_8);
	}

	/**
	 * Extracts the Cloud ALM feature ID from a transport description.
	 * Feature IDs follow the pattern "6-NNNN" (e.g., "6-123").
	 *
	 * @param descr The transport request description
	 * @return The feature ID if found and valid, null otherwise
	 */
	public static String getFeatureFromDescription(String descr) {
		if (descr == null || descr.isEmpty()) {
			return null;
		}

		String[] descrSplit = descr.split(":");
		String feature = descrSplit[0];

		if (FEATURE_PATTERN.matcher(feature).matches()) {
			return feature;
		}
		return null;
	}

	/**
	 * Checks if a given ID is a valid Cloud ALM ID pattern.
	 *
	 * @param id The ID to check
	 * @return true if the ID matches Cloud ALM pattern
	 */
	public static boolean isValidCalmId(String id) {
		if (id == null) {
			return false;
		}
		return CALM_ID_PATTERN.matcher(id).matches();
	}

	/**
	 * Determines if an ID is inside a comment context.
	 *
	 * @param lineText The full line text
	 * @param matchStart The start position of the ID match
	 * @return true if the ID is within an ABAP comment
	 */
	public static boolean isInComment(String lineText, int matchStart) {
		if (lineText == null) {
			return false;
		}

		String textBeforeMatch = lineText.substring(0, matchStart);
		String trimmedLine = lineText.trim();
		return trimmedLine.startsWith("*") || textBeforeMatch.contains("\"");
	}

	/**
	 * Constructs a Cloud ALM URL for the given item ID.
	 *
	 * @param itemId The Cloud ALM item ID (e.g., "6-1234")
	 * @param tenant The Cloud ALM tenant
	 * @param region The Cloud ALM region
	 * @return The complete Cloud ALM URL, or null if invalid ID
	 */
	public static String buildCloudAlmUrl(String itemId, String tenant, String region) {
		if (itemId == null || tenant == null || region == null) {
			return null;
		}

		String baseUrl = "https://" + tenant + "." + region + ".alm.cloud.sap";

		if (itemId.startsWith("6-")) {
			// Feature
			return baseUrl + "/launchpad#feature-display?sap-ui-app-id-hint=com.sap.calm.imp.cdm.features.ui&/details/" + itemId;
		} else if (itemId.startsWith("3-")) {
			// Task/Requirement
			return baseUrl + "/launchpad#task-management?sap-app-origin-hint=&/taskDetail/" + itemId;
		} else if (itemId.startsWith("7-")) {
			// Document
			return baseUrl + "/launchpad#DocumentationObject-manage?sap-ui-app-id-hint=com.sap.calm.imp.sd.docu.ui&/Documents('" + itemId + "')";
		} else if (itemId.startsWith("15-")) {
			// Library
			return baseUrl + "/launchpad#library-management?sap-ui-app-id-hint=com.sap.calm.imp.lib.ui&/LibraryElement('" + itemId + "')";
		}

		return null;
	}

	/**
	 * Resolves a relative version URI to an absolute path.
	 *
	 * @param adtBasePath The base ADT path
	 * @param versionsURL The relative versions URL
	 * @return The resolved absolute path
	 */
	public static String resolveVersionUri(String adtBasePath, String versionsURL) {
		if (adtBasePath == null || versionsURL == null) {
			return versionsURL;
		}

		URI baseUri;
		if (versionsURL.startsWith("./")) {
			// ./ means relative to parent directory - don't add trailing slash
			baseUri = URI.create(adtBasePath);
		} else {
			// No ./ prefix - add trailing slash so resolve appends to base path
			baseUri = URI.create(adtBasePath.endsWith("/") ? adtBasePath : adtBasePath + "/");
		}
		URI resolved = baseUri.resolve(versionsURL);
		return resolved.getPath();
	}

	/**
	 * Finds all Cloud ALM IDs in a line of text.
	 *
	 * @param lineText The text to search
	 * @return Array of found Cloud ALM IDs
	 */
	public static String[] findCalmIds(String lineText) {
		if (lineText == null) {
			return new String[0];
		}

		Matcher matcher = CALM_ID_PATTERN.matcher(lineText);
		java.util.List<String> ids = new java.util.ArrayList<>();
		while (matcher.find()) {
			ids.add(matcher.group());
		}
		return ids.toArray(new String[0]);
	}
}
