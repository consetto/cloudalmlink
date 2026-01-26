package com.consetto.adt.cloudalmlink.views;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.consetto.adt.cloudalmlink.model.FeatureElement;
import com.consetto.adt.cloudalmlink.model.VersionElement;

/**
 * Unit tests for {@link TransportFilter}.
 * Tests filtering logic for the TransportView.
 */
@DisplayName("TransportFilter")
class TransportFilterTest {

	private TransportFilter filter;
	private VersionElement versionWithFeature;
	private VersionElement versionWithoutFeature;

	@BeforeEach
	void setUp() {
		filter = new TransportFilter();

		// Create version with feature
		versionWithFeature = new VersionElement();
		versionWithFeature.setID("00001");
		versionWithFeature.setTransport("DEVK900042");
		versionWithFeature.setAuthor("DEVELOPER1");
		versionWithFeature.setTitle("Fix payment gateway bug");

		FeatureElement feature = new FeatureElement();
		feature.setDisplayId("6-1234");
		feature.setStatusCode("IN_PROGRESS");
		feature.setResponsibleId("PRODUCT_OWNER");
		versionWithFeature.setFeature(feature);

		// Create version without feature
		versionWithoutFeature = new VersionElement();
		versionWithoutFeature.setID("00002");
		versionWithoutFeature.setTransport("DEVK900043");
		versionWithoutFeature.setAuthor("DEVELOPER2");
		versionWithoutFeature.setTitle("Refactor logging module");
	}

	@Nested
	@DisplayName("Empty Search")
	class EmptySearch {

		@Test
		@DisplayName("should match all elements when search is empty")
		void shouldMatchAllWhenSearchEmpty() {
			filter.setSearchText("");

			assertThat(filter.select(versionWithFeature)).isTrue();
			assertThat(filter.select(versionWithoutFeature)).isTrue();
		}

		@Test
		@DisplayName("should match all elements when search is null")
		void shouldMatchAllWhenSearchNull() {
			filter.setSearchText(null);

			assertThat(filter.select(versionWithFeature)).isTrue();
			assertThat(filter.select(versionWithoutFeature)).isTrue();
		}

		@Test
		@DisplayName("should match all elements when search is whitespace")
		void shouldMatchAllWhenSearchWhitespace() {
			filter.setSearchText("   ");

			assertThat(filter.select(versionWithFeature)).isTrue();
			assertThat(filter.select(versionWithoutFeature)).isTrue();
		}
	}

	@Nested
	@DisplayName("Version Field Matching")
	class VersionFieldMatching {

		@Test
		@DisplayName("should match by version ID")
		void shouldMatchByVersionId() {
			filter.setSearchText("00001");

			assertThat(filter.select(versionWithFeature)).isTrue();
			assertThat(filter.select(versionWithoutFeature)).isFalse();
		}

		@Test
		@DisplayName("should match by transport ID")
		void shouldMatchByTransportId() {
			filter.setSearchText("DEVK900042");

			assertThat(filter.select(versionWithFeature)).isTrue();
			assertThat(filter.select(versionWithoutFeature)).isFalse();
		}

		@Test
		@DisplayName("should match by partial transport ID")
		void shouldMatchByPartialTransportId() {
			filter.setSearchText("900042");

			assertThat(filter.select(versionWithFeature)).isTrue();
			assertThat(filter.select(versionWithoutFeature)).isFalse();
		}

		@Test
		@DisplayName("should match by author")
		void shouldMatchByAuthor() {
			filter.setSearchText("DEVELOPER1");

			assertThat(filter.select(versionWithFeature)).isTrue();
			assertThat(filter.select(versionWithoutFeature)).isFalse();
		}

		@Test
		@DisplayName("should match by title")
		void shouldMatchByTitle() {
			filter.setSearchText("payment");

			assertThat(filter.select(versionWithFeature)).isTrue();
			assertThat(filter.select(versionWithoutFeature)).isFalse();
		}

		@Test
		@DisplayName("should match by partial title")
		void shouldMatchByPartialTitle() {
			filter.setSearchText("gateway");

			assertThat(filter.select(versionWithFeature)).isTrue();
			assertThat(filter.select(versionWithoutFeature)).isFalse();
		}
	}

	@Nested
	@DisplayName("Feature Field Matching")
	class FeatureFieldMatching {

		@Test
		@DisplayName("should match by feature display ID")
		void shouldMatchByFeatureDisplayId() {
			filter.setSearchText("6-1234");

			assertThat(filter.select(versionWithFeature)).isTrue();
			assertThat(filter.select(versionWithoutFeature)).isFalse();
		}

