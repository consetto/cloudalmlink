package com.consetto.adt.cloudalmlink.handlers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link PatternUtils}.
 * Tests pattern matching and URL construction logic extracted from handlers.
 */
@DisplayName("PatternUtils")
class PatternUtilsTest {

	@Nested
	@DisplayName("extractTransportId")
	class ExtractTransportId {

		@Test
		@DisplayName("should extract transport ID from tm:request attribute")
		void shouldExtractFromTmRequestAttribute() {
			String response = """
				<transport tm:request="NPLK900001" xmlns:tm="http://example.com">
					<description>Test</description>
				</transport>
				""";

			String result = PatternUtils.extractTransportId(response);

			assertThat(result).isEqualTo("NPLK900001");
		}

		@Test
		@DisplayName("should extract transport ID from element content")
		void shouldExtractFromElementContent() {
			String response = """
				<request>
					<number>DEVK912345</number>
				</request>
				""";

			String result = PatternUtils.extractTransportId(response);

			assertThat(result).isEqualTo("DEVK912345");
		}

		@Test
		@DisplayName("should extract transport ID using general pattern")
		void shouldExtractUsingGeneralPattern() {
			String response = "Some text with transport S4DK911940 embedded";

			String result = PatternUtils.extractTransportId(response);

			assertThat(result).isEqualTo("S4DK911940");
		}

		@ParameterizedTest
		@DisplayName("should extract various transport ID formats")
		@CsvSource({
			"'tm:request=\"NPLK900001\"', NPLK900001",
			"'>DEVK912345<', DEVK912345",
			"'Transport: S4DK911940', S4DK911940",
			"'ABCK123456 is the ID', ABCK123456",
			"'Request XYZK000001 created', XYZK000001"
		})
		void shouldExtractVariousFormats(String response, String expected) {
			String result = PatternUtils.extractTransportId(response);
			assertThat(result).isEqualTo(expected);
		}

		@Test
		@DisplayName("should return null for null input")
		void shouldReturnNullForNullInput() {
			assertThat(PatternUtils.extractTransportId(null)).isNull();
		}

		@Test
		@DisplayName("should return null when no transport ID found")
		void shouldReturnNullWhenNotFound() {
			String response = "No transport ID here";

			assertThat(PatternUtils.extractTransportId(response)).isNull();
		}

		@Test
		@DisplayName("should prioritize tm:request attribute pattern")
		void shouldPrioritizeTmRequestAttribute() {
			String response = """
				<transport tm:request="NPLK900001">
					<other>DEVK912345</other>
				</transport>
				""";

			String result = PatternUtils.extractTransportId(response);

			assertThat(result).isEqualTo("NPLK900001");
		}
	}

	@Nested
	@DisplayName("extractUriParameter")
	class ExtractUriParameter {

		@Test
		@DisplayName("should extract and decode URI parameter")
		void shouldExtractAndDecodeUri() {
			String href = "http://example.com/action?uri=%2Fsap%2Fbc%2Fadt%2Fclasses%2Fzcl_test";

			String result = PatternUtils.extractUriParameter(href);

			assertThat(result).isEqualTo("/sap/bc/adt/classes/zcl_test");
		}

		@Test
		@DisplayName("should extract URI parameter with additional query params")
		void shouldExtractWithAdditionalParams() {
			String href = "http://example.com?uri=%2Fpath%2Fto%2Fobject&other=value";

			String result = PatternUtils.extractUriParameter(href);

			assertThat(result).isEqualTo("/path/to/object");
		}

		@Test
		@DisplayName("should handle URI at end of string")
		void shouldHandleUriAtEnd() {
			String href = "http://example.com?param=value&uri=%2Fsap%2Fbc";

			String result = PatternUtils.extractUriParameter(href);

			assertThat(result).isEqualTo("/sap/bc");
		}

		@Test
		@DisplayName("should return null when no uri parameter")
		void shouldReturnNullWhenNoUriParam() {
			String href = "http://example.com?param=value";

			assertThat(PatternUtils.extractUriParameter(href)).isNull();
		}

		@Test
		@DisplayName("should return null for null input")
		void shouldReturnNullForNullInput() {
			assertThat(PatternUtils.extractUriParameter(null)).isNull();
		}
	}

	@Nested
	@DisplayName("getFeatureFromDescription")
	class GetFeatureFromDescription {

		@Test
		@DisplayName("should extract feature ID from description")
		void shouldExtractFeatureId() {
			String descr = "6-1234: Fix payment bug";

			String result = PatternUtils.getFeatureFromDescription(descr);

			assertThat(result).isEqualTo("6-1234");
		}

		@Test
		@DisplayName("should extract feature ID without colon")
		void shouldExtractFeatureIdWithoutColon() {
			String descr = "6-5678";

			String result = PatternUtils.getFeatureFromDescription(descr);

			assertThat(result).isEqualTo("6-5678");
		}

