package com.consetto.adt.cloudalmlink.handlers;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

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
import com.sap.adt.tools.core.model.atom.IAtomLink;
import com.sap.adt.tools.core.project.IAbapProject;
import com.sap.adt.tools.core.ui.editors.IAdtFormEditor;

/**
 * Command handler for displaying transports and features associated with ABAP source code.
 * Retrieves version data from ADT and displays results in the TransportView.
 */
public class CalmSourceHandler extends AbstractHandler {

	private static final String TRANSPORT_REL = "http://www.sap.com/adt/relations/transport";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		// Get the active ADT editor and extract source information
		IAdtFormEditor editor = (IAdtFormEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();

		IProject project = editor.getModelFile().getProject();
		IAbapProject abapProject = (IAbapProject) project.getAdapter(IAbapProject.class);

		EList<IAtomLink> links = editor.getModel().getLinks();
		String type = editor.getModel().getType();

		// Find the versions URL and transports URL from available links
		String versionsURL = null;
		String transportsURL = null;
		String adtBasePath = null;

		for (IAtomLink link : links) {
			// Find versions endpoint
			if (link.getHref().contains("source/main/versions")
					|| link.getHref().contains("implementations/versions")
					|| link.getRel().contains("relations/versions")) {
				versionsURL = link.getHref();
			}
			// Find transports endpoint (for active version)
			if (TRANSPORT_REL.equals(link.getRel())) {
				transportsURL = link.getHref();
			}
			// Extract base ADT path from uri= query parameter (e.g., abaplanguageversions link)
			if (adtBasePath == null && link.getHref().contains("uri=")) {
				adtBasePath = extractUriParameter(link.getHref());
			}
		}

		// Handle class implementations that don't have a direct versions link
		if (versionsURL == null && type.equals("CLAS/OC")) {
			versionsURL = "includes/implementations/versions";
		}

		// Exit if no versions URL could be determined
		if (versionsURL == null) {
			MessageDialog.openError(window.getShell(), "ADT Cloud ALM Link Error",
					"Could not find versions URL for this object");
			return null;
		}

		// Construct the version URI - prefer using extracted base path
		String versionURIString = resolveVersionUri(adtBasePath, versionsURL, editor, type);

		// Create REST resource factory
		IRestResourceFactory restResourceFactory = AdtRestResourceFactory.createRestResourceFactory();

		// Ensure user is logged on to the ABAP system
		AdtLogonServiceUIFactory.createLogonServiceUI().ensureLoggedOn(abapProject.getDestinationData(),
				PlatformUI.getWorkbench().getProgressService());

		String destination = abapProject.getDestinationId();


		// STEP 1: Fetch the ACTIVE version's transport from /transports endpoint
		String activeTransportId = null;
		if (transportsURL != null) {
			try {
				URI transportUri = URI.create(transportsURL);
				IRestResource transportResource = restResourceFactory.createResourceWithStatelessSession(transportUri, destination);

				IHeaders transportHeader = HeadersFactory.newHeaders();
				IField transportAcceptField = HeadersFactory.newField("Accept", "application/vnd.sap.as+xml");
				transportHeader.setField(transportAcceptField);

				// Get response as IMessageBody to read raw content
				IMessageBody transportBody = transportResource.get(null, transportHeader, IMessageBody.class);

				if (transportBody != null) {
					// Read raw content from message body
					byte[] bytes = transportBody.getContent().readAllBytes();
					String transportResponse = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

					// Parse transport ID using regex patterns
					// Pattern 1: Look for tm:request attribute
					Pattern pattern1 = Pattern.compile("tm:request=\"([A-Z0-9]+)\"");
					Matcher matcher1 = pattern1.matcher(transportResponse);
					if (matcher1.find()) {
						activeTransportId = matcher1.group(1);
					}

					// Pattern 2: Look for transport request element content
					if (activeTransportId == null) {
						Pattern pattern2 = Pattern.compile(">([A-Z][A-Z0-9]{2}K\\d{6})<");
						Matcher matcher2 = pattern2.matcher(transportResponse);
						if (matcher2.find()) {
							activeTransportId = matcher2.group(1);
						}
					}

					// Pattern 3: General SAP transport pattern anywhere in response
					if (activeTransportId == null) {
						Pattern pattern3 = Pattern.compile("[A-Z][A-Z0-9]{2}K\\d{6}");
						Matcher matcher3 = pattern3.matcher(transportResponse);
						if (matcher3.find()) {
							activeTransportId = matcher3.group();
						}
					}
				}
			} catch (Exception e) {
				// Transport fetch failed - continue without active version
			}
		}
		
		// STEP 2: Fetch released versions from /versions endpoint
		URI versionUri = URI.create(versionURIString);
		IRestResource versionResource = restResourceFactory.createResourceWithStatelessSession(versionUri, destination);

		// Configure content handler for version data response
		VersionDataContentHandler versionHandler = new VersionDataContentHandler();
		versionResource.addContentHandler(versionHandler);

		// Set Accept header for ATOM+XML response
		IHeaders requestHeader = HeadersFactory.newHeaders();
		IField acceptField = HeadersFactory.newField("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
		requestHeader.setField(acceptField);

		VersionData versions = null;
		try {
			versions = (VersionData) versionResource.get(null, requestHeader, VersionData.class);
		} catch (RuntimeException e) {
			MessageDialog.openError(window.getShell(), "ADT Cloud ALM Link Error",
					"An exception occurred reading the versions: " + e.getMessage());
		}

		// STEP 3: Add active transport as first entry if found
		if (versions != null && activeTransportId != null) {
			versions.addActiveVersion(activeTransportId);
		}

		// Display results in TransportView
		IWorkbenchPage workbenchPage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
		try {
			workbenchPage.showView("com.consetto.adt.cloudalmlink.views.TransportView");
			TransportView transportView = (TransportView) workbenchPage
					.findView("com.consetto.adt.cloudalmlink.views.TransportView");
			transportView.setVersionData(versions);
		} catch (PartInitException e) {
			// View could not be opened - fail silently
		}

		return null;
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
	 * Resolves the version URI using the extracted base path or falls back to legacy approach.
	 *
	 * @param adtBasePath The base ADT path extracted from links (may be null)
	 * @param versionsURL The relative or absolute versions URL from links
	 * @param editor The ADT editor for fallback path extraction
	 * @param type The object type for special case handling
	 * @return The resolved version URI string
	 */
	private String resolveVersionUri(String adtBasePath, String versionsURL, IAdtFormEditor editor, String type) {
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

		// Fallback: legacy approach using getRawLocationURI
		String adtUrl = editor.getModelFile().getRawLocationURI().toString();
		String fileName = adtUrl.substring(adtUrl.indexOf(".") + 1, adtUrl.lastIndexOf("/") + 1);

		// Replace DDIC path segments for CDS views
		fileName = fileName.replace("/ddlsources/", "/ddl/sources/");
		fileName = fileName.replace("/ddlxsources/", "/ddlx/sources/");
		fileName = fileName.replace("/dclsources/", "/dcl/sources/");

		String versionURIString = "/sap/bc/" + fileName + versionsURL;

		// Adjust URI path for class implementations
		if (type.equals("CLAS/OC") && !versionURIString.contains("/adt/oo")) {
			versionURIString = versionURIString.replace("/adt/classlib", "/adt/oo");
		}

		// Normalize the URI to handle ./ and ../ path segments
		return URI.create(versionURIString).normalize().getPath();
	}
}
