package com.consetto.adt.cloudalmlink.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.consetto.adt.cloudalmlink.model.FeatureElement;
import com.consetto.adt.cloudalmlink.model.VersionElement;

/**
 * Unit tests for {@link VersionList}: order of the versions feed and feature assignment.
 */
@DisplayName("VersionList")
class VersionListTest {

	private static VersionElement version(String id, String transportId, String title, String updated) {
		VersionElement version = new VersionElement();
		version.setID(id);
		version.setTransport(transportId);
		version.setTitle(title);
		version.setLastUpdate(updated);
		return version;
	}

	private static FeatureElement feature(String displayId) {
		FeatureElement feature = new FeatureElement();
		feature.setDisplayId(displayId);
		return feature;
	}

	private static List<String> ids(List<VersionElement> versions) {
		return versions.stream().map(VersionElement::getID).toList();
	}

	@Nested
	@DisplayName("arrange")
	class Arrange {

		@Test
		@DisplayName("should put the active version first although the feed lists it elsewhere")
		void shouldPutActiveFirst() {
			// Feed order and dates as returned by ADT
			List<VersionElement> versions = new ArrayList<>(List.of(
					version("00002", "A4HK900002", "second", "2026-06-23T11:23:41Z"),
					version("00000", null, null, "2026-06-23T11:22:09Z"),
					version("00001", "A4HK900001", "first", "2026-06-23T09:34:43Z")));

			VersionList.arrange(versions, null);

			assertThat(ids(versions)).containsExactly("Active", "00002", "00001");
		}

		@Test
		@DisplayName("should sort by date rather than by version number")
		void shouldSortByDate() {
			List<VersionElement> versions = new ArrayList<>(List.of(
					version("00003", "A4HK900003", "older", "2026-01-01T00:00:00Z"),
					version("00002", "A4HK900002", "newer", "2026-02-01T00:00:00Z"),
					version("00001", "A4HK900001", "undated", null)));

			VersionList.arrange(versions, null);

			assertThat(ids(versions)).containsExactly("00002", "00003", "00001");
		}

		@Test
		@DisplayName("should merge the current transport into the active version instead of adding a row")
		void shouldMergeIntoActiveVersion() {
			List<VersionElement> versions = new ArrayList<>(List.of(
					version("00000", "D01K902694", "Demo", "2026-10-02T08:57:10Z"),
					version("00001", "D01K902000", "Older", "2026-09-01T00:00:00Z")));

			VersionList.arrange(versions, "D01K902693");

			assertThat(versions).hasSize(2);
			assertThat(versions.get(0).getID()).isEqualTo("Active");
			assertThat(versions.get(0).getTransportId()).isEqualTo("D01K902693");
			assertThat(versions.get(0).getTitle()).isEqualTo("Demo");
		}

		@Test
		@DisplayName("should add an undated active version when the feed has none")
		void shouldAddActiveVersion() {
			List<VersionElement> versions = new ArrayList<>(List.of(
					version("00001", "A4HK900001", "first", "2026-06-23T09:34:43Z")));

			VersionList.arrange(versions, "A4HK900002");

			assertThat(ids(versions)).containsExactly("Active", "00001");
			assertThat(versions.get(0).getTransportId()).isEqualTo("A4HK900002");
			assertThat(versions.get(0).getTitle()).isEqualTo("Current working version");
			assertThat(versions.get(0).getLastUpdate()).isNull();
		}

		@Test
		@DisplayName("should put the inactive draft before the active version")
		void shouldPutInactiveFirst() {
			List<VersionElement> versions = new ArrayList<>(List.of(
					version("00000", null, null, "2026-06-23T11:22:09Z"),
					version("99999", null, null, "2026-06-24T00:00:00Z")));

			VersionList.arrange(versions, null);

			assertThat(ids(versions)).containsExactly("Inactive", "Active");
		}
	}

	@Nested
	@DisplayName("assignFeatures")
	class AssignFeatures {

		@Test
		@DisplayName("should look up the original transport of a transport of copies")
		void shouldUseTocSource() {
			List<VersionElement> versions = List.of(
					version("00002", "D01K902692", "ToC from D01K900322 : 6-33: WM", null),
					version("00001", "D01K902690", "ToC from D01K900322 : 6-33: WM", null));
			List<String> lookups = new ArrayList<>();

			VersionList.assignFeatures(versions, id -> {
				lookups.add(id);
				return "D01K900322".equals(id) ? feature("6-33") : null;
			}, id -> id);

			assertThat(lookups).containsExactly("D01K900322");
			assertThat(versions).allSatisfy(v -> assertThat(v.getFeature().getDisplayId()).isEqualTo("6-33"));
		}

		@Test
		@DisplayName("should look up a task's request, once per distinct transport")
		void shouldResolveTaskOnce() {
			List<VersionElement> versions = List.of(
					version("00003", "D01K902694", "Demo", null),
					version("00002", "D01K902694", "Demo", null),
					version("00001", "D01K902695", "Demo", null));
			Map<String, String> parents = Map.of("D01K902694", "D01K902693", "D01K902695", "D01K902693");
			List<String> parentLookups = new ArrayList<>();
			List<String> featureLookups = new ArrayList<>();

			VersionList.assignFeatures(versions, id -> {
				featureLookups.add(id);
				return "D01K902693".equals(id) ? feature("6-1") : null;
			}, id -> {
				parentLookups.add(id);
				return parents.getOrDefault(id, id);
			});

			assertThat(parentLookups).containsExactly("D01K902694", "D01K902695");
			assertThat(featureLookups).containsExactly("D01K902694", "D01K902693", "D01K902695");
			assertThat(versions).allSatisfy(v -> assertThat(v.getFeature().getDisplayId()).isEqualTo("6-1"));
		}

		@Test
		@DisplayName("should skip versions without a transport")
		void shouldSkipWithoutTransport() {
			List<VersionElement> versions = List.of(version("Active", null, null, null));

			VersionList.assignFeatures(versions, id -> {
				throw new AssertionError("no lookup expected");
			}, id -> {
				throw new AssertionError("no lookup expected");
			});

			assertThat(versions.get(0).getFeature()).isNull();
		}
	}
}