		@ParameterizedTest
		@DisplayName("should extract various feature ID formats")
		@CsvSource({
			"'6-1: Simple', 6-1",
			"'6-12: Test', 6-12",
			"'6-123: Medium', 6-123",
			"'6-1234: Standard', 6-1234",
			"'6-12345: Long', 6-12345"
		})
		void shouldExtractVariousFormats(String descr, String expected) {
			assertThat(PatternUtils.getFeatureFromDescription(descr)).isEqualTo(expected);
		}

		@Test
		@DisplayName("should return null for non-feature prefix")
		void shouldReturnNullForNonFeaturePrefix() {
			String descr = "3-1234: This is a task, not a feature";

			assertThat(PatternUtils.getFeatureFromDescription(descr)).isNull();
		}

		@Test
		@DisplayName("should return null for invalid format")
		void shouldReturnNullForInvalidFormat() {
			String descr = "Not a feature ID";

			assertThat(PatternUtils.getFeatureFromDescription(descr)).isNull();
		}

		@ParameterizedTest
		@NullAndEmptySource
		@DisplayName("should return null for null or empty input")
		void shouldReturnNullForNullOrEmpty(String descr) {
			assertThat(PatternUtils.getFeatureFromDescription(descr)).isNull();
		}
	}

	@Nested
	@DisplayName("isValidCalmId")
	class IsValidCalmId {

		@ParameterizedTest
		@DisplayName("should return true for valid Cloud ALM IDs")
		@ValueSource(strings = {"6-1", "6-123", "6-12345", "3-1", "3-999", "7-1", "7-42", "15-1", "15-9999"})
		void shouldReturnTrueForValidIds(String id) {
			assertThat(PatternUtils.isValidCalmId(id)).isTrue();
		}

		@ParameterizedTest
		@DisplayName("should return false for invalid Cloud ALM IDs")
		@ValueSource(strings = {"1-123", "2-123", "4-123", "5-123", "8-123", "16-123", "abc", "6-", "-123", "6123"})
		void shouldReturnFalseForInvalidIds(String id) {
			assertThat(PatternUtils.isValidCalmId(id)).isFalse();
		}

		@Test
		@DisplayName("should return false for null")
		void shouldReturnFalseForNull() {
			assertThat(PatternUtils.isValidCalmId(null)).isFalse();
		}
	}

	@Nested
	@DisplayName("isInComment")
	class IsInComment {

		@Test
		@DisplayName("should return true for line starting with asterisk")
		void shouldReturnTrueForAsteriskLine() {
			String line = "* This is a comment with 6-123";

			assertThat(PatternUtils.isInComment(line, 28)).isTrue();
		}

		@Test
		@DisplayName("should return true for asterisk line with leading spaces")
		void shouldReturnTrueForAsteriskLineWithSpaces() {
			String line = "   * Comment 6-123";

			assertThat(PatternUtils.isInComment(line, 14)).isTrue();
		}

		@Test
		@DisplayName("should return true when preceded by quote")
		void shouldReturnTrueWhenPrecededByQuote() {
			String line = "DATA: lv_var TYPE string. \" See 6-123";

			assertThat(PatternUtils.isInComment(line, 33)).isTrue();
		}

		@Test
		@DisplayName("should return false for code line")
		void shouldReturnFalseForCodeLine() {
			String line = "DATA: lv_feature TYPE string VALUE '6-123'.";

			assertThat(PatternUtils.isInComment(line, 36)).isFalse();
		}

		@Test
		@DisplayName("should return false for null line")
		void shouldReturnFalseForNullLine() {
			assertThat(PatternUtils.isInComment(null, 0)).isFalse();
		}
	}

	@Nested
	@DisplayName("buildCloudAlmUrl")
	class BuildCloudAlmUrl {

		private static final String TENANT = "mytenant";
		private static final String REGION = "eu10";

		@Test
		@DisplayName("should build feature URL")
		void shouldBuildFeatureUrl() {
			String url = PatternUtils.buildCloudAlmUrl("6-1234", TENANT, REGION);

			assertThat(url).isEqualTo(
				"https://mytenant.eu10.alm.cloud.sap/launchpad#feature-display?sap-ui-app-id-hint=com.sap.calm.imp.cdm.features.ui&/details/6-1234"
			);
		}

		@Test
		@DisplayName("should build task URL")
		void shouldBuildTaskUrl() {
			String url = PatternUtils.buildCloudAlmUrl("3-5678", TENANT, REGION);

			assertThat(url).isEqualTo(
				"https://mytenant.eu10.alm.cloud.sap/launchpad#task-management?sap-app-origin-hint=&/taskDetail/3-5678"
			);
		}

		@Test
		@DisplayName("should build document URL")
		void shouldBuildDocumentUrl() {
			String url = PatternUtils.buildCloudAlmUrl("7-9012", TENANT, REGION);

			assertThat(url).isEqualTo(
				"https://mytenant.eu10.alm.cloud.sap/launchpad#DocumentationObject-manage?sap-ui-app-id-hint=com.sap.calm.imp.sd.docu.ui&/Documents('7-9012')"
			);
		}

