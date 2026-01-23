package com.consetto.adt.cloudalmlink.handlers;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.consetto.adt.cloudalmlink.handlers.AdtObjectContext.AtomLink;
import com.consetto.adt.cloudalmlink.model.DemoDataProvider;
import com.consetto.adt.cloudalmlink.model.VersionData;
import com.consetto.adt.cloudalmlink.views.TransportView;
import com.sap.adt.communication.message.HeadersFactory;
import com.sap.adt.communication.message.IHeaders;
import com.sap.adt.communication.message.IHeaders.IField;
import com.sap.adt.communication.message.IMessageBody;
import com.sap.adt.communication.resources.AdtRestResourceFactory;
import com.sap.adt.communication.resources.IRestResource;
import com.sap.adt.communication.resources.IRestResourceFactory;
import com.sap.adt.destinations.ui.logon.AdtLogonServiceUIFactory;
import com.sap.adt.tools.core.ui.editors.IAdtFormEditor;

/**
 * Unified command handler for displaying transports and features associated with ABAP source code.
 * Handles both editor context (active ADT editor) and Project Explorer selection.
 * Retrieves version data from ADT using atom links and displays results in the TransportView.
 */
public class CalmSourceHandler extends AbstractHandler {

	private static final String TRANSPORT_REL = "http://www.sap.com/adt/relations/transport";
	private static final String VERSIONS_REL = "http://www.sap.com/adt/relations/versions";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		// Check if demo mode is enabled
		if (DemoDataProvider.isDemoModeEnabled()) {
			showDemoData(event);
			return null;
		}

		// Resolve object context from either editor or Project Explorer selection
		AdtObjectContext context = resolveObjectContext(event, window);
		if (context == null) {
			MessageDialog.openError(window.getShell(), "ADT Cloud ALM Link Error",
					"Could not determine ABAP object from editor or selection");
			return null;
		}

		// Extract URLs from atom links
		VersionUrls urls = extractVersionUrls(context);
		if (urls.versionsURL == null) {
			MessageDialog.openError(window.getShell(), "ADT Cloud ALM Link Error",
					"Could not find versions URL for this object");
			return null;
		}

		// Ensure user is logged on to the ABAP system
		AdtLogonServiceUIFactory.createLogonServiceUI().ensureLoggedOn(
				context.getAbapProject().getDestinationData(),
				PlatformUI.getWorkbench().getProgressService());

		String destination = context.getDestination();
		IRestResourceFactory restResourceFactory = AdtRestResourceFactory.createRestResourceFactory();

		// STEP 1: Fetch the ACTIVE version's transport from /transports endpoint
		String activeTransportId = fetchActiveTransport(urls.transportsURL, destination, restResourceFactory);

		// STEP 2: Fetch released versions from /versions endpoint
		VersionData versions = fetchVersions(urls.versionsURL, destination, restResourceFactory, window);

		// STEP 3: Add active transport as first entry if found
		if (versions != null && activeTransportId != null) {
			versions.addActiveVersion(activeTransportId);
		}

		// Display results in TransportView
		showTransportView(event, versions);

