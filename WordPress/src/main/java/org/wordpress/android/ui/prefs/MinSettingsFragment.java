package org.wordpress.android.ui.prefs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.ShareIntentReceiverActivity;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPEditTextPreference;
import org.wordpress.passcodelock.AppLockManager;
import org.wordpress.passcodelock.PasscodePreferencesActivity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("deprecation")
public class MinSettingsFragment extends PreferenceFragment {
    public static final String SETTINGS_PREFERENCES = "settings-pref";

    private AlertDialog mDialog;
    private SharedPreferences mSettings;
    private WPEditTextPreference mTaglineTextPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources resources = getResources();

        if (savedInstanceState == null) {
            AnalyticsTracker.track(Stat.OPENED_SETTINGS);
        }

        addPreferencesFromResource(R.xml.min_settings);
        OnPreferenceChangeListener preferenceChangeListener = new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null) { // cancelled dismiss keyboard
                    preference.setSummary(newValue.toString());
                }
                ActivityUtils.hideKeyboard(getActivity());
                return true;
            }
        };


        findPreference(resources.getString(R.string.pref_key_passlock)).setOnPreferenceChangeListener(passcodeCheckboxChangeListener);

        mSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());


        // Passcode Lock not supported
        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            final CheckBoxPreference passcodeEnabledCheckBoxPreference = (CheckBoxPreference) findPreference(
                    resources.getString(R.string.pref_key_passlock));
            // disable on-click changes on the property
            passcodeEnabledCheckBoxPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    passcodeEnabledCheckBoxPreference.setChecked(
                            AppLockManager.getInstance().getCurrentAppLock().isPasswordLocked());
                    return false;
                }
            });
        } else {
            PreferenceScreen rootScreen = (PreferenceScreen) findPreference(resources.getString(R.string.pref_key_settings_root));
            PreferenceGroup passcodeGroup = (PreferenceGroup) findPreference(resources.getString(R.string.pref_key_passlock_section));
            rootScreen.removePreference(passcodeGroup);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.settings);

        //update Passcode lock row if available
        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            CheckBoxPreference passcodeEnabledCheckBoxPreference = (CheckBoxPreference) findPreference(getResources().getString(R.string.pref_key_passlock));
            if (AppLockManager.getInstance().getCurrentAppLock().isPasswordLocked()) {
                passcodeEnabledCheckBoxPreference.setChecked(true);
            } else {
                passcodeEnabledCheckBoxPreference.setChecked(false);
            }
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
        }
        return super.onOptionsItemSelected(item);
    }


    private final OnPreferenceChangeListener passcodeCheckboxChangeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            startActivity(new Intent(getActivity(), PasscodePreferencesActivity.class));
            return true;
        }
    };
}