		@Test
		@DisplayName("should build library URL")
		void shouldBuildLibraryUrl() {
			String url = PatternUtils.buildCloudAlmUrl("15-3456", TENANT, REGION);

			assertThat(url).isEqualTo(
				"https://mytenant.eu10.alm.cloud.sap/launchpad#library-management?sap-ui-app-id-hint=com.sap.calm.imp.lib.ui&/LibraryElement('15-3456')"
			);
		}

		@Test
		@DisplayName("should return null for invalid item ID")
		void shouldReturnNullForInvalidItemId() {
			assertThat(PatternUtils.buildCloudAlmUrl("invalid", TENANT, REGION)).isNull();
			assertThat(PatternUtils.buildCloudAlmUrl("1-123", TENANT, REGION)).isNull();
		}

		@Test
		@DisplayName("should return null for null parameters")
		void shouldReturnNullForNullParams() {
			assertThat(PatternUtils.buildCloudAlmUrl(null, TENANT, REGION)).isNull();
			assertThat(PatternUtils.buildCloudAlmUrl("6-123", null, REGION)).isNull();
			assertThat(PatternUtils.buildCloudAlmUrl("6-123", TENANT, null)).isNull();
		}

		@ParameterizedTest
		@DisplayName("should work with different regions")
		@CsvSource({
			"eu10, mytenant.eu10.alm.cloud.sap",
			"eu20, mytenant.eu20.alm.cloud.sap",
			"us10, mytenant.us10.alm.cloud.sap",
			"jp10, mytenant.jp10.alm.cloud.sap"
		})
		void shouldWorkWithDifferentRegions(String region, String expectedHost) {
			String url = PatternUtils.buildCloudAlmUrl("6-1", TENANT, region);

			assertThat(url).contains(expectedHost);
		}
	}

	@Nested
	@DisplayName("resolveVersionUri")
	class ResolveVersionUri {

		@Test
		@DisplayName("should resolve relative path without dot-slash")
		void shouldResolveRelativePathWithoutDotSlash() {
			String basePath = "/sap/bc/adt/classes/zcl_test";
			String versionsUrl = "source/main/versions";

			String result = PatternUtils.resolveVersionUri(basePath, versionsUrl);

			assertThat(result).isEqualTo("/sap/bc/adt/classes/zcl_test/source/main/versions");
		}

		@Test
		@DisplayName("should resolve relative path with dot-slash")
		void shouldResolveRelativePathWithDotSlash() {
			String basePath = "/sap/bc/adt/classes/zcl_test/source/main";
			String versionsUrl = "./versions";

			String result = PatternUtils.resolveVersionUri(basePath, versionsUrl);

			assertThat(result).isEqualTo("/sap/bc/adt/classes/zcl_test/source/versions");
		}

		@Test
		@DisplayName("should handle base path with trailing slash")
		void shouldHandleBasePathWithTrailingSlash() {
			String basePath = "/sap/bc/adt/classes/zcl_test/";
			String versionsUrl = "versions";

			String result = PatternUtils.resolveVersionUri(basePath, versionsUrl);

			assertThat(result).isEqualTo("/sap/bc/adt/classes/zcl_test/versions");
		}

		@Test
		@DisplayName("should return versionsUrl when basePath is null")
		void shouldReturnVersionsUrlWhenBasePathNull() {
			String versionsUrl = "some/path/versions";

			String result = PatternUtils.resolveVersionUri(null, versionsUrl);

			assertThat(result).isEqualTo(versionsUrl);
		}

		@Test
		@DisplayName("should return null when versionsUrl is null")
		void shouldReturnNullWhenVersionsUrlNull() {
			String result = PatternUtils.resolveVersionUri("/base/path", null);

			assertThat(result).isNull();
		}
	}

	@Nested
	@DisplayName("findCalmIds")
	class FindCalmIds {

		@Test
		@DisplayName("should find single Cloud ALM ID")
		void shouldFindSingleId() {
			String[] ids = PatternUtils.findCalmIds("Reference: 6-1234");

			assertThat(ids).containsExactly("6-1234");
		}

		@Test
		@DisplayName("should find multiple Cloud ALM IDs")
		void shouldFindMultipleIds() {
			String[] ids = PatternUtils.findCalmIds("Features 6-1234 and 6-5678, task 3-999");

			assertThat(ids).containsExactly("6-1234", "6-5678", "3-999");
		}

		@Test
		@DisplayName("should find all supported ID types")
		void shouldFindAllSupportedTypes() {
			String[] ids = PatternUtils.findCalmIds("Feature 6-1, Task 3-2, Doc 7-3, Lib 15-4");

			assertThat(ids).containsExactly("6-1", "3-2", "7-3", "15-4");
		}

		@Test
		@DisplayName("should return empty array when no IDs found")
		void shouldReturnEmptyWhenNoIds() {
			String[] ids = PatternUtils.findCalmIds("No IDs here");

			assertThat(ids).isEmpty();
		}

		@Test
		@DisplayName("should return empty array for null input")
		void shouldReturnEmptyForNull() {
			String[] ids = PatternUtils.findCalmIds(null);

			assertThat(ids).isEmpty();
		}
	}
}