		return null;
	}

	/**
	 * Resolves the ADT object context from either editor or Project Explorer selection.
	 * Tries editor first, then falls back to selection.
	 *
	 * @param event The execution event
	 * @param window The workbench window
	 * @return The resolved context, or null if no valid context found
	 */
	private AdtObjectContext resolveObjectContext(ExecutionEvent event, IWorkbenchWindow window) {
		// Try editor first
		IAdtFormEditor editor = getActiveAdtEditor();
		if (editor != null) {
			AdtObjectContext context = AdtObjectContext.fromEditor(editor);
			if (context != null) {
				return context;
			}
		}

		// Fall back to Project Explorer selection
		ISelection selection = window.getSelectionService().getSelection();
		if (selection != null) {
			return AdtObjectContext.fromSelection(selection);
		}

		return null;
	}

	/**
	 * Gets the active ADT form editor if available.
	 */
	private IAdtFormEditor getActiveAdtEditor() {
		try {
			var activeEditor = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow()
					.getActivePage()
					.getActiveEditor();
			if (activeEditor instanceof IAdtFormEditor) {
				return (IAdtFormEditor) activeEditor;
			}
		} catch (Exception e) {
			// No active editor
		}
		return null;
	}

	/**
	 * Container for extracted version-related URLs.
	 */
	private static class VersionUrls {
		String versionsURL;
		String transportsURL;
		String adtBasePath;
	}

	/**
	 * Extracts version and transport URLs from the context's atom links.
	 *
	 * @param context The ADT object context
	 * @return Container with extracted URLs
	 */
	private VersionUrls extractVersionUrls(AdtObjectContext context) {
		VersionUrls urls = new VersionUrls();
		List<AtomLink> links = context.getAtomLinks();
		String type = context.getObjectType();

		for (AtomLink link : links) {
			String href = link.getHref();
			String rel = link.getRel();

			// Find versions endpoint
			if (href.contains("source/main/versions")
					|| href.contains("implementations/versions")
					|| href.contains("definitions/versions")
					|| (rel != null && rel.contains("relations/versions"))) {
				urls.versionsURL = href;
			}

			// Find transports endpoint (for active version)
			if (TRANSPORT_REL.equals(rel)) {
				urls.transportsURL = href;
			}

			// Extract base ADT path from uri= query parameter
			if (urls.adtBasePath == null && href.contains("uri=")) {
				urls.adtBasePath = extractUriParameter(href);
			}
		}

		// Handle class implementations that don't have a direct versions link
		if (urls.versionsURL == null && "CLAS/OC".equals(type)) {
			urls.versionsURL = "includes/implementations/versions";
		}

		// Handle class definitions
		if (urls.versionsURL == null && "CLAS/OO".equals(type)) {
			urls.versionsURL = "includes/definitions/versions";
		}

		// Resolve relative version URL to absolute path
		if (urls.versionsURL != null && !urls.versionsURL.startsWith("/")) {
			urls.versionsURL = resolveVersionUri(urls.adtBasePath, urls.versionsURL, context);
		}

		return urls;
	}

	/**
	 * Extracts and decodes the uri= query parameter from a link href.
	 *
	 * @param href The link href containing a uri= parameter
	 * @return The decoded URI value, or null if not found
	 */
	private String extractUriParameter(String href) {
		int uriStart = href.indexOf("uri=");
		if (uriStart == -1) {
			return null;
		}
		uriStart += 4; // Skip "uri="
		int uriEnd = href.indexOf("&", uriStart);
		if (uriEnd == -1) {
			uriEnd = href.length();
		}
		String encodedUri = href.substring(uriStart, uriEnd);
		return URLDecoder.decode(encodedUri, StandardCharsets.UTF_8);
	}

	/**
	 * Resolves a relative version URI to an absolute path.
	 *
	 * @param adtBasePath The base ADT path extracted from links (may be null)
	 * @param versionsURL The relative versions URL from links
	 * @param context The ADT object context
	 * @return The resolved version URI string
	 */
	private String resolveVersionUri(String adtBasePath, String versionsURL, AdtObjectContext context) {
		// Primary approach: use extracted base path from links
		if (adtBasePath != null) {
			URI baseUri;
			if (versionsURL.startsWith("./")) {
				// ./ means relative to parent directory - don't add trailing slash
				baseUri = URI.create(adtBasePath);
			} else {
				// No ./ prefix - add trailing slash so resolve appends to base path
				baseUri = URI.create(adtBasePath.endsWith("/") ? adtBasePath : adtBasePath + "/");
			}
			URI resolved = baseUri.resolve(versionsURL);
			return resolved.getPath();
		}

		// Secondary approach: use object URI from context
		String objectUri = context.getObjectUri();
		if (objectUri != null) {
			URI baseUri = URI.create(objectUri.endsWith("/") ? objectUri : objectUri + "/");
			URI resolved = baseUri.resolve(versionsURL);
			return resolved.normalize().getPath();
		}

		// Fallback for editor context: use raw location URI (original logic)
		String rawLocationUri = context.getRawLocationUri();
		if (rawLocationUri != null) {
			return resolveFromRawLocationUri(rawLocationUri, versionsURL, context.getObjectType());
		}

		// Last resort: return as-is
		return versionsURL;
	}

	/**
	 * Resolves version URI using the raw location URI from editor (original legacy logic).
	 *
	 * @param rawLocationUri The raw location URI from the editor
	 * @param versionsURL The relative versions URL
	 * @param type The object type
	 * @return The resolved version URI string
	 */
	private String resolveFromRawLocationUri(String rawLocationUri, String versionsURL, String type) {
		// Extract path from raw URI: adt://DEST/sap/bc/adt/... or adt://DEST.client/sap/bc/adt/...
		int dotIndex = rawLocationUri.indexOf(".");
		int slashIndex = rawLocationUri.lastIndexOf("/");

		String fileName;
		if (dotIndex > 0 && dotIndex < slashIndex) {
			// Has a dot before the last slash - extract from after the dot
			fileName = rawLocationUri.substring(dotIndex + 1, slashIndex + 1);
		} else {
			// No dot - try to extract /sap/bc/adt path
			int sapIndex = rawLocationUri.indexOf("/sap/bc/adt/");
			if (sapIndex != -1) {
				// Extract from /sap/bc/adt/ to last slash, then remove leading /sap/bc/
				String fullPath = rawLocationUri.substring(sapIndex, slashIndex + 1);
				fileName = fullPath.substring(8); // Remove "/sap/bc/" prefix to get "adt/..."
			} else {
				// Cannot extract - return versionsURL as-is
				return versionsURL;
			}
		}

		// Replace DDIC path segments for CDS views
		fileName = fileName.replace("/ddlsources/", "/ddl/sources/");
		fileName = fileName.replace("/ddlxsources/", "/ddlx/sources/");
		fileName = fileName.replace("/dclsources/", "/dcl/sources/");

		String versionURIString = "/sap/bc/" + fileName + versionsURL;

		// Adjust URI path for class implementations
		if ("CLAS/OC".equals(type) && !versionURIString.contains("/adt/oo")) {
			versionURIString = versionURIString.replace("/adt/classlib", "/adt/oo");
		}

		// Normalize the URI to handle ./ and ../ path segments
		return URI.create(versionURIString).normalize().getPath();
	}

	/**
	 * Fetches the active transport ID from the transports endpoint.
	 *
	 * @param transportsURL The transports endpoint URL
	 * @param destination The ABAP destination ID
	 * @param restResourceFactory The REST resource factory
	 * @return The transport ID if found, null otherwise
	 */
	private String fetchActiveTransport(String transportsURL, String destination,
			IRestResourceFactory restResourceFactory) {
		if (transportsURL == null) {
			return null;
		}

		try {
			URI transportUri = URI.create(transportsURL);
			IRestResource transportResource = restResourceFactory.createResourceWithStatelessSession(
					transportUri, destination);

			IHeaders transportHeader = HeadersFactory.newHeaders();
			IField transportAcceptField = HeadersFactory.newField("Accept", "application/vnd.sap.as+xml");
			transportHeader.setField(transportAcceptField);

			IMessageBody transportBody = transportResource.get(null, transportHeader, IMessageBody.class);
			if (transportBody != null) {
				byte[] bytes = transportBody.getContent().readAllBytes();
				String transportResponse = new String(bytes, StandardCharsets.UTF_8);
				return extractTransportId(transportResponse);
			}
		} catch (Exception e) {
			// Transport fetch failed - continue without active version
		}

		return null;
	}

	/**
	 * Extracts transport ID from transport response using multiple regex patterns.
	 *
	 * @param transportResponse The raw transport response
	 * @return The transport ID if found, null otherwise
	 */
	private String extractTransportId(String transportResponse) {
		if (transportResponse == null) {
			return null;
		}

		// Pattern 1: Look for tm:request attribute
		Pattern pattern1 = Pattern.compile("tm:request=\"([A-Z0-9]+)\"");
		Matcher matcher1 = pattern1.matcher(transportResponse);
		if (matcher1.find()) {
			return matcher1.group(1);
		}

		// Pattern 2: Look for transport request element content
		Pattern pattern2 = Pattern.compile(">([A-Z][A-Z0-9]{2}K\\d{6})<");
		Matcher matcher2 = pattern2.matcher(transportResponse);
		if (matcher2.find()) {
			return matcher2.group(1);
		}

		// Pattern 3: General SAP transport pattern anywhere in response
		Pattern pattern3 = Pattern.compile("[A-Z][A-Z0-9]{2}K\\d{6}");
		Matcher matcher3 = pattern3.matcher(transportResponse);
		if (matcher3.find()) {
			return matcher3.group();
		}

		return null;
	}

	/**
	 * Fetches version data from the versions endpoint.
	 *
	 * @param versionsURL The versions endpoint URL
	 * @param destination The ABAP destination ID
	 * @param restResourceFactory The REST resource factory
	 * @param window The workbench window for error dialogs
	 * @return The parsed version data, or null on error
	 */
	private VersionData fetchVersions(String versionsURL, String destination,
			IRestResourceFactory restResourceFactory, IWorkbenchWindow window) {
		try {
			URI versionUri = URI.create(versionsURL);
			IRestResource versionResource = restResourceFactory.createResourceWithStatelessSession(
					versionUri, destination);

			VersionDataContentHandler versionHandler = new VersionDataContentHandler();
			versionResource.addContentHandler(versionHandler);

			IHeaders requestHeader = HeadersFactory.newHeaders();
			IField acceptField = HeadersFactory.newField("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
			requestHeader.setField(acceptField);

			return (VersionData) versionResource.get(null, requestHeader, VersionData.class);
		} catch (RuntimeException e) {
			MessageDialog.openError(window.getShell(), "ADT Cloud ALM Link Error",
					"An exception occurred reading the versions: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Shows demo data in the TransportView.
	 */
	private void showDemoData(ExecutionEvent event) {
		try {
			IWorkbenchPage workbenchPage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
			workbenchPage.showView("com.consetto.adt.cloudalmlink.views.TransportView");
			TransportView transportView = (TransportView) workbenchPage
					.findView("com.consetto.adt.cloudalmlink.views.TransportView");
			transportView.setDemoData(DemoDataProvider.getDemoVersions());
		} catch (PartInitException e) {
			// View could not be opened - fail silently
		}
	}

	/**
	 * Displays the version data in the TransportView.
	 */
	private void showTransportView(ExecutionEvent event, VersionData versions) {
		try {
			IWorkbenchPage workbenchPage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
			workbenchPage.showView("com.consetto.adt.cloudalmlink.views.TransportView");
			TransportView transportView = (TransportView) workbenchPage
					.findView("com.consetto.adt.cloudalmlink.views.TransportView");
			transportView.setVersionData(versions);
		} catch (PartInitException e) {
			// View could not be opened - fail silently
		}
	}
}
