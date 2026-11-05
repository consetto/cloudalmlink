package com.consetto.adt.cloudalmlink.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.consetto.adt.cloudalmlink.model.VersionElement;

/**
 * Unit tests for {@link VersionFeedParser}: reading the ATOM versions feed of an ABAP object.
 */
@DisplayName("VersionFeedParser")
class VersionFeedParserTest {

	private static final String ATOM = "http://www.w3.org/2005/Atom";
	private static final String ADTCORE = "http://www.sap.com/adt/core";
	private static final String TRANSPORT_REL = "http://www.sap.com/adt/relations/transport/request";

	private static List<VersionElement> parseResource(String name) throws IOException, XMLStreamException {
		try (InputStream feed = VersionFeedParserTest.class.getResourceAsStream("/feeds/" + name)) {
			assertThat(feed).as("test resource " + name).isNotNull();
			return VersionFeedParser.parse(feed);
		}
	}

	private static List<VersionElement> parse(String xml) throws XMLStreamException {
		return VersionFeedParser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}

	/** A feed with the given entries, using the atom and adtcore prefixes as ADT does. */
	private static String feed(String entries) {
		return "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
				+ "<atom:feed xmlns:atom=\"" + ATOM + "\" xmlns:adtcore=\"" + ADTCORE + "\">"
				+ entries
				+ "</atom:feed>";
	}

	@Nested
	@DisplayName("class versions feed")
	class ClassFeed {

		@Test
		@DisplayName("should read one version per entry in feed order")
		void shouldReadEntriesInOrder() throws Exception {
			List<VersionElement> versions = parseResource("class-versions.xml");

			assertThat(versions).extracting(VersionElement::getID)
					.containsExactly("00002", "00000", "00001", "00003");
		}

		@Test
		@DisplayName("should read title, date, author and transport of an entry")
		void shouldReadEntryFields() throws Exception {
			VersionElement version = parseResource("class-versions.xml").get(0);

			assertThat(version.getTitle()).isEqualTo("6-1234: Änderung am Preis");
			assertThat(version.getLastUpdate()).isEqualTo("2026-06-23T11:23:41Z");
			assertThat(version.getAuthor()).isEqualTo("DEVELOPER2");
			assertThat(version.getTransportId()).isEqualTo("A4HK900002");
		}

		@Test
		@DisplayName("should not take the feed's own author, title or date")
		void shouldIgnoreFeedLevelElements() throws Exception {
			List<VersionElement> versions = parseResource("class-versions.xml");

			assertThat(versions).extracting(VersionElement::getAuthor).doesNotContain("FEEDOWNER");
			assertThat(versions).extracting(VersionElement::getTitle)
					.doesNotContain("Version List of ZCL_EXAMPLE (CLAS)");
		}

		@Test
		@DisplayName("should leave the transport of the active version empty")
		void shouldLeaveActiveWithoutTransport() throws Exception {
			VersionElement active = parseResource("class-versions.xml").get(1);

			assertThat(active.getID()).isEqualTo("00000");
			assertThat(active.getTransportId()).isNull();
			assertThat(active.getTitle()).isEmpty();
		}

		@Test
		@DisplayName("should take the transport from the transport link only and trim it")
		void shouldTakeTransportFromTransportLink() throws Exception {
			VersionElement version = parseResource("class-versions.xml").get(2);

			assertThat(version.getTransportId()).isEqualTo("A4HK9A21AJ");
		}

		@Test
		@DisplayName("should keep the ToC title so the original transport can be looked up")
		void shouldKeepTocTitle() throws Exception {
			VersionElement toc = parseResource("class-versions.xml").get(3);

			assertThat(toc.getTransportId()).isEqualTo("A4HK900010");
			assertThat(AdtResponseParser.extractTocTransportId(toc.getTitle())).isEqualTo("A4HK900001");
		}
	}

	@Nested
	@DisplayName("links")
	class Links {

		@Test
		@DisplayName("should ignore a link that is not a transport link although it has a name")
		void shouldIgnoreOtherLinks() throws Exception {
			List<VersionElement> versions = parse(feed("""
					<atom:entry><atom:id>00001</atom:id>
					<atom:link rel="http://www.sap.com/adt/relations/versions/content" adtcore:name="A4HK900001" href="x"/>
					</atom:entry>"""));

			assertThat(versions.get(0).getTransportId()).isNull();
		}

