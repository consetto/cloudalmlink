package com.consetto.adt.cloudalmlink.core;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts links, transport IDs and feature IDs from ADT responses and texts.
 * Free of Eclipse dependencies so that it is unit-tested directly (see the tests module).
 */
public final class AdtResponseParser {

	/** atom:link or link elements with rel and href attributes, in either order */
	private static final Pattern LINK_PATTERN = Pattern.compile(
			"<(?:atom:)?link[^>]*\\s(?:rel=[\"']([^\"']*)[\"'][^>]*href=[\"']([^\"']*)[\"']|href=[\"']([^\"']*)[\"'][^>]*rel=[\"']([^\"']*)[\"'])[^>]*/?>",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern TM_REQUEST_ATTRIBUTE = Pattern.compile("tm:request=\"([A-Z0-9]+)\"");
	private static final Pattern TRANSPORT_ELEMENT = Pattern.compile(">([A-Z][A-Z0-9]{2}K\\d{6})<");
	private static final Pattern TRANSPORT_ANYWHERE = Pattern.compile("[A-Z][A-Z0-9]{2}K\\d{6}");
	private static final Pattern TASK_ELEMENT = Pattern.compile("<tm:task\\s([^>]+)>");
	private static final Pattern PARENT_ATTRIBUTE = Pattern.compile("tm:parent=\"([^\"]+)\"");

	/** Version title of a transport of copies: "ToC from DEVK900042: ..." */
	private static final Pattern TOC_PATTERN = Pattern.compile("^ToC from (\\S+)\\s*:");

	/** Transport description starting with a feature ID: "6-1234: ..." */
	private static final Pattern FEATURE_ID = Pattern.compile("^6-\\d+$");

	private AdtResponseParser() {
	}

	/**
	 * Parses the atom links of an ADT object response.
	 *
	 * @param xmlResponse The XML response
	 * @return The links found, never null
	 */
	public static List<AtomLink> parseAtomLinks(String xmlResponse) {
		List<AtomLink> links = new ArrayList<>();
		if (xmlResponse == null || xmlResponse.isEmpty()) {
			return links;
		}

		Matcher matcher = LINK_PATTERN.matcher(xmlResponse);
		while (matcher.find()) {
			boolean relFirst = matcher.group(1) != null;
			String rel = relFirst ? matcher.group(1) : matcher.group(4);
			String href = relFirst ? matcher.group(2) : matcher.group(3);
			if (rel != null && href != null) {
				links.add(new AtomLink(rel, href));
			}
		}
		return links;
	}

	/**
	 * Extracts and decodes the uri= query parameter from a link href.
	 *
	 * @param href The link href
	 * @return The decoded URI value, or null if there is none
	 */
	public static String extractUriParameter(String href) {
		if (href == null) {
			return null;
		}
		int uriStart = href.indexOf("uri=");
		if (uriStart == -1) {
			return null;
		}
		uriStart += 4;
		int uriEnd = href.indexOf('&', uriStart);
		if (uriEnd == -1) {
			uriEnd = href.length();
		}
		try {
			return URLDecoder.decode(href.substring(uriStart, uriEnd), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Returns the object URI from the first link that carries a uri= parameter.
	 *
	 * @param links The object's atom links
	 * @return The decoded object URI, or null if no link carries one
	 */
	public static String extractObjectUri(List<AtomLink> links) {
		for (AtomLink link : links) {
			String uri = extractUriParameter(link.href());
			if (uri != null) {
				return uri;
			}
		}
		return null;
	}

	/**
	 * Extracts the ADT path from a raw location URI such as adt://DEST/sap/bc/adt/...
	 *
	 * @param rawUri The raw URI
	 * @return The path starting at /sap/bc/adt/ without query, or null if there is none
	 */
	public static String extractPathFromRawUri(String rawUri) {
		if (rawUri == null) {
			return null;
		}
		int adtIndex = rawUri.indexOf("/sap/bc/adt/");
		if (adtIndex == -1) {
			return null;
		}
		int endIndex = rawUri.indexOf('?', adtIndex);
		return rawUri.substring(adtIndex, endIndex == -1 ? rawUri.length() : endIndex);
	}

	/**
	 * Extracts the transport ID from the response of an object's transports endpoint.
	 *
	 * @param transportResponse The raw response
	 * @return The transport ID, or null if none is found
	 */
	public static String extractTransportId(String transportResponse) {
		if (transportResponse == null) {
			return null;
		}
		Matcher matcher = TM_REQUEST_ATTRIBUTE.matcher(transportResponse);
		if (matcher.find()) {
			return matcher.group(1);
		}
		matcher = TRANSPORT_ELEMENT.matcher(transportResponse);
		if (matcher.find()) {
			return matcher.group(1);
		}
		matcher = TRANSPORT_ANYWHERE.matcher(transportResponse);
		if (matcher.find()) {
			return matcher.group();
		}
		return null;
	}

	/**
	 * Returns the parent request of a transport task from a CTS transport request response.
	 *
	 * @param xmlResponse The response of /sap/bc/adt/cts/transportrequests/{id}
	 * @param transportId The transport ID to check
	 * @return The parent request if the ID is a task, null otherwise
	 */
	public static String extractParentTransport(String xmlResponse, String transportId) {
		if (xmlResponse == null || transportId == null) {
			return null;
		}
		Matcher matcher = TASK_ELEMENT.matcher(xmlResponse);
		while (matcher.find()) {
			String attributes = matcher.group(1);
			if (attributes.contains("tm:number=\"" + transportId + "\"")) {
				Matcher parentMatcher = PARENT_ATTRIBUTE.matcher(attributes);
				if (parentMatcher.find() && !parentMatcher.group(1).isEmpty()) {
					return parentMatcher.group(1);
				}
			}
		}
		return null;
	}

	/**
	 * Extracts the original transport from a "ToC from {transportId}: ..." version title.
	 *
	 * @param title The version title
	 * @return The transport ID, or null if the title is not a transport of copies
	 */
	public static String extractTocTransportId(String title) {
		if (title == null) {
			return null;
		}
		Matcher matcher = TOC_PATTERN.matcher(title);
		return matcher.find() ? matcher.group(1) : null;
	}

	/**
	 * Extracts a feature ID from a transport description of the form "6-1234: ...".
	 *
	 * @param description The transport request description
	 * @return The feature ID, or null if the description does not start with one
	 */
	public static String extractFeatureIdFromDescription(String description) {
		if (description == null || description.isEmpty()) {
			return null;
		}
		// Not split(":")[0]: a description of only colons splits into an empty array
		int colon = description.indexOf(':');
		String candidate = colon == -1 ? description : description.substring(0, colon);
		return FEATURE_ID.matcher(candidate).matches() ? candidate : null;
	}
}
