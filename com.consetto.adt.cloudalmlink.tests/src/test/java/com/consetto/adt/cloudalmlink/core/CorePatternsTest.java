package com.consetto.adt.cloudalmlink.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.consetto.adt.cloudalmlink.model.CloudAlmConfig;
import com.consetto.adt.cloudalmlink.model.CloudAlmItemType;

/**
 * Unit tests for the parsing rules in {@link AdtResponseParser}, {@link CalmIds}, {@link VersionUris}
 * and the URL building in {@link CloudAlmItemType}.
 */
@DisplayName("Core patterns")
class CorePatternsTest {

	private static String url(String itemId, String tenant, String region) {
		return CloudAlmItemType.getUrlForItem(itemId, new CloudAlmConfig(tenant, region, null, null));
	}

	private static String[] ids(String lineText) {
		return CalmIds.find(lineText).stream().map(CalmIds.Match::id).toArray(String[]::new);
	}

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

			String result = AdtResponseParser.extractTransportId(response);

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

			String result = AdtResponseParser.extractTransportId(response);

			assertThat(result).isEqualTo("DEVK912345");
		}

		@Test
		@DisplayName("should extract transport ID using general pattern")
		void shouldExtractUsingGeneralPattern() {
			String response = "Some text with transport S4DK911940 embedded";

			String result = AdtResponseParser.extractTransportId(response);

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
			String result = AdtResponseParser.extractTransportId(response);
			assertThat(result).isEqualTo(expected);
		}

		@Test
		@DisplayName("should read CORRNR from the response of an object's transports endpoint")
		void shouldExtractFromCorrnr() {
			// Shape of com.sap.adt.lock.result2; CORRTEXT may name another transport
			String response = """
				<?xml version="1.0" encoding="utf-8"?>
				<asx:abap version="1.0" xmlns:asx="http://www.sap.com/abapxml">
				  <asx:values>
				    <DATA>
				      <LOCK_HANDLE/>
				      <CORRNR>A4HK900123</CORRNR>
				      <CORRUSER>DEVELOPER</CORRUSER>
				      <CORRTEXT>Follow-up of A4HK900100</CORRTEXT>
				    </DATA>
				  </asx:values>
				</asx:abap>
				""";

			assertThat(AdtResponseParser.extractTransportId(response)).isEqualTo("A4HK900123");
		}

		@Test
		@DisplayName("should read CORRNR with letters in the number")
		void shouldExtractAlphanumericCorrnr() {
			assertThat(AdtResponseParser.extractTransportId("<DATA><CORRNR>T4DK9A21AJ</CORRNR></DATA>"))
					.isEqualTo("T4DK9A21AJ");
		}

		@ParameterizedTest
		@DisplayName("should accept letters in the number after its first digit")
		@CsvSource({
			"'>T4DK9A21AJ<', T4DK9A21AJ",
			"'Transport: YI3K8A1A7B', YI3K8A1A7B"
		})
		void shouldExtractAlphanumericNumbers(String response, String expected) {
			assertThat(AdtResponseParser.extractTransportId(response)).isEqualTo(expected);
		}

		@ParameterizedTest
		@DisplayName("should not take a transport number out of a longer word")
		@ValueSource(strings = { "XABCK900001", "ABCK900001X", "ABCKAAAAAA" })
		void shouldNotMatchInsideWords(String response) {
			assertThat(AdtResponseParser.extractTransportId(response)).isNull();
		}

		@Test
		@DisplayName("should return null for null input")
		void shouldReturnNullForNullInput() {
			assertThat(AdtResponseParser.extractTransportId(null)).isNull();
		}

		@Test
		@DisplayName("should return null when no transport ID found")
		void shouldReturnNullWhenNotFound() {
			String response = "No transport ID here";

			assertThat(AdtResponseParser.extractTransportId(response)).isNull();
		}

		@Test
		@DisplayName("should prioritize tm:request attribute pattern")
		void shouldPrioritizeTmRequestAttribute() {
			String response = """
				<transport tm:request="NPLK900001">
					<other>DEVK912345</other>
				</transport>
				""";

			String result = AdtResponseParser.extractTransportId(response);

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

			String result = AdtResponseParser.extractUriParameter(href);

			assertThat(result).isEqualTo("/sap/bc/adt/classes/zcl_test");
		}

		@Test
		@DisplayName("should extract URI parameter with additional query params")
		void shouldExtractWithAdditionalParams() {
			String href = "http://example.com?uri=%2Fpath%2Fto%2Fobject&other=value";

			String result = AdtResponseParser.extractUriParameter(href);

			assertThat(result).isEqualTo("/path/to/object");
		}

		@Test
		@DisplayName("should handle URI at end of string")
		void shouldHandleUriAtEnd() {
			String href = "http://example.com?param=value&uri=%2Fsap%2Fbc";

			String result = AdtResponseParser.extractUriParameter(href);

			assertThat(result).isEqualTo("/sap/bc");
		}

		@Test
		@DisplayName("should return null when no uri parameter")
		void shouldReturnNullWhenNoUriParam() {
			String href = "http://example.com?param=value";

			assertThat(AdtResponseParser.extractUriParameter(href)).isNull();
		}

		@Test
		@DisplayName("should return null for null input")
		void shouldReturnNullForNullInput() {
			assertThat(AdtResponseParser.extractUriParameter(null)).isNull();
		}
	}

	@Nested
	@DisplayName("extractFeatureIdFromDescription")
	class GetFeatureFromDescription {

		@Test
		@DisplayName("should extract feature ID from description")
		void shouldExtractFeatureId() {
			String descr = "6-1234: Fix payment bug";

			String result = AdtResponseParser.extractFeatureIdFromDescription(descr);

			assertThat(result).isEqualTo("6-1234");
		}

		@Test
		@DisplayName("should extract feature ID without colon")
		void shouldExtractFeatureIdWithoutColon() {
			String descr = "6-5678";

			String result = AdtResponseParser.extractFeatureIdFromDescription(descr);

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
			assertThat(AdtResponseParser.extractFeatureIdFromDescription(descr)).isEqualTo(expected);
		}

		@Test
		@DisplayName("should return null for non-feature prefix")
		void shouldReturnNullForNonFeaturePrefix() {
			String descr = "3-1234: This is a task, not a feature";

			assertThat(AdtResponseParser.extractFeatureIdFromDescription(descr)).isNull();
		}

		@Test
		@DisplayName("should return null for invalid format")
		void shouldReturnNullForInvalidFormat() {
			String descr = "Not a feature ID";

			assertThat(AdtResponseParser.extractFeatureIdFromDescription(descr)).isNull();
		}

		@ParameterizedTest
		@NullAndEmptySource
		@DisplayName("should return null for null or empty input")
		void shouldReturnNullForNullOrEmpty(String descr) {
			assertThat(AdtResponseParser.extractFeatureIdFromDescription(descr)).isNull();
		}
	}

	@Nested
	@DisplayName("isCalmId")
	class IsValidCalmId {

		@ParameterizedTest
		@DisplayName("should return true for valid Cloud ALM IDs")
		@ValueSource(strings = {"6-1", "6-123", "6-12345", "3-1", "3-999", "7-1", "7-42", "15-1", "15-9999"})
		void shouldReturnTrueForValidIds(String id) {
			assertThat(CalmIds.isCalmId(id)).isTrue();
		}

		@ParameterizedTest
		@DisplayName("should return false for invalid Cloud ALM IDs")
		@ValueSource(strings = {"1-123", "2-123", "4-123", "5-123", "8-123", "16-123", "abc", "6-", "-123", "6123"})
		void shouldReturnFalseForInvalidIds(String id) {
			assertThat(CalmIds.isCalmId(id)).isFalse();
		}

		@Test
		@DisplayName("should return false for null")
		void shouldReturnFalseForNull() {
			assertThat(CalmIds.isCalmId(null)).isFalse();
		}
	}

	@Nested
	@DisplayName("isInComment")
	class IsInComment {

		@Test
		@DisplayName("should return true for line starting with asterisk")
		void shouldReturnTrueForAsteriskLine() {
			String line = "* This is a comment with 6-123";

			assertThat(CalmIds.isInComment(line, 28)).isTrue();
		}

		@Test
		@DisplayName("should return true for asterisk line with leading spaces")
		void shouldReturnTrueForAsteriskLineWithSpaces() {
			String line = "   * Comment 6-123";

			assertThat(CalmIds.isInComment(line, 14)).isTrue();
		}

		@Test
		@DisplayName("should return true when preceded by quote")
		void shouldReturnTrueWhenPrecededByQuote() {
			String line = "DATA: lv_var TYPE string. \" See 6-123";

			assertThat(CalmIds.isInComment(line, 33)).isTrue();
		}

		@Test
		@DisplayName("should return false for code line")
		void shouldReturnFalseForCodeLine() {
			String line = "DATA: lv_feature TYPE string VALUE '6-123'.";

			assertThat(CalmIds.isInComment(line, 36)).isFalse();
		}

		@Test
		@DisplayName("should return false for null line")
		void shouldReturnFalseForNullLine() {
			assertThat(CalmIds.isInComment(null, 0)).isFalse();
		}
	}

	@Nested
	@DisplayName("CloudAlmItemType.getUrlForItem")
	class BuildCloudAlmUrl {

		private static final String TENANT = "mytenant";
		private static final String REGION = "eu10";

		@Test
		@DisplayName("should build feature URL")
		void shouldBuildFeatureUrl() {
			String url = url("6-1234", TENANT, REGION);

			assertThat(url).isEqualTo(
				"https://mytenant.eu10.alm.cloud.sap/launchpad#feature-display?sap-ui-app-id-hint=com.sap.calm.imp.cdm.features.ui&/details/6-1234"
			);
		}

		@Test
		@DisplayName("should build task URL")
		void shouldBuildTaskUrl() {
			String url = url("3-5678", TENANT, REGION);

			assertThat(url).isEqualTo(
				"https://mytenant.eu10.alm.cloud.sap/launchpad#task-management?sap-app-origin-hint=&/taskDetail/3-5678"
			);
		}

		@Test
		@DisplayName("should build document URL")
		void shouldBuildDocumentUrl() {
			String url = url("7-9012", TENANT, REGION);

			assertThat(url).isEqualTo(
				"https://mytenant.eu10.alm.cloud.sap/launchpad#DocumentationObject-manage?sap-ui-app-id-hint=com.sap.calm.imp.sd.docu.ui&/Documents('7-9012')"
			);
		}

		@Test
		@DisplayName("should build library URL")
		void shouldBuildLibraryUrl() {
			String url = url("15-3456", TENANT, REGION);

			assertThat(url).isEqualTo(
				"https://mytenant.eu10.alm.cloud.sap/launchpad#library-management?sap-ui-app-id-hint=com.sap.calm.imp.lib.ui&/LibraryElement('15-3456')"
			);
		}

		@Test
		@DisplayName("should return null for invalid item ID")
		void shouldReturnNullForInvalidItemId() {
			assertThat(url("invalid", TENANT, REGION)).isNull();
			assertThat(url("1-123", TENANT, REGION)).isNull();
		}

		@Test
		@DisplayName("should return null for null parameters")
		void shouldReturnNullForNullParams() {
			assertThat(url(null, TENANT, REGION)).isNull();
			assertThat(url("6-123", null, REGION)).isNull();
			assertThat(url("6-123", TENANT, null)).isNull();
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
			String url = url("6-1", TENANT, region);

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

			String result = VersionUris.resolveVersionUri(basePath, versionsUrl);

			assertThat(result).isEqualTo("/sap/bc/adt/classes/zcl_test/source/main/versions");
		}

		@Test
		@DisplayName("should resolve relative path with dot-slash")
		void shouldResolveRelativePathWithDotSlash() {
			String basePath = "/sap/bc/adt/classes/zcl_test/source/main";
			String versionsUrl = "./versions";

			String result = VersionUris.resolveVersionUri(basePath, versionsUrl);

			assertThat(result).isEqualTo("/sap/bc/adt/classes/zcl_test/source/versions");
		}

		@Test
		@DisplayName("should handle base path with trailing slash")
		void shouldHandleBasePathWithTrailingSlash() {
			String basePath = "/sap/bc/adt/classes/zcl_test/";
			String versionsUrl = "versions";

			String result = VersionUris.resolveVersionUri(basePath, versionsUrl);

			assertThat(result).isEqualTo("/sap/bc/adt/classes/zcl_test/versions");
		}

		@Test
		@DisplayName("should return versionsUrl when basePath is null")
		void shouldReturnVersionsUrlWhenBasePathNull() {
			String versionsUrl = "some/path/versions";

			String result = VersionUris.resolveVersionUri(null, versionsUrl);

			assertThat(result).isEqualTo(versionsUrl);
		}

		@Test
		@DisplayName("should return null when versionsUrl is null")
		void shouldReturnNullWhenVersionsUrlNull() {
			String result = VersionUris.resolveVersionUri("/base/path", null);

			assertThat(result).isNull();
		}
	}

	@Nested
	@DisplayName("CalmIds.find")
	class FindCalmIds {

		@Test
		@DisplayName("should find single Cloud ALM ID")
		void shouldFindSingleId() {
			String[] ids = ids("Reference: 6-1234");

			assertThat(ids).containsExactly("6-1234");
		}

		@Test
		@DisplayName("should find multiple Cloud ALM IDs")
		void shouldFindMultipleIds() {
			String[] ids = ids("Features 6-1234 and 6-5678, task 3-999");

			assertThat(ids).containsExactly("6-1234", "6-5678", "3-999");
		}

		@Test
		@DisplayName("should find all supported ID types")
		void shouldFindAllSupportedTypes() {
			String[] ids = ids("Feature 6-1, Task 3-2, Doc 7-3, Lib 15-4");

			assertThat(ids).containsExactly("6-1", "3-2", "7-3", "15-4");
		}

		@Test
		@DisplayName("should return empty array when no IDs found")
		void shouldReturnEmptyWhenNoIds() {
			String[] ids = ids("No IDs here");

			assertThat(ids).isEmpty();
		}

		@Test
		@DisplayName("should return empty array for null input")
		void shouldReturnEmptyForNull() {
			String[] ids = ids(null);

			assertThat(ids).isEmpty();
		}
	}

	@Nested
	@DisplayName("extractTocTransportId")
	class ExtractTocTransportId {

		@Test
		@DisplayName("should extract transport ID from ToC title")
		void shouldExtractTransportIdFromTocTitle() {
			String result = AdtResponseParser.extractTocTransportId("ToC from DEVK900042: Some description");

			assertThat(result).isEqualTo("DEVK900042");
		}

		@Test
		@DisplayName("should extract transport ID with space before colon")
		void shouldExtractWithSpaceBeforeColon() {
			String result = AdtResponseParser.extractTocTransportId("ToC from S4DK903536 : 6-3");

			assertThat(result).isEqualTo("S4DK903536");
		}

		@ParameterizedTest
		@DisplayName("should extract various transport ID formats")
		@CsvSource({
			"'ToC from NPLK900001: Description', NPLK900001",
			"'ToC from S4DK911940: Another desc', S4DK911940",
			"'ToC from XYZK000001: Test', XYZK000001",
			"'ToC from ABCK123456: Fix bug', ABCK123456",
			"'ToC from S4DK903536 : 6-3', S4DK903536"
		})
		void shouldExtractVariousFormats(String title, String expected) {
			assertThat(AdtResponseParser.extractTocTransportId(title)).isEqualTo(expected);
		}

		@Test
		@DisplayName("should return null for title without ToC prefix")
		void shouldReturnNullForNonTocTitle() {
			assertThat(AdtResponseParser.extractTocTransportId("Regular transport title")).isNull();
		}

		@Test
		@DisplayName("should return null for null input")
		void shouldReturnNullForNullInput() {
			assertThat(AdtResponseParser.extractTocTransportId(null)).isNull();
		}

		@Test
		@DisplayName("should return null for empty string")
		void shouldReturnNullForEmptyString() {
			assertThat(AdtResponseParser.extractTocTransportId("")).isNull();
		}

		@Test
		@DisplayName("should not match ToC in middle of title")
		void shouldNotMatchTocInMiddle() {
			assertThat(AdtResponseParser.extractTocTransportId("Something ToC from DEVK900042: desc")).isNull();
		}
	}

	@Nested
	@DisplayName("extractParentTransport")
	class ExtractParentTransport {

		@Test
		@DisplayName("should extract parent transport from task XML")
		void shouldExtractParentFromTaskXml() {
			String xml = """
				<?xml version="1.0" encoding="utf-8"?>
				<tm:root xmlns:tm="http://www.sap.com/adt/cts/transportrequests">
				  <tm:workbench tm:number="NPLK900042" tm:owner="DEVELOPER" tm:type="K">
				    <tm:task tm:number="NPLK900043" tm:parent="NPLK900042" tm:owner="DEVELOPER" tm:type="development"/>
				  </tm:workbench>
				</tm:root>
				""";

			String result = AdtResponseParser.extractParentTransport(xml, "NPLK900043");

			assertThat(result).isEqualTo("NPLK900042");
		}

		@Test
		@DisplayName("should return null when transport is a request, not a task")
		void shouldReturnNullWhenTransportIsRequest() {
			String xml = """
				<?xml version="1.0" encoding="utf-8"?>
				<tm:root xmlns:tm="http://www.sap.com/adt/cts/transportrequests">
				  <tm:workbench tm:number="NPLK900042" tm:owner="DEVELOPER" tm:type="K">
				    <tm:task tm:number="NPLK900043" tm:parent="NPLK900042" tm:owner="DEVELOPER" tm:type="development"/>
				  </tm:workbench>
				</tm:root>
				""";

			String result = AdtResponseParser.extractParentTransport(xml, "NPLK900042");

			assertThat(result).isNull();
		}

		@Test
		@DisplayName("should return null when tm:parent is empty")
		void shouldReturnNullWhenParentIsEmpty() {
			String xml = """
				<tm:root xmlns:tm="http://www.sap.com/adt/cts/transportrequests">
				  <tm:task tm:number="NPLK900043" tm:parent="" tm:owner="DEVELOPER"/>
				</tm:root>
				""";

			String result = AdtResponseParser.extractParentTransport(xml, "NPLK900043");

			assertThat(result).isNull();
		}

		@Test
		@DisplayName("should return null for null inputs")
		void shouldReturnNullForNullInputs() {
			assertThat(AdtResponseParser.extractParentTransport(null, "NPLK900043")).isNull();
			assertThat(AdtResponseParser.extractParentTransport("<xml/>", null)).isNull();
			assertThat(AdtResponseParser.extractParentTransport(null, null)).isNull();
		}

		@Test
		@DisplayName("should handle attribute ordering variations")
		void shouldHandleAttributeOrderingVariations() {
			String xml = """
				<tm:root xmlns:tm="http://www.sap.com/adt/cts/transportrequests">
				  <tm:task tm:parent="NPLK900042" tm:type="development" tm:number="NPLK900043" tm:owner="DEVELOPER"/>
				</tm:root>
				""";

			String result = AdtResponseParser.extractParentTransport(xml, "NPLK900043");

			assertThat(result).isEqualTo("NPLK900042");
		}

		@Test
		@DisplayName("should match correct task when multiple tasks exist")
		void shouldMatchCorrectTaskAmongMultiple() {
			String xml = """
				<tm:root xmlns:tm="http://www.sap.com/adt/cts/transportrequests">
				  <tm:workbench tm:number="NPLK900042" tm:owner="DEVELOPER" tm:type="K">
				    <tm:task tm:number="NPLK900043" tm:parent="NPLK900042" tm:owner="DEV1" tm:type="development"/>
				    <tm:task tm:number="NPLK900044" tm:parent="NPLK900042" tm:owner="DEV2" tm:type="development"/>
				  </tm:workbench>
				</tm:root>
				""";

			assertThat(AdtResponseParser.extractParentTransport(xml, "NPLK900044")).isEqualTo("NPLK900042");
			assertThat(AdtResponseParser.extractParentTransport(xml, "NPLK900043")).isEqualTo("NPLK900042");
			assertThat(AdtResponseParser.extractParentTransport(xml, "NPLK900099")).isNull();
		}
	}
}
