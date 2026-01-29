package com.consetto.adt.cloudalmlink;

/**
 * Central constants for the Cloud ALM Link plugin.
 * Eliminates magic strings scattered throughout the codebase.
 */
public final class CloudAlmLinkConstants {

	private CloudAlmLinkConstants() {
		// Prevent instantiation
	}

	/** The plugin ID used for logging and extension points */
	public static final String PLUGIN_ID = "com.consetto.adt.cloudalmlink";

	/** Preference qualifier for ScopedPreferenceStore */
	public static final String PREFERENCE_QUALIFIER = "com.consetto.adt.cloudalmlink.preferences.CloudAlmPeferencePage";

	/** Transport View ID for view registration */
	public static final String TRANSPORT_VIEW_ID = "com.consetto.adt.cloudalmlink.views.TransportView";

	/** Cloud ALM domain suffix */
	public static final String CLOUD_ALM_DOMAIN = ".alm.cloud.sap";

	/** SAP authentication domain suffix */
	public static final String AUTH_DOMAIN = ".hana.ondemand.com";

	/** Cloud ALM API path for features */
	public static final String FEATURES_API_PATH = "/api/calm-features/v1";

	/** OAuth token endpoint path */
	public static final String OAUTH_TOKEN_PATH = "/oauth/token";

	/** Launchpad fragment for feature details */
	public static final String FEATURE_LAUNCHPAD_FRAGMENT = "/launchpad#feature-display?sap-ui-app-id-hint=com.sap.calm.imp.cdm.features.ui&/details/";

	/** Launchpad fragment for task details */
	public static final String TASK_LAUNCHPAD_FRAGMENT = "/launchpad#task-management?sap-app-origin-hint=&/taskDetail/";

	/** Launchpad fragment for document details */
	public static final String DOCUMENT_LAUNCHPAD_FRAGMENT = "/launchpad#DocumentationObject-manage?sap-ui-app-id-hint=com.sap.calm.imp.sd.docu.ui&/Documents('";

	/** Launchpad fragment for library details */
	public static final String LIBRARY_LAUNCHPAD_FRAGMENT = "/launchpad#library-management?sap-ui-app-id-hint=com.sap.calm.imp.lib.ui&/LibraryElement('";

	// ADT Relations
	public static final String TRANSPORT_REL = "http://www.sap.com/adt/relations/transport";
	public static final String VERSIONS_REL = "http://www.sap.com/adt/relations/versions";
}
