package com.consetto.adt.cloudalmlink.model;

/**
 * Enum representing Cloud ALM item types identified by their ID prefix.
 * Provides type-safe handling of different Cloud ALM entities.
 */
public enum CloudAlmItemType {

	/** Feature items (prefix: 6-) */
	FEATURE("6-"),

	/** Task/Requirement items (prefix: 3-) */
	TASK("3-"),

	/** Document items (prefix: 7-) */
	DOCUMENT("7-"),

	/** Library items (prefix: 15-) */
	LIBRARY("15-"),

	/** Unknown item type */
	UNKNOWN("");

	private final String prefix;

	CloudAlmItemType(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * Gets the ID prefix for this item type.
	 *
	 * @return The prefix string
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Determines the item type from a Cloud ALM ID.
	 *
	 * @param itemId The item ID (e.g., "6-1234")
	 * @return The corresponding item type, or UNKNOWN if not recognized
	 */
	public static CloudAlmItemType fromId(String itemId) {
		if (itemId == null || itemId.isEmpty()) {
			return UNKNOWN;
		}

		// Check prefixes in order (15- before others to avoid false matches)
		if (itemId.startsWith("15-")) {
			return LIBRARY;
		}
		if (itemId.startsWith("6-")) {
			return FEATURE;
		}
		if (itemId.startsWith("3-")) {
			return TASK;
		}
		if (itemId.startsWith("7-")) {
			return DOCUMENT;
		}

		return UNKNOWN;
	}

	/**
	 * Checks if the given ID matches this item type.
	 *
	 * @param itemId The item ID to check
	 * @return true if the ID starts with this type's prefix
	 */
	public boolean matches(String itemId) {
		return itemId != null && !prefix.isEmpty() && itemId.startsWith(prefix);
	}

	/**
	 * Builds the appropriate Cloud ALM URL for the given item ID.
	 *
	 * @param itemId The item ID
	 * @param config The Cloud ALM configuration
	 * @return The URL for this item, or null if type is UNKNOWN
	 */
	public String buildUrl(String itemId, CloudAlmConfig config) {
		if (config == null || !config.hasConnectionSettings()) {
			return null;
		}

		return switch (this) {
			case FEATURE -> config.featureUrl(itemId);
			case TASK -> config.taskUrl(itemId);
			case DOCUMENT -> config.documentUrl(itemId);
			case LIBRARY -> config.libraryUrl(itemId);
			case UNKNOWN -> null;
		};
	}

	/**
	 * Gets the URL for a Cloud ALM item ID using the appropriate type.
	 *
	 * @param itemId The item ID
	 * @param config The Cloud ALM configuration
	 * @return The URL, or null if the item type is unknown
	 */
	public static String getUrlForItem(String itemId, CloudAlmConfig config) {
		CloudAlmItemType type = fromId(itemId);
		return type.buildUrl(itemId, config);
	}
}
