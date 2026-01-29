package com.consetto.adt.cloudalmlink.views;

import java.util.Comparator;
import java.util.function.Function;

/**
 * Immutable definition of a table column configuration.
 * Used to declaratively define columns instead of repetitive inline code.
 *
 * @param <T> The type of elements in the table
 */
public record TableColumnDefinition<T>(
		String title,
		int width,
		Function<T, String> labelExtractor,
		Comparator<T> comparator
) {

	/**
	 * Creates a column definition without a comparator.
	 *
	 * @param title The column header text
	 * @param width The initial column width in pixels
	 * @param labelExtractor Function to extract the display text from an element
	 */
	public TableColumnDefinition(String title, int width, Function<T, String> labelExtractor) {
		this(title, width, labelExtractor, null);
	}

	/**
	 * Gets the display text for an element.
	 *
	 * @param element The table element
	 * @return The text to display, or empty string if null
	 */
	public String getText(T element) {
		if (element == null || labelExtractor == null) {
			return "";
		}
		String text = labelExtractor.apply(element);
		return text != null ? text : "";
	}
}
