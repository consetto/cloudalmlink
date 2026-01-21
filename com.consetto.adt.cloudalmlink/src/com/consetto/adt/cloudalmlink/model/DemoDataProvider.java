package com.consetto.adt.cloudalmlink.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.consetto.adt.cloudalmlink.preferences.PreferenceConstants;

/**
 * Provides demo data for testing the Cloud ALM Link plugin.
 * When demo mode is enabled, displays sample transports and features with funny names.
 */
public class DemoDataProvider {

	/** URL to open when in demo mode */
	public static final String DEMO_CLOUD_ALM_URL = "https://cloudalmlink.consetto.com/";

	/**
	 * Checks if demo mode is enabled in preferences.
	 *
	 * @return true if demo mode is enabled
	 */
	public static boolean isDemoModeEnabled() {
		ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				"com.consetto.adt.cloudalmlink.preferences.CloudAlmPeferencePage");
		return scopedPreferenceStore.getBoolean(PreferenceConstants.P_DEMO);
	}

	/**
	 * Creates and returns a list of demo version elements with funny names.
	 *
	 * @return List of demo VersionElement objects
	 */
	public static List<VersionElement> getDemoVersions() {
		List<VersionElement> demoVersions = new ArrayList<>();

		// Active version
		VersionElement active = new VersionElement();
		active.setID("Active");
		active.setTransport("DEVK900042");
		active.setTitle("Current working version");
		FeatureElement activeFeature = new FeatureElement();
		activeFeature.setDisplayId("6-1234");
		activeFeature.setTitle("Fix the coffee machine API");
		activeFeature.setStatusCode("IN_PROGRESS");
		activeFeature.setResponsibleId("ABAP_JOE");
		active.setFeature(activeFeature);
		demoVersions.add(active);

		// Version 00001
		VersionElement v1 = new VersionElement();
		v1.setID("00001");
		v1.setTransport("DEVK900041");
		v1.setTitle("Added infinite scroll to infinite meetings");
		FeatureElement f1 = new FeatureElement();
		f1.setDisplayId("6-1337");
		f1.setTitle("Implement Rubber Duck Debugging Module");
		f1.setStatusCode("RELEASED");
		f1.setResponsibleId("DEBUG_DUCK");
		v1.setFeature(f1);
		demoVersions.add(v1);

		// Version 00002
		VersionElement v2 = new VersionElement();
		v2.setID("00002");
		v2.setTransport("DEVK900040");
		v2.setTitle("Refactored spaghetti code to linguine");
		FeatureElement f2 = new FeatureElement();
		f2.setDisplayId("6-404");
		f2.setTitle("Feature Not Found - But We Fixed It");
		f2.setStatusCode("COMPLETED");
		f2.setResponsibleId("BUG_WHISPERER");
		v2.setFeature(f2);
		demoVersions.add(v2);

		// Version 00003
		VersionElement v3 = new VersionElement();
		v3.setID("00003");
		v3.setTransport("DEVK900039");
		v3.setTitle("Fixed bug that only appears on Fridays");
		FeatureElement f3 = new FeatureElement();
		f3.setDisplayId("6-42");
		f3.setTitle("The Answer to Life, Universe, and This Bug");
		f3.setStatusCode("IN_REVIEW");
		f3.setResponsibleId("DEEP_THOUGHT");
		v3.setFeature(f3);
		demoVersions.add(v3);

		// Version 00004 (no feature)
		VersionElement v4 = new VersionElement();
		v4.setID("00004");
		v4.setTransport("DEVK900038");
		v4.setTitle("Works on my machine certified");
		// No feature for this one
		demoVersions.add(v4);

		return demoVersions;
	}
}
