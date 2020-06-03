package uk.co.cocosquid.tokiponakeyboard;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class MySettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
        init();
    }

    private void init() {

        // Activating
        Preference openOnScreenKeyboardSetting = findPreference("open_on_screen_keyboard_setting");
        assert openOnScreenKeyboardSetting != null;
        openOnScreenKeyboardSetting.setOnPreferenceClickListener(this);

        Preference selectInputMethod = findPreference("select_input_method");
        assert selectInputMethod != null;
        selectInputMethod.setOnPreferenceClickListener(this);

        // Appearances
        //ListPreference themes = findPreference("themes");
        //assert themes != null;
        //themes.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {

            // Activating
            case "open_on_screen_keyboard_setting":
                startActivityForResult(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS), 0);
                break;
            case "select_input_method":
                InputMethodManager imeManager = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                assert imeManager != null;
                imeManager.showInputMethodPicker();
                break;
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case "themes":
                break;
            case "":
                break;
        }
        return true;
    }
}
