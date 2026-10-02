package com.consetto.adt.cloudalmlink.views;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.consetto.adt.cloudalmlink.core.VersionSearch;

/**
 * Filter for TransportView that matches search text against all visible columns.
 * The matching itself lives in {@link VersionSearch}, which is unit-tested.
 */
public class TransportFilter extends ViewerFilter {

	private final VersionSearch search = new VersionSearch();

	/**
	 * Sets the search text for filtering.
	 *
	 * @param s The search string (case-insensitive)
	 */
	public void setSearchText(String s) {
		search.setSearchText(s);
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		return search.select(element);
	}
}
