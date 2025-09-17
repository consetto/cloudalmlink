package com.consetto.adt.cloudalmlink.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.FrameworkUtil;


/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		ScopedPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, String.valueOf(FrameworkUtil.getBundle(getClass()).getBundleId()));

		store.setDefault(PreferenceConstants.P_URL, "https://calm-demo.eu10.alm.cloud.sap");

	}

}
