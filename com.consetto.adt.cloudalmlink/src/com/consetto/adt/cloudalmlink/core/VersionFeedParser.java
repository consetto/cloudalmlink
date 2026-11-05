package com.consetto.adt.cloudalmlink.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.consetto.adt.cloudalmlink.model.VersionElement;

/**
 * Reads the ATOM versions feed of an ABAP object into version elements.
 * Uses the JDK's StAX and is free of Eclipse dependencies so that it is unit-tested directly
 * (see the tests module).
 */
public final class VersionFeedParser {

	private VersionFeedParser() {
	}

	/**
	 * Parses a versions feed. Each entry becomes one version with its ID, title, date, author and
	 * the transport named by its transport link. Elements outside an entry are ignored.
	 * The feed comes from the backend and is untrusted: DTDs and external entities are not
	 * processed.
	 *
	 * @param feed The feed; its encoding is taken from the XML declaration (default UTF-8).
	 *            Not closed.
	 * @return The versions in feed order, never null
	 * @throws XMLStreamException If the feed is not well-formed or declares a DTD
	 */
	public static List<VersionElement> parse(InputStream feed) throws XMLStreamException {
		List<VersionElement> versions = new ArrayList<>();
		XMLStreamReader reader = createFactory().createXMLStreamReader(feed);
		try {
			VersionElement version = null;
			boolean inAuthor = false;
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.DTD) {
					throw new XMLStreamException("DTD in versions feed", reader.getLocation());
				}
				if (event == XMLStreamConstants.END_ELEMENT) {
					switch (reader.getLocalName()) {
					case "entry" -> version = null;
					case "author" -> inAuthor = false;
					default -> {
						// nothing to close
					}
					}
					continue;
				}
				if (event != XMLStreamConstants.START_ELEMENT) {
					continue;
				}
				String name = reader.getLocalName();
				if ("entry".equals(name)) {
					version = new VersionElement();
					versions.add(version);
				} else if (version != null) {
					switch (name) {
					case "id" -> version.setID(reader.getElementText());
					case "title" -> version.setTitle(reader.getElementText());
					case "updated" -> version.setLastUpdate(reader.getElementText());
					case "author" -> inAuthor = true;
					case "name" -> {
						if (inAuthor) {
							version.setAuthor(reader.getElementText());
						}
					}
					case "link" -> readTransportLink(reader, version);
					default -> {
						// content, category and other elements carry nothing the view shows
					}
					}
				}
			}
		} finally {
			reader.close();
		}
		return versions;
	}

	/**
	 * Sets the version's transport from a link whose relation is a transport relation. Its
	 * adtcore:name attribute holds the transport ID; a link without one is ignored.
	 */
	private static void readTransportLink(XMLStreamReader reader, VersionElement version) {
		if (!AdtResponseParser.isTransportRel(reader.getAttributeValue(null, "rel"))) {
			return;
		}
		// A null namespace matches adtcore:name as well as an unqualified name attribute
		String transportId = reader.getAttributeValue(null, "name");
		if (transportId != null && !transportId.isBlank()) {
			version.setTransport(transportId.trim());
		}
	}

	/** The JDK's own implementation, not whichever StAX provider the OSGi class path offers. */
	private static XMLInputFactory createFactory() {
		XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
		return factory;
	}
}
