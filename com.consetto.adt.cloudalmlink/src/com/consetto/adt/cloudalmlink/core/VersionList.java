package com.consetto.adt.cloudalmlink.core;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.consetto.adt.cloudalmlink.model.FeatureElement;
import com.consetto.adt.cloudalmlink.model.VersionElement;

/**
 * Orders the entries of an ADT versions feed and assigns Cloud ALM features to them.
 * Free of Eclipse dependencies so that it is unit-tested directly (see the tests module).
 */
public final class VersionList {

	/** Feed ID of the active version */
	public static final String ACTIVE_VERSION = "00000";

	/** Feed ID of the inactive draft */
	public static final String INACTIVE_VERSION = "99999";

	/** Displayed ID of the active version */
	public static final String ACTIVE_LABEL = "Active";

	/** Displayed ID of the inactive draft */
	public static final String INACTIVE_LABEL = "Inactive";

	private static final String ACTIVE_TITLE = "Current working version";

	private VersionList() {
	}

	/**
	 * Merges the object's current transport into the active version, labels the active and
	 * inactive versions and sorts the list newest first.
	 * <p>
	 * The feed lists the active version as "00000". While the object is in an open transport, that
	 * entry may already name the transport, often the task; the object's transports endpoint names
	 * the request. One entry stands for both, carrying the request.
	 *
	 * @param versions          The feed entries; changed in place
	 * @param activeTransportId The transport from the object's transports endpoint, or null
	 */
	public static void arrange(List<VersionElement> versions, String activeTransportId) {
		VersionElement active = versions.stream()
				.filter(v -> ACTIVE_VERSION.equals(v.getID()))
				.findFirst()
				.orElse(null);

		if (activeTransportId != null && !activeTransportId.isEmpty()) {
			if (active == null) {
				active = new VersionElement();
				versions.add(active);
			}
			active.setTransport(activeTransportId);
			if (active.getTitle() == null || active.getTitle().isEmpty()) {
				active.setTitle(ACTIVE_TITLE);
			}
		}

		for (VersionElement version : versions) {
			if (version == active || ACTIVE_VERSION.equals(version.getID())) {
				version.setID(ACTIVE_LABEL);
			} else if (INACTIVE_VERSION.equals(version.getID())) {
				version.setID(INACTIVE_LABEL);
			}
		}

		versions.sort(NEWEST_FIRST);
	}

	/**
	 * Inactive draft, then active version, then the others by date and version number, newest first.
	 * The feed order is neither, and the active version's date says nothing about its position.
	 * Undated entries come last.
	 */
	static final Comparator<VersionElement> NEWEST_FIRST = Comparator
			.comparingInt(VersionList::rank)
			.thenComparing(VersionList::updated, Comparator.nullsLast(Comparator.reverseOrder()))
			.thenComparing(VersionElement::getID, Comparator.nullsLast(Comparator.reverseOrder()));

	private static int rank(VersionElement version) {
		if (INACTIVE_LABEL.equals(version.getID())) {
			return 0;
		}
		return ACTIVE_LABEL.equals(version.getID()) ? 1 : 2;
	}

	private static Instant updated(VersionElement version) {
		String updated = version.getLastUpdate();
		if (updated == null || updated.isEmpty()) {
			return null;
		}
		try {
			return Instant.parse(updated);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	/**
	 * Returns the transport to look up in Cloud ALM for a version: the original transport of a
	 * transport of copies, otherwise the version's transport.
	 *
	 * @param version The version
	 * @return The transport ID, or null if the version has none
	 */
	public static String lookupTransportId(VersionElement version) {
		String transportId = version.getTransportId();
		if (transportId == null || transportId.isEmpty()) {
			return null;
		}
		String tocId = AdtResponseParser.extractTocTransportId(version.getTitle());
		return tocId != null ? tocId : transportId;
	}

	/**
	 * Assigns Cloud ALM features to the versions. A transport without a feature may be a task, so
	 * it is looked up again under its request. Each distinct transport is looked up once.
	 *
	 * @param versions       The versions
	 * @param featureLookup  Returns the feature of a transport, or null
	 * @param parentResolver Returns the request of a task, or its argument if it is none
	 */
	public static void assignFeatures(List<VersionElement> versions, Function<String, FeatureElement> featureLookup,
			Function<String, String> parentResolver) {
		Map<String, FeatureElement> lookedUp = new HashMap<>();
		Map<String, FeatureElement> resolved = new HashMap<>();
		// Not computeIfAbsent: it does not cache null results
		Function<String, FeatureElement> cachedLookup = id -> {
			if (!lookedUp.containsKey(id)) {
				lookedUp.put(id, featureLookup.apply(id));
			}
			return lookedUp.get(id);
		};

		for (VersionElement version : versions) {
			String transportId = lookupTransportId(version);
			if (transportId == null) {
				continue;
			}
			if (!resolved.containsKey(transportId)) {
				resolved.put(transportId, findFeature(transportId, cachedLookup, parentResolver));
			}
			FeatureElement feature = resolved.get(transportId);
			if (feature != null) {
				version.setFeature(feature);
			}
		}
	}

	private static FeatureElement findFeature(String transportId, Function<String, FeatureElement> featureLookup,
			Function<String, String> parentResolver) {
		FeatureElement feature = featureLookup.apply(transportId);
		if (feature != null) {
			return feature;
		}
		String parentId = parentResolver.apply(transportId);
		if (parentId == null || parentId.equals(transportId)) {
			return null;
		}
		return featureLookup.apply(parentId);
	}
}
