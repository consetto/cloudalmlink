package com.consetto.adt.cloudalmlink.views;

import java.util.Comparator;
import java.util.List;

import com.consetto.adt.cloudalmlink.model.VersionElement;

/**
 * Factory providing column definitions for the Transport View table.
 * Centralizes column configuration that was previously scattered in createColumns().
 */
public final class TransportViewColumns {

	private TransportViewColumns() {
		// Prevent instantiation
	}

	/**
	 * Gets the list of column definitions for the transport view.
	 *
	 * @return Immutable list of column definitions
	 */
	public static List<TableColumnDefinition<VersionElement>> getColumns() {
		return List.of(
				new TableColumnDefinition<>(
						"ID",
						80,
						VersionElement::getID,
						Comparator.comparing(VersionElement::getID, Comparator.nullsLast(Comparator.naturalOrder()))
				),
				new TableColumnDefinition<>(
						"Transport",
						120,
						VersionElement::getTransportId,
						Comparator.comparing(VersionElement::getTransportId, Comparator.nullsLast(Comparator.naturalOrder()))
				),
				new TableColumnDefinition<>(
						"TR Owner",
						100,
						v -> v.getAuthor() != null ? v.getAuthor() : "",
						Comparator.comparing(VersionElement::getAuthor, Comparator.nullsLast(Comparator.naturalOrder()))
				),
				new TableColumnDefinition<>(
						"Title",
						180,
						VersionElement::getTitle,
						Comparator.comparing(VersionElement::getTitle, Comparator.nullsLast(Comparator.naturalOrder()))
				),
				new TableColumnDefinition<>(
						"Feature",
						100,
						v -> v.getFeature() != null ? v.getFeature().getDisplayId() : "No Feature",
						Comparator.comparing(
								v -> v.getFeature() != null ? v.getFeature().getDisplayId() : "",
								Comparator.nullsLast(Comparator.naturalOrder())
						)
				),
				new TableColumnDefinition<>(
						"Status",
						100,
						v -> v.getFeature() != null ? v.getFeature().getStatus() : "No Feature",
						Comparator.comparing(
								v -> v.getFeature() != null ? v.getFeature().getStatus() : "",
								Comparator.nullsLast(Comparator.naturalOrder())
						)
				),
				new TableColumnDefinition<>(
						"Responsible",
						100,
						v -> v.getFeature() != null ? v.getFeature().getResponsibleId() : "No Feature",
						Comparator.comparing(
								v -> v.getFeature() != null ? v.getFeature().getResponsibleId() : "",
								Comparator.nullsLast(Comparator.naturalOrder())
						)
				)
		);
	}
}
