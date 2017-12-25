package ch.epfl.sweng.project.fragments;


import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import ch.epfl.sweng.project.R;
import ch.epfl.sweng.project.models.Settings;


/**
 * Preference {@link Fragment}.
 * Content of the SettingActivity file.
 * @author Dominique Roduit
 */
public final class SettingsFragment extends PreferenceFragment {

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        // update preferences values
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

        // set entries for languages
        final ListPreference languagePref = (ListPreference)findPreference("language");
        setLanguagePrefData(languagePref);
        languagePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                languagePref.setValue(newValue.toString());
                languagePref.setSummary(Settings.Language.values()[Integer.parseInt(newValue.toString())].getDisplayName());
                return false;
            }
        });

    }

    /**
     * Attachs the data to bind entries to the Language item
     * @param lp Item "Language" in the preference xml file
     */
    protected static void setLanguagePrefData(ListPreference lp) {
        String[] entries = Settings.getLanguageEntries();

        Settings.Language[] langValues = Settings.Language.values();
        String[] entryValues = new String[langValues.length];
        int i = 0;
        for(Settings.Language l : langValues) {
            entryValues[i++] = String.valueOf(l.ordinal());
        }

        lp.setEntries(entries);
        lp.setEntryValues(entryValues);
        lp.setSummary(Settings.Language.values()[Integer.parseInt(lp.getValue())].getDisplayName());
    }

}
