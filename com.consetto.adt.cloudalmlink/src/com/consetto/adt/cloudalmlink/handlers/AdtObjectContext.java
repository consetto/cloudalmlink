package com.consetto.adt.cloudalmlink.handlers;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;

import com.sap.adt.communication.message.HeadersFactory;
import com.sap.adt.communication.message.IHeaders;
import com.sap.adt.communication.message.IHeaders.IField;
import com.sap.adt.communication.message.IMessageBody;
import com.sap.adt.communication.resources.AdtRestResourceFactory;
import com.sap.adt.communication.resources.IRestResource;
import com.sap.adt.communication.resources.IRestResourceFactory;
import com.sap.adt.destinations.ui.logon.AdtLogonServiceUIFactory;
import com.sap.adt.project.IAdtCoreProject;
import com.sap.adt.project.ui.util.ProjectUtil;
import com.sap.adt.tools.core.IAdtObjectReference;
import com.sap.adt.tools.core.project.IAbapProject;
import com.sap.adt.tools.core.ui.editors.IAdtFormEditor;

/**
 * Encapsulates ADT object resolution from either editor or Project Explorer selection.
 * Provides unified access to object metadata including atom links for both contexts.
 */
public class AdtObjectContext {

	private IProject project;
	private IAbapProject abapProject;
	private String objectUri;
	private String objectType;
	private List<AtomLink> atomLinks;
	private String destination;
	private String rawLocationUri;  // For editor context: the raw file location URI

	/**
	 * Simple representation of an atom link.
	 */
	public static class AtomLink {
		private final String rel;
		private final String href;

		public AtomLink(String rel, String href) {
			this.rel = rel;
			this.href = href;
		}

		public String getRel() {
			return rel;
		}

		public String getHref() {
			return href;
		}
	}

	private AdtObjectContext() {
		this.atomLinks = new ArrayList<>();
	}

