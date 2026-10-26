package com.consetto.adt.cloudalmlink.handlers;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.consetto.adt.cloudalmlink.core.AdtResponseParser;
import com.consetto.adt.cloudalmlink.core.AtomLink;
import com.consetto.adt.cloudalmlink.util.CloudAlmLinkLogger;
import com.sap.adt.communication.message.HeadersFactory;
import com.sap.adt.communication.message.IHeaders;
import com.sap.adt.communication.message.IHeaders.IField;
import com.sap.adt.communication.message.IMessageBody;
import com.sap.adt.communication.resources.AdtRestResourceFactory;
import com.sap.adt.communication.resources.IRestResource;
import com.sap.adt.communication.resources.IRestResourceFactory;
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
	private boolean linksFromBackend; // For selection context: atom links still to be fetched

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
			context.objectUri = AdtResponseParser.extractObjectUri(context.atomLinks);
			if (context.objectUri == null) {
				// Fallback: try to get from editor file location
				context.objectUri = AdtResponseParser.extractPathFromRawUri(context.rawLocationUri);
			}

			return context;
		} catch (Exception e) {
			CloudAlmLinkLogger.logWarning("Failed to create context from editor: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Creates context from a Project Explorer selection.
	 * Does not call the backend; {@link #loadMissingAtomLinks()} fetches the atom links.
	 *
	 * @param selection The workbench selection
	 * @return The object context, or null if extraction failed
	 */
	public static AdtObjectContext fromSelection(ISelection selection) {
		// Use pattern matching for instanceof (Java 21)
		if (!(selection instanceof IStructuredSelection structuredSelection)) {
			return null;
		}

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

			// Get the selected ADT object reference using pattern matching
			Object selectedElement = structuredSelection.getFirstElement();
			IAdtObjectReference adtObjectRef = null;
			if (selectedElement instanceof IAdtObjectReference ref) {
				adtObjectRef = ref;
			} else if (selectedElement instanceof IAdaptable adaptable) {
				adtObjectRef = adaptable.getAdapter(IAdtObjectReference.class);
			}

			if (adtObjectRef == null) {
				return null;
			}

			context.objectUri = adtObjectRef.getUri().toString();
			context.objectType = adtObjectRef.getType();
			context.destination = context.abapProject.getDestinationId();
			context.linksFromBackend = true;

			return context;
		} catch (Exception e) {
			CloudAlmLinkLogger.logWarning("Failed to create context from selection: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Fetches the atom links of a Project Explorer selection from the backend; an editor context
	 * already has them. Calls the backend; do not call it on the UI thread.
	 */
	public void loadMissingAtomLinks() {
		if (linksFromBackend) {
			atomLinks = fetchAtomLinks(objectUri, destination);
			linksFromBackend = false;
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
				// Use try-with-resources to ensure InputStream is properly closed
				try (InputStream content = body.getContent()) {
					byte[] bytes = content.readAllBytes();
					String response = new String(bytes, StandardCharsets.UTF_8);

					// Parse atom links from XML response
					links = AdtResponseParser.parseAtomLinks(response);
				}
			}
		} catch (Exception e) {
			CloudAlmLinkLogger.logWarning("Failed to fetch atom links for " + objectUri + ": " + e.getMessage());
		}

		return links;
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
