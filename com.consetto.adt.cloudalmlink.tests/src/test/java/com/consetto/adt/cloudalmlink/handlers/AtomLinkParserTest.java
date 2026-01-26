package com.consetto.adt.cloudalmlink.handlers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.consetto.adt.cloudalmlink.handlers.AtomLinkParser.AtomLink;

/**
 * Unit tests for {@link AtomLinkParser}.
 * Tests XML atom link parsing extracted from AdtObjectContext.
 */
@DisplayName("AtomLinkParser")
class AtomLinkParserTest {

	@Nested
	@DisplayName("parseAtomLinks")
	class ParseAtomLinks {

		@Test
		@DisplayName("should parse atom:link elements with rel before href")
		void shouldParseAtomLinkWithRelBeforeHref() {
			String xml = """
				<entry xmlns:atom="http://www.w3.org/2005/Atom">
					<atom:link rel="http://www.sap.com/adt/relations/versions" href="/sap/bc/adt/classes/zcl_test/versions"/>
				</entry>
				""";

			List<AtomLink> links = AtomLinkParser.parseAtomLinks(xml);

			assertThat(links).hasSize(1);
			assertThat(links.get(0).getRel()).isEqualTo("http://www.sap.com/adt/relations/versions");
			assertThat(links.get(0).getHref()).isEqualTo("/sap/bc/adt/classes/zcl_test/versions");
		}

		@Test
		@DisplayName("should parse atom:link elements with href before rel")
		void shouldParseAtomLinkWithHrefBeforeRel() {
			String xml = """
				<entry xmlns:atom="http://www.w3.org/2005/Atom">
					<atom:link href="/sap/bc/adt/classes/zcl_test/transports" rel="http://www.sap.com/adt/relations/transport"/>
				</entry>
				""";

			List<AtomLink> links = AtomLinkParser.parseAtomLinks(xml);

			assertThat(links).hasSize(1);
			assertThat(links.get(0).getRel()).isEqualTo("http://www.sap.com/adt/relations/transport");
			assertThat(links.get(0).getHref()).isEqualTo("/sap/bc/adt/classes/zcl_test/transports");
		}

		@Test
		@DisplayName("should parse link elements without namespace prefix")
		void shouldParseLinkWithoutNamespace() {
			String xml = """
				<entry>
					<link rel="self" href="/sap/bc/adt/classes/zcl_test"/>
				</entry>
				""";

			List<AtomLink> links = AtomLinkParser.parseAtomLinks(xml);

			assertThat(links).hasSize(1);
			assertThat(links.get(0).getRel()).isEqualTo("self");
			assertThat(links.get(0).getHref()).isEqualTo("/sap/bc/adt/classes/zcl_test");
		}

		@Test
		@DisplayName("should parse multiple links")
		void shouldParseMultipleLinks() {
			String xml = """
				<entry xmlns:atom="http://www.w3.org/2005/Atom">
					<atom:link rel="self" href="/sap/bc/adt/classes/zcl_test"/>
					<atom:link rel="http://www.sap.com/adt/relations/versions" href="/sap/bc/adt/classes/zcl_test/versions"/>
					<atom:link rel="http://www.sap.com/adt/relations/transport" href="/sap/bc/adt/classes/zcl_test/transports"/>
				</entry>
				""";

			List<AtomLink> links = AtomLinkParser.parseAtomLinks(xml);

			assertThat(links).hasSize(3);
		}

		@Test
		@DisplayName("should handle self-closing link tags")
		void shouldHandleSelfClosingTags() {
			String xml = """
				<entry>
					<link rel="test" href="/path" />
				</entry>
				""";

			List<AtomLink> links = AtomLinkParser.parseAtomLinks(xml);

			assertThat(links).hasSize(1);
			assertThat(links.get(0).getRel()).isEqualTo("test");
			assertThat(links.get(0).getHref()).isEqualTo("/path");
		}

		@Test
		@DisplayName("should handle links with additional attributes")
		void shouldHandleLinksWithAdditionalAttributes() {
			String xml = """
				<entry>
					<atom:link type="application/xml" rel="versions" title="Versions" href="/versions"/>
				</entry>
				""";

			List<AtomLink> links = AtomLinkParser.parseAtomLinks(xml);

			assertThat(links).hasSize(1);
			assertThat(links.get(0).getRel()).isEqualTo("versions");
			assertThat(links.get(0).getHref()).isEqualTo("/versions");
		}

