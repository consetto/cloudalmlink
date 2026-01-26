package com.consetto.adt.cloudalmlink.views;

import com.consetto.adt.cloudalmlink.model.VersionElement;

/**
 * Filter for TransportView that matches search text against all visible columns.
 * Supports case-insensitive partial matching across all fields.
 *
 * Note: This is a standalone version without Eclipse ViewerFilter dependency for testing.
 */
public class TransportFilter {

	private String searchString = "";

	/**
	 * Sets the search text for filtering.
	 *
	 * @param s The search string (case-insensitive)
	 */
	public void setSearchText(String s) {
		this.searchString = (s == null ? "" : s.toLowerCase().trim());
	}

	/**
	 * Gets the current search text.
	 *
	 * @return The current search string
	 */
	public String getSearchText() {
		return searchString;
	}

	/**
	 * Determines if the given element matches the current search criteria.
	 *
	 * @param element The element to check
	 * @return true if the element matches, false otherwise
	 */
	public boolean select(Object element) {
		if (searchString.isEmpty()) {
			return true;
		}

		if (!(element instanceof VersionElement)) {
			return false;
		}

		VersionElement v = (VersionElement) element;

		// Match against version/transport fields
		if (matches(v.getID())) return true;
		if (matches(v.getTransportId())) return true;
		if (matches(v.getAuthor())) return true;
		if (matches(v.getTitle())) return true;

		// Match against feature fields
		if (v.getFeature() != null) {
			if (matches(v.getFeature().getDisplayId())) return true;
			if (matches(v.getFeature().getStatus())) return true;
			if (matches(v.getFeature().getResponsibleId())) return true;
		}

		return false;
	}

	private boolean matches(String value) {
		return value != null && value.toLowerCase().contains(searchString);
	}
}
