package com.consetto.adt.cloudalmlink.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link VersionUris}: which versions and transports endpoints an object gets.
 */
@DisplayName("VersionUris")
class VersionUrisTest {

	private static final String VERSIONS_REL = "http://www.sap.com/adt/relations/versions";

	@Nested
	@DisplayName("find")
	class Find {

		@Test
		@DisplayName("should take an absolute versions link as is")
		void shouldTakeAbsoluteVersionsLink() {
			var links = List.of(
					new AtomLink(VERSIONS_REL, "/sap/bc/adt/programs/programs/ztest/source/main/versions"),
					new AtomLink(VersionUris.TRANSPORT_REL, "/sap/bc/adt/programs/programs/ztest/transports"));

			var endpoints = VersionUris.find(links, "PROG/P", null, null);

			assertThat(endpoints.versionsUrl()).isEqualTo("/sap/bc/adt/programs/programs/ztest/source/main/versions");
			assertThat(endpoints.transportsUrl()).isEqualTo("/sap/bc/adt/programs/programs/ztest/transports");
		}

		@Test
		@DisplayName("should resolve a relative versions link against the uri= base path")
		void shouldResolveRelativeLinkAgainstUriParameter() {
			var links = List.of(
					new AtomLink("self", "/sap/bc/adt/navigation?uri=%2Fsap%2Fbc%2Fadt%2Foo%2Fclasses%2Fzcl_test"),
					new AtomLink(VERSIONS_REL, "includes/implementations/versions"));

			var endpoints = VersionUris.find(links, "CLAS/OC", null, null);

			assertThat(endpoints.versionsUrl())
					.isEqualTo("/sap/bc/adt/oo/classes/zcl_test/includes/implementations/versions");
			assertThat(endpoints.transportsUrl()).isNull();
		}

		@Test
		@DisplayName("should prefer the main include's versions link, whatever the link order")
		void shouldPreferMainInclude() {
			var links = List.of(
					new AtomLink("self", "/sap/bc/adt/x?uri=%2Fsap%2Fbc%2Fadt%2Foo%2Fclasses%2Fzcl_test"),
					new AtomLink(VERSIONS_REL, "includes/definitions/versions"),
					new AtomLink(VERSIONS_REL, "includes/main/versions"),
					new AtomLink(VERSIONS_REL, "includes/testclasses/versions"));

			var endpoints = VersionUris.find(links, "CLAS/OC", null, null);

			assertThat(endpoints.versionsUrl()).isEqualTo("/sap/bc/adt/oo/classes/zcl_test/includes/main/versions");
		}

		@Test
		@DisplayName("should use the class's main include when there is no versions link")
		void shouldDefaultForClass() {
			var endpoints = VersionUris.find(List.of(), "CLAS/OC", "/sap/bc/adt/oo/classes/zcl_test", null);

			assertThat(endpoints.versionsUrl()).isEqualTo("/sap/bc/adt/oo/classes/zcl_test/includes/main/versions");
		}

		@Test
		@DisplayName("should take the class from an include's location")
		void shouldDefaultForClassFromIncludeLocation() {
			var endpoints = VersionUris.find(List.of(), "CLAS/OO", null,
					"adt://A4H/sap/bc/adt/classlib/classes/zcl_test/includes/implementations");

			assertThat(endpoints.versionsUrl()).isEqualTo("/sap/bc/adt/oo/classes/zcl_test/includes/main/versions");
		}

		@Test
		@DisplayName("should fall back to <object>/versions for other types")
		void shouldDefaultToVersions() {
			var endpoints = VersionUris.find(List.of(), "TABL/DT", "/sap/bc/adt/ddic/tables/ztab", null);

			assertThat(endpoints.versionsUrl()).isEqualTo("/sap/bc/adt/ddic/tables/ztab/versions");
		}

		@Test
		@DisplayName("should hang a CDS view's feed off the object, from the editor's raw location URI")
		void shouldUseRawLocationUri() {
			var endpoints = VersionUris.find(List.of(), "DDLS/DF", null,
					"adt://A4H/sap/bc/adt/ddic/ddlsources/zi_test/source/main");

			assertThat(endpoints.versionsUrl()).isEqualTo("/sap/bc/adt/ddic/ddl/sources/zi_test/versions");
		}

