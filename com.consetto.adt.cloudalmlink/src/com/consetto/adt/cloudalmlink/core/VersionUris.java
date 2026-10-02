package com.consetto.adt.cloudalmlink.core;

import java.net.URI;
import java.util.List;

/**
 * Finds the versions and transports endpoints of an ADT object from its atom links.
 */
public final class VersionUris {

	public static final String TRANSPORT_REL = "http://www.sap.com/adt/relations/transport";

	/**
	 * Endpoints of an ADT object.
	 *
	 * @param versionsUrl   Absolute path of the versions feed (never null)
	 * @param transportsUrl The transports endpoint of the active version, or null
	 */
	public record Endpoints(String versionsUrl, String transportsUrl) {
	}

	private VersionUris() {
	}

	/**
	 * Determines the versions and transports endpoints of an object.
	 *
	 * @param links          The object's atom links
	 * @param objectType     The ADT object type, e.g. "CLAS/OC"
	 * @param objectUri      The object URI, or null
	 * @param rawLocationUri The editor's raw location URI (adt://DEST/sap/bc/adt/...), or null
	 * @return The endpoints
	 */
	public static Endpoints find(List<AtomLink> links, String objectType, String objectUri, String rawLocationUri) {
		String versionsUrl = null;
		String transportsUrl = null;
		String adtBasePath = null;

		for (AtomLink link : links) {
			String href = link.href();
			String rel = link.rel();
			if (href == null) {
				continue;
			}

			if (href.contains("source/main/versions")
					|| href.contains("implementations/versions")
					|| href.contains("definitions/versions")
					|| (rel != null && rel.contains("relations/versions"))) {
				versionsUrl = href;
			}
			if (TRANSPORT_REL.equals(rel)) {
				transportsUrl = href;
			}
			if (adtBasePath == null && href.contains("uri=")) {
				adtBasePath = AdtResponseParser.extractUriParameter(href);
			}
		}

		if (versionsUrl == null) {
			versionsUrl = switch (objectType == null ? "" : objectType) {
				case "CLAS/OC" -> "includes/implementations/versions";
				case "CLAS/OO" -> "includes/definitions/versions";
				default -> "versions";
			};
		}

		if (!versionsUrl.startsWith("/")) {
			versionsUrl = resolveRelative(adtBasePath, versionsUrl, objectUri, rawLocationUri, objectType);
		}
		return new Endpoints(versionsUrl, transportsUrl);
	}

	private static String resolveRelative(String adtBasePath, String versionsUrl, String objectUri,
			String rawLocationUri, String objectType) {
		if (adtBasePath != null) {
			return resolveVersionUri(adtBasePath, versionsUrl);
		}
		if (objectUri != null) {
			URI baseUri = URI.create(objectUri.endsWith("/") ? objectUri : objectUri + "/");
			return baseUri.resolve(versionsUrl).normalize().getPath();
		}
		if (rawLocationUri != null) {
			return resolveFromRawLocationUri(rawLocationUri, versionsUrl, objectType);
		}
		return versionsUrl;
	}

	/**
	 * Resolves a relative versions URL against the object's base path.
	 *
	 * @param adtBasePath The object's ADT path, or null
	 * @param versionsUrl The relative versions URL, or null
	 * @return The absolute path; versionsUrl unchanged if adtBasePath is null
	 */
	public static String resolveVersionUri(String adtBasePath, String versionsUrl) {
		if (adtBasePath == null || versionsUrl == null) {
			return versionsUrl;
		}

		if (versionsUrl.startsWith("./")) {
			String relativePath = versionsUrl.substring(2);

			// The relative path may repeat the end of the base path,
			// e.g. base ".../source/main" and "./source/main/versions"
			int lastSlash = relativePath.lastIndexOf('/');
			if (lastSlash > 0) {
				String pathPrefix = relativePath.substring(0, lastSlash);
				if (adtBasePath.endsWith(pathPrefix) || adtBasePath.endsWith("/" + pathPrefix)) {
					return adtBasePath + relativePath.substring(lastSlash);
				}
			}
			return URI.create(adtBasePath).resolve(versionsUrl).normalize().getPath();
		}

		// No ./ prefix: append to the base path
		URI baseUri = URI.create(adtBasePath.endsWith("/") ? adtBasePath : adtBasePath + "/");
		return baseUri.resolve(versionsUrl).getPath();
	}

	/**
	 * Resolves a versions URL from an editor's raw location URI when the links carry no base path.
	 *
	 * @param rawLocationUri adt://DEST/sap/bc/adt/... or adt://DEST.client/sap/bc/adt/...
	 * @param versionsUrl    The relative versions URL
	 * @param objectType     The ADT object type
	 * @return The absolute path, or versionsUrl if the raw URI has no ADT path
	 */
	public static String resolveFromRawLocationUri(String rawLocationUri, String versionsUrl, String objectType) {
		int dotIndex = rawLocationUri.indexOf('.');
		int slashIndex = rawLocationUri.lastIndexOf('/');

		String path;
		if (dotIndex > 0 && dotIndex < slashIndex) {
			path = rawLocationUri.substring(dotIndex + 1, slashIndex + 1);
		} else {
			int sapIndex = rawLocationUri.indexOf("/sap/bc/adt/");
			if (sapIndex == -1) {
				return versionsUrl;
			}
			// From "adt/..." up to the last slash
			path = rawLocationUri.substring(sapIndex, slashIndex + 1).substring("/sap/bc/".length());
		}

		// DDIC sources use different collection paths for versions
		path = path.replace("/ddlsources/", "/ddl/sources/")
				.replace("/ddlxsources/", "/ddlx/sources/")
				.replace("/dclsources/", "/dcl/sources/");

		String versionUri = "/sap/bc/" + path + versionsUrl;
		if ("CLAS/OC".equals(objectType) && !versionUri.contains("/adt/oo")) {
			versionUri = versionUri.replace("/adt/classlib", "/adt/oo");
		}
		return URI.create(versionUri).normalize().getPath();
	}
}