	/**
	 * Creates context from an active ADT editor.
	 * The editor already has atom links available from its model.
	 *
	 * @param editor The active ADT form editor
	 * @return The object context, or null if extraction failed
	 */
	public static AdtObjectContext fromEditor(IAdtFormEditor editor) {
		if (editor == null || editor.getModel() == null) {
			return null;
		}

		AdtObjectContext context = new AdtObjectContext();

		try {
			context.project = editor.getModelFile().getProject();
			context.abapProject = context.project.getAdapter(IAbapProject.class);
			if (context.abapProject == null) {
				return null;
			}

			context.objectType = editor.getModel().getType();
			context.destination = context.abapProject.getDestinationId();

			// Store the raw location URI for fallback path resolution
			context.rawLocationUri = editor.getModelFile().getRawLocationURI().toString();

			// Extract atom links from editor model
			for (com.sap.adt.tools.core.model.atom.IAtomLink link : editor.getModel().getLinks()) {
				context.atomLinks.add(new AtomLink(link.getRel(), link.getHref()));
			}

			// Extract object URI from atom links (from uri= parameter)
			context.objectUri = extractObjectUriFromLinks(context.atomLinks);
			if (context.objectUri == null) {
				// Fallback: try to get from editor file location
				context.objectUri = extractPathFromRawUri(context.rawLocationUri);
			}

			return context;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Creates context from a Project Explorer selection.
	 * Fetches atom links via REST API call to the object's ADT endpoint.
	 *
	 * @param selection The workbench selection
	 * @return The object context, or null if extraction failed
	 */
	public static AdtObjectContext fromSelection(ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			return null;
		}

		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		if (structuredSelection.isEmpty()) {
			return null;
		}

		AdtObjectContext context = new AdtObjectContext();

		try {
			// Get project using ADT ProjectUtil
			context.project = ProjectUtil.getActiveAdtCoreProject(selection, null, null,
					IAdtCoreProject.ABAP_PROJECT_NATURE);
			if (context.project == null) {
				return null;
			}

			context.abapProject = context.project.getAdapter(IAbapProject.class);
			if (context.abapProject == null) {
				return null;
			}

			// Get the selected ADT object reference
			Object selectedElement = structuredSelection.getFirstElement();
			IAdtObjectReference adtObjectRef = null;
			if (selectedElement instanceof IAdtObjectReference) {
				adtObjectRef = (IAdtObjectReference) selectedElement;
			} else if (selectedElement instanceof IAdaptable) {
				adtObjectRef = ((IAdaptable) selectedElement).getAdapter(IAdtObjectReference.class);
			}

			if (adtObjectRef == null) {
				return null;
			}

			context.objectUri = adtObjectRef.getUri().toString();
			context.objectType = adtObjectRef.getType();
			context.destination = context.abapProject.getDestinationId();

			// Ensure user is logged on before making REST call
			AdtLogonServiceUIFactory.createLogonServiceUI().ensureLoggedOn(
					context.abapProject.getDestinationData(),
					PlatformUI.getWorkbench().getProgressService());

			// Fetch atom links via REST API
			context.atomLinks = fetchAtomLinks(context.objectUri, context.destination);

			return context;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Fetches atom links for an object via REST API call.
	 *
	 * @param objectUri The ADT object URI
	 * @param destination The ABAP destination ID
	 * @return List of atom links parsed from the response
	 */
	private static List<AtomLink> fetchAtomLinks(String objectUri, String destination) {
		List<AtomLink> links = new ArrayList<>();

		try {
			IRestResourceFactory restResourceFactory = AdtRestResourceFactory.createRestResourceFactory();
			URI uri = URI.create(objectUri);
			IRestResource resource = restResourceFactory.createResourceWithStatelessSession(uri, destination);

			IHeaders headers = HeadersFactory.newHeaders();
			IField acceptField = HeadersFactory.newField("Accept", "application/atom+xml,application/xml");
			headers.setField(acceptField);

			IMessageBody body = resource.get(null, headers, IMessageBody.class);
			if (body != null) {
				InputStream content = body.getContent();
				byte[] bytes = content.readAllBytes();
				String response = new String(bytes, StandardCharsets.UTF_8);

				// Parse atom links from XML response
				links = parseAtomLinks(response);
			}
		} catch (Exception e) {
			// Failed to fetch atom links - return empty list
		}

		return links;
	}

	/**
	 * Parses atom link elements from an XML response.
	 *
	 * @param xmlResponse The XML response string
	 * @return List of parsed atom links
	 */
	private static List<AtomLink> parseAtomLinks(String xmlResponse) {
		List<AtomLink> links = new ArrayList<>();

		// Pattern to match atom:link or link elements with rel and href attributes
		// Handles both orders: rel before href and href before rel
		Pattern linkPattern = Pattern.compile(
				"<(?:atom:)?link[^>]*\\s(?:rel=[\"']([^\"']*)[\"'][^>]*href=[\"']([^\"']*)[\"']|href=[\"']([^\"']*)[\"'][^>]*rel=[\"']([^\"']*)[\"'])[^>]*/?>",
				Pattern.CASE_INSENSITIVE);

		Matcher matcher = linkPattern.matcher(xmlResponse);
		while (matcher.find()) {
			String rel, href;
			if (matcher.group(1) != null) {
				// rel before href
				rel = matcher.group(1);
				href = matcher.group(2);
			} else {
				// href before rel
				href = matcher.group(3);
				rel = matcher.group(4);
			}

			if (rel != null && href != null) {
				links.add(new AtomLink(rel, href));
			}
		}

		return links;
	}

	/**
	 * Extracts the object URI from atom links by looking for uri= parameter.
	 */
	private static String extractObjectUriFromLinks(List<AtomLink> links) {
		for (AtomLink link : links) {
			String href = link.getHref();
			if (href != null && href.contains("uri=")) {
				int uriStart = href.indexOf("uri=") + 4;
				int uriEnd = href.indexOf("&", uriStart);
				if (uriEnd == -1) {
					uriEnd = href.length();
				}
				String encodedUri = href.substring(uriStart, uriEnd);
				try {
					return java.net.URLDecoder.decode(encodedUri, StandardCharsets.UTF_8);
				} catch (Exception e) {
					// Decode failed
				}
			}
		}
		return null;
	}

	/**
	 * Extracts the ADT path from a raw URI string.
	 */
	private static String extractPathFromRawUri(String rawUri) {
		// Format: adt://DEST/sap/bc/adt/...
		int adtIndex = rawUri.indexOf("/sap/bc/adt/");
		if (adtIndex != -1) {
			// Find the end of the path (before any query params)
			int endIndex = rawUri.indexOf("?", adtIndex);
			if (endIndex == -1) {
				endIndex = rawUri.length();
			}
			return rawUri.substring(adtIndex, endIndex);
		}
		return null;
	}

	// Getters

	public IProject getProject() {
		return project;
	}

	public IAbapProject getAbapProject() {
		return abapProject;
	}

	public String getObjectUri() {
		return objectUri;
	}

	public String getObjectType() {
		return objectType;
	}

	public List<AtomLink> getAtomLinks() {
		return atomLinks;
	}

	public String getDestination() {
		return destination;
	}

	public String getRawLocationUri() {
		return rawLocationUri;
	}
}