		@ParameterizedTest
		@DisplayName("should use the feed of the object type when there is no versions link")
		@CsvSource({
			"PROG/P,  /sap/bc/adt/programs/programs/ztest,                 /sap/bc/adt/programs/programs/ztest/source/main/versions",
			"PROG/P,  /sap/bc/adt/programs/programs/ztest/source/main,     /sap/bc/adt/programs/programs/ztest/source/main/versions",
			"PROG/I,  /sap/bc/adt/programs/includes/ztest_top,             /sap/bc/adt/programs/includes/ztest_top/source/main/versions",
			"INTF/OI, /sap/bc/adt/oo/interfaces/zif_test,                  /sap/bc/adt/oo/interfaces/zif_test/source/main/versions",
			"FUGR/FF, /sap/bc/adt/functions/groups/zfg/fmodules/z_fm,      /sap/bc/adt/functions/groups/zfg/fmodules/z_fm/source/main/versions",
			"BDEF/BDO, /sap/bc/adt/bo/behaviordefinitions/zi_test,         /sap/bc/adt/bo/behaviordefinitions/zi_test/source/main/versions",
			"SRVD/SRV, /sap/bc/adt/ddic/srvd/sources/zsd_test,             /sap/bc/adt/ddic/srvd/sources/zsd_test/source/main/versions",
			"DDLS/DF, /sap/bc/adt/ddic/ddl/sources/zi_test/source/main,    /sap/bc/adt/ddic/ddl/sources/zi_test/versions",
			"DCLS/DL, /sap/bc/adt/acm/dcl/sources/zi_test,                 /sap/bc/adt/acm/dcl/sources/zi_test/versions"
		})
		void shouldUseFeedOfType(String objectType, String objectUri, String expected) {
			var endpoints = VersionUris.find(List.of(), objectType, objectUri, null);

			assertThat(endpoints.versionsUrl()).isEqualTo(expected);
		}

		@Test
		@DisplayName("should not take another object's path from a uri= link")
		void shouldSkipOtherObjectsPath() {
			var links = List.of(new AtomLink("http://www.sap.com/adt/relations/packages",
					"/sap/bc/adt/repository/informationsystem/search?uri=%2Fsap%2Fbc%2Fadt%2Fpackages%2Fzpkg"));

			var endpoints = VersionUris.find(links, "PROG/P", "/sap/bc/adt/programs/programs/ztest", null);

			assertThat(endpoints.versionsUrl()).isEqualTo("/sap/bc/adt/programs/programs/ztest/source/main/versions");
		}

		@Test
		@DisplayName("should ignore links without href")
		void shouldIgnoreLinksWithoutHref() {
			var links = List.of(new AtomLink(VERSIONS_REL, null));

			var endpoints = VersionUris.find(links, "PROG/P", "/sap/bc/adt/programs/programs/ztest", null);

			assertThat(endpoints.versionsUrl()).isEqualTo("/sap/bc/adt/programs/programs/ztest/source/main/versions");
		}
	}

	@Nested
	@DisplayName("resolveVersionUri")
	class ResolveVersionUri {

		@Test
		@DisplayName("should not repeat path segments the base already ends with")
		void shouldNotDuplicateSegments() {
			String result = VersionUris.resolveVersionUri("/sap/bc/adt/programs/programs/ztest/source/main",
					"./source/main/versions");

			assertThat(result).isEqualTo("/sap/bc/adt/programs/programs/ztest/source/main/versions");
		}
	}

	@Nested
	@DisplayName("resolveFromRawLocationUri")
	class ResolveFromRawLocationUri {

		@Test
		@DisplayName("should map classlib to oo for class implementations")
		void shouldMapClasslibToOo() {
			String result = VersionUris.resolveFromRawLocationUri(
					"adt://A4H/sap/bc/adt/classlib/classes/zcl_test/includes/implementations",
					"../versions", "CLAS/OC");

			assertThat(result).isEqualTo("/sap/bc/adt/oo/classes/zcl_test/versions");
		}

		@Test
		@DisplayName("should return the versions URL when the raw URI has no ADT path")
		void shouldReturnInputWithoutAdtPath() {
			assertThat(VersionUris.resolveFromRawLocationUri("file:/tmp/x", "versions", "PROG/P"))
					.isEqualTo("versions");
		}
	}
}