		@Test
		@DisplayName("should handle single quotes in attributes")
		void shouldHandleSingleQuotes() {
			String xml = """
				<entry>
					<link rel='test' href='/path'/>
				</entry>
				""";

			List<AtomLink> links = AtomLinkParser.parseAtomLinks(xml);

			assertThat(links).hasSize(1);
			assertThat(links.get(0).getRel()).isEqualTo("test");
			assertThat(links.get(0).getHref()).isEqualTo("/path");
		}

		@Test
		@DisplayName("should return empty list for null input")
		void shouldReturnEmptyForNull() {
			List<AtomLink> links = AtomLinkParser.parseAtomLinks(null);

			assertThat(links).isEmpty();
		}

		@Test
		@DisplayName("should return empty list for empty input")
		void shouldReturnEmptyForEmptyInput() {
			List<AtomLink> links = AtomLinkParser.parseAtomLinks("");

			assertThat(links).isEmpty();
		}

		@Test
		@DisplayName("should return empty list when no links found")
		void shouldReturnEmptyWhenNoLinks() {
			String xml = """
				<entry>
					<title>No links here</title>
				</entry>
				""";

			List<AtomLink> links = AtomLinkParser.parseAtomLinks(xml);

			assertThat(links).isEmpty();
		}

		@Test
		@DisplayName("should handle complex URIs with query parameters")
		void shouldHandleComplexUris() {
			String xml = """
				<entry>
					<atom:link rel="action" href="/sap/bc/adt/action?uri=%2Fsap%2Fbc%2Fadt%2Fclasses%2Fzcl_test&amp;param=value"/>
				</entry>
				""";

			List<AtomLink> links = AtomLinkParser.parseAtomLinks(xml);

			assertThat(links).hasSize(1);
			assertThat(links.get(0).getHref()).contains("uri=");
		}

		@Test
		@DisplayName("should be case insensitive for tag names")
		void shouldBeCaseInsensitive() {
			String xml = """
				<entry>
					<ATOM:LINK rel="test1" href="/path1"/>
					<Link rel="test2" href="/path2"/>
				</entry>
				""";

			List<AtomLink> links = AtomLinkParser.parseAtomLinks(xml);

			assertThat(links).hasSize(2);
		}
	}

	@Nested
	@DisplayName("extractPathFromRawUri")
	class ExtractPathFromRawUri {

		@Test
		@DisplayName("should extract path from ADT URI")
		void shouldExtractPathFromAdtUri() {
			String rawUri = "adt://NPL/sap/bc/adt/classes/zcl_test";

			String result = AtomLinkParser.extractPathFromRawUri(rawUri);

			assertThat(result).isEqualTo("/sap/bc/adt/classes/zcl_test");
		}

		@Test
		@DisplayName("should extract path from ADT URI with client")
		void shouldExtractPathFromAdtUriWithClient() {
			String rawUri = "adt://NPL.001/sap/bc/adt/programs/programs/ztest";

			String result = AtomLinkParser.extractPathFromRawUri(rawUri);

			assertThat(result).isEqualTo("/sap/bc/adt/programs/programs/ztest");
		}

		@Test
		@DisplayName("should stop at query parameters")
		void shouldStopAtQueryParams() {
			String rawUri = "adt://NPL/sap/bc/adt/classes/zcl_test?version=1";

			String result = AtomLinkParser.extractPathFromRawUri(rawUri);

			assertThat(result).isEqualTo("/sap/bc/adt/classes/zcl_test");
		}

		@Test
		@DisplayName("should return null when no ADT path found")
		void shouldReturnNullWhenNoAdtPath() {
			String rawUri = "file:///some/local/path";

			String result = AtomLinkParser.extractPathFromRawUri(rawUri);

			assertThat(result).isNull();
		}

		@Test
		@DisplayName("should return null for null input")
		void shouldReturnNullForNull() {
			assertThat(AtomLinkParser.extractPathFromRawUri(null)).isNull();
		}