		@Test
		@DisplayName("should match by feature status")
		void shouldMatchByFeatureStatus() {
			filter.setSearchText("IN_PROGRESS");

			assertThat(filter.select(versionWithFeature)).isTrue();
			assertThat(filter.select(versionWithoutFeature)).isFalse();
		}

		@Test
		@DisplayName("should match by feature responsible ID")
		void shouldMatchByFeatureResponsibleId() {
			filter.setSearchText("PRODUCT_OWNER");

			assertThat(filter.select(versionWithFeature)).isTrue();
			assertThat(filter.select(versionWithoutFeature)).isFalse();
		}

		@Test
		@DisplayName("should not fail when version has no feature")
		void shouldNotFailWhenNoFeature() {
			filter.setSearchText("SOME_STATUS");

			assertThat(filter.select(versionWithoutFeature)).isFalse();
		}
	}

	@Nested
	@DisplayName("Case Insensitivity")
	class CaseInsensitivity {

		@Test
		@DisplayName("should match case-insensitively for author")
		void shouldMatchCaseInsensitivelyForAuthor() {
			filter.setSearchText("developer1");

			assertThat(filter.select(versionWithFeature)).isTrue();
		}

		@Test
		@DisplayName("should match case-insensitively for title")
		void shouldMatchCaseInsensitivelyForTitle() {
			filter.setSearchText("PAYMENT");

			assertThat(filter.select(versionWithFeature)).isTrue();
		}

		@Test
		@DisplayName("should match case-insensitively for transport")
		void shouldMatchCaseInsensitivelyForTransport() {
			filter.setSearchText("devk900042");

			assertThat(filter.select(versionWithFeature)).isTrue();
		}

		@Test
		@DisplayName("should match case-insensitively for status")
		void shouldMatchCaseInsensitivelyForStatus() {
			filter.setSearchText("in_progress");

			assertThat(filter.select(versionWithFeature)).isTrue();
		}
	}

	@Nested
	@DisplayName("Non-VersionElement Objects")
	class NonVersionElementObjects {

		@Test
		@DisplayName("should not match non-VersionElement objects")
		void shouldNotMatchNonVersionElement() {
			filter.setSearchText("test");

			assertThat(filter.select("String object")).isFalse();
			assertThat(filter.select(Integer.valueOf(123))).isFalse();
			assertThat(filter.select(new Object())).isFalse();
		}

		@Test
		@DisplayName("should not match null objects")
		void shouldNotMatchNull() {
			filter.setSearchText("test");

			assertThat(filter.select(null)).isFalse();
		}
	}

	@Nested
	@DisplayName("Search Text Management")
	class SearchTextManagement {

		@Test
		@DisplayName("should trim search text")
		void shouldTrimSearchText() {
			filter.setSearchText("  payment  ");

			assertThat(filter.getSearchText()).isEqualTo("payment");
			assertThat(filter.select(versionWithFeature)).isTrue();
		}

		@Test
		@DisplayName("should convert search text to lowercase")
		void shouldConvertToLowercase() {
			filter.setSearchText("PAYMENT");

			assertThat(filter.getSearchText()).isEqualTo("payment");
		}

		@Test
		@DisplayName("should update search text")
		void shouldUpdateSearchText() {
			filter.setSearchText("first");
			assertThat(filter.getSearchText()).isEqualTo("first");

			filter.setSearchText("second");
			assertThat(filter.getSearchText()).isEqualTo("second");
		}
	}

	@Nested
	@DisplayName("Null Field Handling")
	class NullFieldHandling {

		@Test
		@DisplayName("should handle version with null fields")
		void shouldHandleNullFields() {
			VersionElement nullFieldVersion = new VersionElement();
			// All fields are null by default

			filter.setSearchText("test");

			assertThat(filter.select(nullFieldVersion)).isFalse();
		}

		@Test
		@DisplayName("should match partial when some fields are null")
		void shouldMatchPartialWhenSomeFieldsNull() {
			VersionElement partialVersion = new VersionElement();
			partialVersion.setTitle("Test title");
			// Other fields are null

			filter.setSearchText("test");

			assertThat(filter.select(partialVersion)).isTrue();
		}
	}

	@Nested
	@DisplayName("Multiple Matches")
	class MultipleMatches {

		@Test
		@DisplayName("should match when search appears in multiple fields")
		void shouldMatchMultipleFields() {
			// Search term "DEV" appears in both transport ID and author
			versionWithFeature.setAuthor("DEV_USER");

			filter.setSearchText("DEV");

			assertThat(filter.select(versionWithFeature)).isTrue();
		}

		@Test
		@DisplayName("should match common prefix across versions")
		void shouldMatchCommonPrefix() {
			filter.setSearchText("DEVK");

			assertThat(filter.select(versionWithFeature)).isTrue();
			assertThat(filter.select(versionWithoutFeature)).isTrue();
		}
	}
}