		@Test
		@DisplayName("should ignore a transport link without a name or with a blank one")
		void shouldIgnoreTransportLinkWithoutName() throws Exception {
			List<VersionElement> versions = parse(feed("""
					<atom:entry><atom:id>00001</atom:id>
					<atom:link rel="%1$s" href="/sap/bc/adt/cts/transportrequests/A4HK900001"/></atom:entry>
					<atom:entry><atom:id>00002</atom:id>
					<atom:link rel="%1$s" adtcore:name="  " href="x"/></atom:entry>""".formatted(TRANSPORT_REL)));

			assertThat(versions).extracting(VersionElement::getTransportId).containsOnlyNulls();
		}

		@Test
		@DisplayName("should accept the name attribute without a namespace")
		void shouldAcceptUnqualifiedName() throws Exception {
			List<VersionElement> versions = parse(feed("""
					<atom:entry><atom:id>00001</atom:id>
					<atom:link rel="%s" name="A4HK900001" href="x"/></atom:entry>""".formatted(TRANSPORT_REL)));

			assertThat(versions.get(0).getTransportId()).isEqualTo("A4HK900001");
		}
	}

	@Nested
	@DisplayName("feed shapes")
	class FeedShapes {

		@Test
		@DisplayName("should return an empty list for a feed without entries")
		void shouldReturnEmptyForNoEntries() throws Exception {
			assertThat(parse(feed(""))).isEmpty();
		}

		@Test
		@DisplayName("should read a feed that uses atom as the default namespace")
		void shouldReadDefaultNamespace() throws Exception {
			List<VersionElement> versions = parse("""
					<feed xmlns="%s"><entry>
					<author><name>DEVELOPER1</name></author><id>00001</id><title>first</title>
					</entry></feed>""".formatted(ATOM));

			assertThat(versions).singleElement().satisfies(version -> {
				assertThat(version.getID()).isEqualTo("00001");
				assertThat(version.getAuthor()).isEqualTo("DEVELOPER1");
				assertThat(version.getTitle()).isEqualTo("first");
			});
		}

		@Test
		@DisplayName("should take the author only from the name inside author")
		void shouldReadAuthorOnlyInsideAuthor() throws Exception {
			List<VersionElement> versions = parse(feed("""
					<atom:entry><atom:id>00001</atom:id>
					<atom:author><atom:name>DEVELOPER1</atom:name></atom:author>
					<atom:contributor><atom:name>OTHER</atom:name></atom:contributor>
					</atom:entry>"""));

			assertThat(versions.get(0).getAuthor()).isEqualTo("DEVELOPER1");
		}

		@Test
		@DisplayName("should read the encoding from the XML declaration")
		void shouldHonourDeclaredEncoding() throws Exception {
			String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"
					+ "<atom:feed xmlns:atom=\"" + ATOM + "\"><atom:entry><atom:title>Änderung</atom:title>"
					+ "</atom:entry></atom:feed>";

			List<VersionElement> versions = VersionFeedParser.parse(
					new ByteArrayInputStream(xml.getBytes(StandardCharsets.ISO_8859_1)));

			assertThat(versions.get(0).getTitle()).isEqualTo("Änderung");
		}
	}

	@Nested
	@DisplayName("refusals")
	class Refusals {

		@Test
		@DisplayName("should reject a feed that is not well-formed")
		void shouldRejectMalformedFeed() {
			assertThatThrownBy(() -> parse(feed("<atom:entry><atom:id>00001</atom:entry>")))
					.isInstanceOf(XMLStreamException.class);
		}

		@Test
		@DisplayName("should reject a DTD instead of resolving its external entities")
		void shouldRejectDtd() {
			String xml = """
					<?xml version="1.0"?>
					<!DOCTYPE feed [<!ENTITY secret SYSTEM "file:///etc/hosts">]>
					<feed xmlns="%s"><entry><title>&secret;</title></entry></feed>""".formatted(ATOM);

			assertThatThrownBy(() -> parse(xml)).isInstanceOf(XMLStreamException.class);
		}

		@Test
		@DisplayName("should reject a DTD without entities as well")
		void shouldRejectPlainDtd() {
			String xml = """
					<?xml version="1.0"?>
					<!DOCTYPE feed>
					<feed xmlns="%s"><entry><id>00001</id></entry></feed>""".formatted(ATOM);

			assertThatThrownBy(() -> parse(xml))
					.isInstanceOf(XMLStreamException.class)
					.hasMessageContaining("DTD");
		}
	}
}