		@Test
		@DisplayName("should handle various object types")
		void shouldHandleVariousObjectTypes() {
			assertThat(AtomLinkParser.extractPathFromRawUri("adt://SYS/sap/bc/adt/classes/zcl_class"))
				.isEqualTo("/sap/bc/adt/classes/zcl_class");

			assertThat(AtomLinkParser.extractPathFromRawUri("adt://SYS/sap/bc/adt/programs/programs/zprogram"))
				.isEqualTo("/sap/bc/adt/programs/programs/zprogram");

			assertThat(AtomLinkParser.extractPathFromRawUri("adt://SYS/sap/bc/adt/functions/groups/zfgroup"))
				.isEqualTo("/sap/bc/adt/functions/groups/zfgroup");

			assertThat(AtomLinkParser.extractPathFromRawUri("adt://SYS/sap/bc/adt/ddl/sources/zcds_view"))
				.isEqualTo("/sap/bc/adt/ddl/sources/zcds_view");
		}
	}

	@Nested
	@DisplayName("AtomLink")
	class AtomLinkTests {

		@Test
		@DisplayName("should store rel and href")
		void shouldStoreRelAndHref() {
			AtomLink link = new AtomLink("http://www.sap.com/adt/relations/versions", "/sap/bc/adt/versions");

			assertThat(link.getRel()).isEqualTo("http://www.sap.com/adt/relations/versions");
			assertThat(link.getHref()).isEqualTo("/sap/bc/adt/versions");
		}

		@Test
		@DisplayName("should handle null values")
		void shouldHandleNullValues() {
			AtomLink link = new AtomLink(null, null);

			assertThat(link.getRel()).isNull();
			assertThat(link.getHref()).isNull();
		}
	}

	@Nested
	@DisplayName("Real World XML Responses")
	class RealWorldXmlResponses {

		@Test
		@DisplayName("should parse typical ADT class response")
		void shouldParseTypicalAdtClassResponse() {
			String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<class:abapClass xmlns:class="http://www.sap.com/adt/oo/classes"
				                 xmlns:atom="http://www.w3.org/2005/Atom">
					<atom:link rel="self" href="/sap/bc/adt/oo/classes/zcl_example"/>
					<atom:link rel="http://www.sap.com/adt/relations/versions" href="/sap/bc/adt/oo/classes/zcl_example/source/main/versions"/>
					<atom:link rel="http://www.sap.com/adt/relations/transport" href="/sap/bc/adt/oo/classes/zcl_example/source/main/transports"/>
					<atom:link rel="http://www.sap.com/adt/relations/source" href="./source/main"/>
				</class:abapClass>
				""";

			List<AtomLink> links = AtomLinkParser.parseAtomLinks(xml);

			assertThat(links).hasSize(4);

			AtomLink versionsLink = links.stream()
				.filter(l -> l.getRel().contains("versions"))
				.findFirst()
				.orElse(null);
			assertThat(versionsLink).isNotNull();
			assertThat(versionsLink.getHref()).contains("versions");

			AtomLink transportLink = links.stream()
				.filter(l -> l.getRel().contains("transport"))
				.findFirst()
				.orElse(null);
			assertThat(transportLink).isNotNull();
			assertThat(transportLink.getHref()).contains("transports");
		}

		@Test
		@DisplayName("should parse typical ADT program response")
		void shouldParseTypicalAdtProgramResponse() {
			String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<program:abapProgram xmlns:program="http://www.sap.com/adt/programs/programs"
				                     xmlns:atom="http://www.w3.org/2005/Atom">
					<atom:link rel="self" href="/sap/bc/adt/programs/programs/ztest_program"/>
					<atom:link href="/sap/bc/adt/programs/programs/ztest_program/source/main/versions" rel="http://www.sap.com/adt/relations/versions"/>
				</program:abapProgram>
				""";

			List<AtomLink> links = AtomLinkParser.parseAtomLinks(xml);

			assertThat(links).hasSize(2);

			AtomLink versionsLink = links.stream()
				.filter(l -> l.getRel().contains("versions"))
				.findFirst()
				.orElse(null);
			assertThat(versionsLink).isNotNull();
			assertThat(versionsLink.getHref()).isEqualTo("/sap/bc/adt/programs/programs/ztest_program/source/main/versions");
		}
	}
}
