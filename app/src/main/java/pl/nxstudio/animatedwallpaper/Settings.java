package pl.nxstudio.animatedwallpaper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;

import java.io.IOException;

/**
 * Created by adrianp on 27.05.15.
 */
public class Settings extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyFragment()).commit();
    }

    public static class MyFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs);

            setupImageSelector();
        }

        Preference.OnPreferenceChangeListener preferenceChangeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d("OnPreferenceChange", preference.getKey() + " has been modified to: " + newValue);
                return true;
            }
        };

        private void setupImageSelector() {
            // fill image preference with resource listing
            ListPreference imagePreference = (ListPreference) getPreferenceScreen().findPreference("image");
            CharSequence entries[] = new CharSequence[0];
            try {
                String assets[] = getResources().getAssets().list("");
                int i = 0;
                int applicableAssets = 0;
                for (String asset : assets) {
                    if (asset.endsWith("gif")) {
                        applicableAssets += 1;
                    }
                }
                entries = new CharSequence[applicableAssets];
                for (String asset : assets) {
                    if (asset.endsWith("gif")) {
                        entries[i++] = asset;
                    }
                }
            } catch (IOException ignored) {
                // hush!
            }
            imagePreference.setEntries(entries);
            imagePreference.setEntryValues(entries);
            imagePreference.setOnPreferenceChangeListener(preferenceChangeListener);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Log.d("Settings", "Closing settings");
        stopService(new Intent(this, AnimatedWallpaperService.class));
        startService(new Intent(this, AnimatedWallpaperService.class));
        super.onDestroy();
    }
}
