package com.consetto.adt.cloudalmlink.core;

import java.util.Locale;
import java.util.stream.Stream;

import com.consetto.adt.cloudalmlink.model.FeatureElement;
import com.consetto.adt.cloudalmlink.model.VersionElement;

/**
 * Case-insensitive search across all columns of the transport view.
 */
public class VersionSearch {

	private String searchString = "";

	/**
	 * Sets the search text.
	 *
	 * @param s The search text; null clears the search
	 */
	public void setSearchText(String s) {
		this.searchString = s == null ? "" : s.toLowerCase(Locale.ROOT).trim();
	}

	/**
	 * Returns the normalized search text (lower case, trimmed).
	 *
	 * @return The search string
	 */
	public String getSearchText() {
		return searchString;
	}

	/**
	 * Checks whether an element matches the search text.
	 *
	 * @param element The element to test
	 * @return true if the search is empty or any column of the version contains the text
	 */
	public boolean select(Object element) {
		if (searchString.isEmpty()) {
			return true;
		}
		if (!(element instanceof VersionElement v)) {
			return false;
		}

		Stream<String> versionColumns = Stream.of(v.getID(), v.getTransportId(), v.getAuthor(), v.getTitle());
		FeatureElement f = v.getFeature();
		Stream<String> featureColumns = f == null ? Stream.empty()
				: Stream.of(f.getDisplayId(), f.getStatus(), f.getResponsibleId(), f.getTitle(), f.getPriority(),
						f.getWorkstreamName(), f.getScopeName(), f.getReleaseName(), f.getRequirementTitle(),
						f.getModifiedDate());

		return Stream.concat(versionColumns, featureColumns).anyMatch(this::matches);
	}

	private boolean matches(String value) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(searchString);
	}
}
