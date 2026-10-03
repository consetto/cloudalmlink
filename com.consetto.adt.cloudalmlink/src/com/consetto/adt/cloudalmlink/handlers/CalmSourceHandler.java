package com.consetto.adt.cloudalmlink.handlers;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import org.eclipse.core.resources.IProject;

import com.consetto.adt.cloudalmlink.core.AdtResponseParser;
import com.consetto.adt.cloudalmlink.core.VersionUris;
import com.consetto.adt.cloudalmlink.model.DemoDataProvider;
import com.consetto.adt.cloudalmlink.model.VersionData;
import com.consetto.adt.cloudalmlink.util.CloudAlmLinkLogger;
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
 * Retrieves version data from ADT in a job and displays results in the TransportView.
 */
public class CalmSourceHandler extends AbstractHandler {

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
			showTransportView(window, null, null);
			return null;
		}

		// Ensure user is logged on to the ABAP system; may show the logon dialog
		AdtLogonServiceUIFactory.createLogonServiceUI().ensureLoggedOn(
				context.getAbapProject().getDestinationData(),
				PlatformUI.getWorkbench().getProgressService());

		Job job = Job.create("Reading transports and Cloud ALM features", monitor -> {
			readVersions(context, window, monitor);
		});
		job.setUser(true);
		job.schedule();
		return null;
	}

	/**
	 * Reads the object's versions, transports and features from ADT and Cloud ALM and shows them.
	 * Runs in a job; calls the backend.
	 */
	private void readVersions(AdtObjectContext context, IWorkbenchWindow window, IProgressMonitor monitor) {
		context.loadMissingAtomLinks();

		// Extract URLs from atom links
		VersionUris.Endpoints urls = VersionUris.find(context.getAtomLinks(), context.getObjectType(),
				context.getObjectUri(), context.getRawLocationUri());
		if (urls.versionsUrl() == null) {
			showError(window, "Could not find versions URL for this object", context.getProject());
			return;
		}

		String destination = context.getDestination();
		IRestResourceFactory restResourceFactory = AdtRestResourceFactory.createRestResourceFactory();

		// Transport of the active version, from the object's transports endpoint
		String activeTransportId = fetchActiveTransport(urls.transportsUrl(), destination, restResourceFactory);
		if (monitor.isCanceled()) {
			return;
		}

		VersionData versions = fetchVersions(urls.versionsUrl(), destination, restResourceFactory);
		if (versions == null) {
			showError(window, "Unable to read the versions for this object. URL " + urls.versionsUrl()
					+ " did not return the right version.", context.getProject());
			return;
		}
		versions.arrange(activeTransportId);
		if (monitor.isCanceled()) {
			return;
		}

		// A version may name a task; Cloud ALM knows its request
		versions.assignFeatures(id -> resolveParentTransport(id, destination, restResourceFactory));
		if (monitor.isCanceled()) {
			return;
		}

		window.getWorkbench().getDisplay().asyncExec(() -> showTransportView(window, versions, context.getProject()));
	}

	private void showError(IWorkbenchWindow window, String message, IProject project) {
		window.getWorkbench().getDisplay().asyncExec(() -> {
			MessageDialog.openError(window.getShell(), "ADT Cloud ALM Link Error", message);
			showTransportView(window, null, project);
		});
	}

	/**
	 * Resolves the ADT object context based on where the command was triggered.
	 * Uses the active part to distinguish between editor and non-editor contexts
	 * (e.g. Project Explorer), so right-clicking in the Project Explorer resolves
	 * the selected object rather than the object open in the editor.
	 *
	 * @param event The execution event
	 * @param window The workbench window
	 * @return The resolved context, or null if no valid context found
	 */
	private AdtObjectContext resolveObjectContext(ExecutionEvent event, IWorkbenchWindow window) {
		IWorkbenchPart activePart = HandlerUtil.getActivePart(event);

		if (activePart instanceof IEditorPart) {
			// Triggered from editor context menu or keyboard shortcut while editor focused
			IAdtFormEditor editor = getActiveAdtEditor();
			if (editor != null) {
				AdtObjectContext context = AdtObjectContext.fromEditor(editor);
				if (context != null) {
					return context;
				}
			}
		} else {
			// Triggered from Project Explorer or other non-editor view
			ISelection selection = HandlerUtil.getCurrentSelection(event);
			if (selection != null) {
				AdtObjectContext context = AdtObjectContext.fromSelection(selection);
				if (context != null) {
					return context;
				}
			}
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
			// Use pattern matching for instanceof (Java 21)
			if (activeEditor instanceof IAdtFormEditor editor) {
				return editor;
			}
		} catch (Exception e) {
			// No active editor
		}
		return null;
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
				return AdtResponseParser.extractTransportId(transportResponse);
			}
		} catch (Exception e) {
			// Transport fetch failed - continue without active version
		}

		return null;
	}

	/**
	 * Fetches version data from the versions endpoint.
	 *
	 * @param versionsURL The versions endpoint URL
	 * @param destination The ABAP destination ID
	 * @param restResourceFactory The REST resource factory
	 * @return The parsed version data, or null on error
	 */
	private VersionData fetchVersions(String versionsURL, String destination,
			IRestResourceFactory restResourceFactory) {
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
			CloudAlmLinkLogger.logWarning("Failed to read versions from " + versionsURL + ": " + e.getMessage());
			return null;
		}
	}

	/**
	 * Resolves a transport ID to its parent transport request if it is a task.
	 * Calls the ADT CTS endpoint and parses the response to find a parent.
	 *
	 * @param transportId The transport ID to check
	 * @param destination The ABAP destination ID
	 * @param restResourceFactory The REST resource factory
	 * @return The parent transport ID if this is a task, the original ID otherwise
	 */
	private String resolveParentTransport(String transportId, String destination,
			IRestResourceFactory restResourceFactory) {
		try {
			URI ctsUri = URI.create("/sap/bc/adt/cts/transportrequests/" + transportId);
			IRestResource ctsResource = restResourceFactory.createResourceWithStatelessSession(
					ctsUri, destination);

			IHeaders ctsHeader = HeadersFactory.newHeaders();
			IField ctsAcceptField = HeadersFactory.newField("Accept",
					"application/vnd.sap.adt.transportorganizer.v1+xml");
			ctsHeader.setField(ctsAcceptField);

			IMessageBody ctsBody = ctsResource.get(null, ctsHeader, IMessageBody.class);
			if (ctsBody != null) {
				byte[] bytes = ctsBody.getContent().readAllBytes();
				String ctsResponse = new String(bytes, StandardCharsets.UTF_8);
				String parentId = AdtResponseParser.extractParentTransport(ctsResponse, transportId);
				if (parentId != null) {
					return parentId;
				}
			}
		} catch (Exception e) {
			// CTS lookup failed - return original transport ID
		}

		return transportId;
	}

	/**
	 * Shows demo data in the TransportView.
	 */
	private void showDemoData(ExecutionEvent event) {
		try {
			IWorkbenchPage workbenchPage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
			workbenchPage.showView(TransportView.ID);
			TransportView transportView = (TransportView) workbenchPage
					.findView(TransportView.ID);
			transportView.setDemoData(DemoDataProvider.getDemoVersions());
		} catch (PartInitException e) {
			// View could not be opened - fail silently
		}
	}

	/**
	 * Displays the version data in the TransportView.
	 */
	private void showTransportView(IWorkbenchWindow window, VersionData versions, IProject project) {
		if (window.getShell() == null || window.getShell().isDisposed() || window.getActivePage() == null) {
			return;
		}
		try {
			IWorkbenchPage workbenchPage = window.getActivePage();
			workbenchPage.showView(TransportView.ID);
			TransportView transportView = (TransportView) workbenchPage
					.findView(TransportView.ID);
			transportView.setProject(project);
			transportView.setVersionData(versions);
		} catch (PartInitException e) {
			// View could not be opened - fail silently
		}
	}
}
