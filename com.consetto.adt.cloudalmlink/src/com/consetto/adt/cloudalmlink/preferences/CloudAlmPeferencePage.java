package com.consetto.adt.cloudalmlink.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */
public class CloudAlmPeferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public CloudAlmPeferencePage() {
		super(GRID);

		ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				"com.consetto.adt.cloudalmlink.preferences.CloudAlmPeferencePage");
		setPreferenceStore(scopedPreferenceStore);
		setDescription("Please enter tenant and region from Cloud ALM: https://tenant.region.alm.cloud.sap");

	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common GUI
	 * blocks needed to manipulate various types of preferences. Each field editor
	 * knows how to save and restore itself.
	 */
	public void createFieldEditors() {

		addField(new StringFieldEditor(PreferenceConstants.P_TEN, "Tenant:", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_REG, "Region:", getFieldEditorParent()));

		addField(new StringFieldEditor(PreferenceConstants.P_CID, "Client ID:", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_KEY, "Client Secret:", getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {
	}

}