/*
 * Copyright 2019-2020 Ernst Jan Plugge <rmc@dds.nl>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smouldering_durtles.wk.fragments;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;

import com.smouldering_durtles.wk.Actment;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.activities.AboutActivity;
import com.smouldering_durtles.wk.activities.AbstractActivity;
import com.smouldering_durtles.wk.activities.DataImportExportActivity;
import com.smouldering_durtles.wk.activities.FontImportActivity;
import com.smouldering_durtles.wk.activities.FontSelectionActivity;
import com.smouldering_durtles.wk.activities.KeyboardHelpActivity;
import com.smouldering_durtles.wk.activities.SupportActivity;
import com.smouldering_durtles.wk.activities.ThemeCustomizationActivity;
import com.smouldering_durtles.wk.api.ApiState;
import com.smouldering_durtles.wk.components.NumberRangePreference;
import com.smouldering_durtles.wk.components.NumberRangePreferenceDialogFragment;
import com.smouldering_durtles.wk.components.TaggedUrlPreference;
import com.smouldering_durtles.wk.components.TaggedUrlPreferenceDialogFragment;
import com.smouldering_durtles.wk.jobs.ResetDatabaseJob;
import com.smouldering_durtles.wk.livedata.LiveApiState;
import com.smouldering_durtles.wk.fragments.services.JobRunnerService;
import com.smouldering_durtles.wk.util.AudioUtil;
import com.smouldering_durtles.wk.util.DbLogger;
import com.smouldering_durtles.wk.util.ThemeUtil;

import java.util.List;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.Constants.API_KEY_PERMISSION_NOTICE;
import static com.smouldering_durtles.wk.Constants.ENABLE_ADVANCED_WARNING;
import static com.smouldering_durtles.wk.Constants.EXPERIMENTAL_PREFERENCE_STATUS_NOTICE;
import static com.smouldering_durtles.wk.Constants.RESET_DATABASE_WARNING;
import static com.smouldering_durtles.wk.Constants.RESET_TUTORIALS_WARNING;
import static com.smouldering_durtles.wk.Constants.SUBJECT_SELECTION_NOTICE;
import static com.smouldering_durtles.wk.Constants.UPLOAD_DEBUG_LOG_WARNING;
import static com.smouldering_durtles.wk.util.ObjectSupport.isTrue;
import static com.smouldering_durtles.wk.util.ObjectSupport.runAsync;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;
import static com.smouldering_durtles.wk.util.TextUtil.renderHtml;

/**
 * Fragment for preferences.
 */
public class PreferencesFragment extends PreferenceFragmentCompat {
    // ...

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, @Nullable final String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof TaggedUrlPreference) {
            TaggedUrlPreferenceDialogFragment fragment = TaggedUrlPreferenceDialogFragment.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(getParentFragmentManager(), null);
            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

    private void onViewCreatedBase(final View view, final @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackgroundColor(ThemeUtil.getColor(R.attr.colorBackground));

        final @Nullable Preference enableAdvanced = findPreference("enable_advanced");
        if (enableAdvanced != null) {
            enableAdvanced.setOnPreferenceChangeListener((preference, newValue) -> safe(false, () -> {
                final boolean enabled = isTrue(newValue);
                if (enabled && !GlobalSettings.getAdvancedEnabled()) {
                    new AlertDialog.Builder(view.getContext())
                            .setTitle("Enable advanced settings?")
                            .setMessage(renderHtml(ENABLE_ADVANCED_WARNING))
                            .setIcon(R.drawable.ic_baseline_warning_24px)
                            .setNegativeButton("No", (dialog, which) -> safe(() -> {
                                GlobalSettings.setAdvancedEnabled(false);
                                setVisibility("advanced_lesson_settings", false);
                                setVisibility("advanced_review_settings", false);
                                setVisibility("advanced_self_study_settings", false);
                                setVisibility("advanced_other_settings", false);
                                ((TwoStatePreference) preference).setChecked(false);
                            }))
                            .setPositiveButton("Yes", (dialog, which) -> safe(() -> {
                                GlobalSettings.setAdvancedEnabled(true);
                                setVisibility("advanced_lesson_settings", true);
                                setVisibility("advanced_review_settings", true);
                                setVisibility("advanced_self_study_settings", true);
                                setVisibility("advanced_other_settings", true);
                                ((TwoStatePreference) preference).setChecked(true);
                            })).create().show();
                    return false;
                }
                else {
                    setVisibility("advanced_lesson_settings", enabled);
                    setVisibility("advanced_review_settings", enabled);
                    setVisibility("advanced_self_study_settings", enabled);
                    setVisibility("advanced_other_settings", enabled);
                }
                return true;
            }));
        }

        setOnPreferenceClick("reset_database", preference -> safe(false, () -> {
            new AlertDialog.Builder(preference.getContext())
                    .setTitle("Reset database?")
                    .setMessage(renderHtml(RESET_DATABASE_WARNING))
                    .setIcon(R.drawable.ic_baseline_warning_24px)
                    .setNegativeButton("No", (dialog, which) -> {})
                    .setPositiveButton("Yes", (dialog, which) -> safe(() -> {
                        JobRunnerService.schedule(ResetDatabaseJob.class, "");
                        goToMainActivity();
                    })).create().show();
            return true;
        }));

        setOnPreferenceClick("reset_tutorials", preference -> safe(false, () -> {
            new AlertDialog.Builder(preference.getContext())
                    .setTitle("Reset confirmations and tutorials?")
                    .setMessage(renderHtml(RESET_TUTORIALS_WARNING))
                    .setIcon(R.drawable.ic_baseline_warning_24px)
                    .setNegativeButton("No", (dialog, which) -> {})
                    .setPositiveButton("Yes", (dialog, which) -> safe(() -> {
                        GlobalSettings.resetConfirmationsAndTutorials();
                        Toast.makeText(preference.getContext(), "Confirmations and tutorials reset", Toast.LENGTH_LONG).show();
                    })).create().show();
            return true;
        }));

        setOnPreferenceClick("upload_debug_log", preference -> safe(false, () -> {
            new AlertDialog.Builder(preference.getContext())
                    .setTitle("Upload debug log?")
                    .setMessage(renderHtml(UPLOAD_DEBUG_LOG_WARNING))
                    .setIcon(R.drawable.ic_baseline_warning_24px)
                    .setNegativeButton("No", (dialog, which) -> {})
                    .setPositiveButton("Yes", (dialog, which) -> safe(() -> runAsync(
                            this,
                            DbLogger::uploadLog,
                            result -> {
                                if (result != null && result) {
                                    Toast.makeText(requireContext(), "Upload successful, thanks!", Toast.LENGTH_LONG).show();
                                }
                                else {
                                    Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_LONG).show();
                                }
                            }))).create().show();
            return true;
        }));

        final @Nullable ListPreference audioLocation = findPreference("audio_location");
        if (audioLocation != null) {
            final List<String> locationValues = AudioUtil.getLocationValues();
            final List<String> locations = AudioUtil.getLocations(locationValues);
            audioLocation.setEntries(locations.toArray(new String[] {}));
            audioLocation.setEntryValues(locationValues.toArray(new String[] {}));
            audioLocation.setVisible(true);
        }

        setVisibility("api_key_help", LiveApiState.getInstance().get() != ApiState.OK);
        setVisibility("advanced_lesson_settings", GlobalSettings.getAdvancedEnabled());
        setVisibility("advanced_review_settings", GlobalSettings.getAdvancedEnabled());
        setVisibility("advanced_self_study_settings", GlobalSettings.getAdvancedEnabled());
        setVisibility("advanced_other_settings", GlobalSettings.getAdvancedEnabled());
        setVisibility("ime_hint_reading", Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
        setVisibility("ime_hint_meaning", Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
        setVisibility("web_password", Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);

        setSummaryHtml("api_key_help", API_KEY_PERMISSION_NOTICE);
        setSummaryHtml("experimental_status", EXPERIMENTAL_PREFERENCE_STATUS_NOTICE);
        setSummaryHtml("lesson_subject_selection", SUBJECT_SELECTION_NOTICE);
        setSummaryHtml("review_subject_selection", SUBJECT_SELECTION_NOTICE);
        setSummaryHtml("self_study_subject_selection", SUBJECT_SELECTION_NOTICE);

        setNumberInputType("overdue_threshold");
        setNumberInputType("max_lesson_session_size");
        setNumberInputType("max_review_session_size");
        setNumberInputType("max_self_study_session_size");
        setDecimalNumberInputType("next_button_delay");

        setOnClickGoToActivity("about_this_app", AboutActivity.class);
        setOnClickGoToActivity("support_and_feedback", SupportActivity.class);
        setOnClickGoToActivity("theme_customization", ThemeCustomizationActivity.class);
        setOnClickGoToActivity("font_selection", FontSelectionActivity.class);
        setOnClickGoToActivity("font_import", FontImportActivity.class);
        setOnClickGoToActivity("keyboard_help", KeyboardHelpActivity.class);
        setOnClickGoToActivity("data_import_export", DataImportExportActivity.class);
    }

    @Override
    public void onViewCreated(final View view, final @Nullable Bundle savedInstanceState) {
        safe(() -> onViewCreatedBase(view, savedInstanceState));
    }

    @Override
    public void onResume() {
        safe(() -> {
            super.onResume();
            final @Nullable Bundle args = getArguments();
            @Nullable CharSequence title = null;
            if (args != null) {
                final @Nullable String key = args.getString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT);
                if (key != null) {
                    final @Nullable Preference preference = findPreference(key);
                    if (preference != null) {
                        title = preference.getTitle();
                    }
                }
            }
            if (title == null) {
                title = "Settings";
            }
            final @Nullable Activity activity = getActivity();
            if (activity instanceof Actment) {
                final @Nullable Toolbar toolbar = ((Actment) activity).getToolbar();
                if (toolbar != null) {
                    toolbar.setTitle(title);
                }
            }
        });
    }

    @Override
    public void onDisplayPreferenceDialog(final Preference preference) {
        safe(() -> {
            if (preference instanceof TaggedUrlPreference) {
                displayCustomPreferenceDialog("TaggedUrlPreference", TaggedUrlPreferenceDialogFragment.newInstance(preference.getKey()));
                return;
            }
            if (preference instanceof NumberRangePreference) {
                displayCustomPreferenceDialog("NumberRangePreference", NumberRangePreferenceDialogFragment.newInstance(preference.getKey()));
                return;
            }

            super.onDisplayPreferenceDialog(preference);
        });
    }

    private void displayCustomPreferenceDialog(String tag, DialogFragment dialogFragment) {
        if (getParentFragmentManager().findFragmentByTag(tag) != null) {
            return;
        }
        dialogFragment.show(getParentFragmentManager(), tag);
    }

    private void goToActivity(final Class<? extends AbstractActivity> clas) {
        final @Nullable Activity a = getActivity();
        if (a instanceof Actment) {
            ((Actment) a).goToActivity(clas);
        }
    }

    private void goToMainActivity() {
        final @Nullable Activity a = getActivity();
        if (a instanceof Actment) {
            ((Actment) a).goToMainActivity();
        }
    }

    private void setOnPreferenceClick(final CharSequence key, final @Nullable Preference.OnPreferenceClickListener listener) {
        safe(() -> {
            final @Nullable Preference pref = findPreference(key);
            if (pref != null) {
                pref.setOnPreferenceClickListener(listener);
            }
        });
    }

    private void setOnClickGoToActivity(final CharSequence key, final Class<? extends AbstractActivity> clas) {
        safe(() -> setOnPreferenceClick(key, preference -> {
            safe(() -> goToActivity(clas));
            return true;
        }));
    }

    private void setNumberInputType(final CharSequence key) {
        safe(() -> {
            final @Nullable EditTextPreference pref = findPreference(key);
            if (pref != null) {
                pref.setOnBindEditTextListener(editText -> safe(() -> {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    editText.setSelection(editText.getText().length());
                }));
            }
        });
    }

    private void setSummaryHtml(final CharSequence key, final String html) {
        safe(() -> {
            final @Nullable Preference pref = findPreference(key);
            if (pref != null) {
                pref.setSummary(renderHtml(html));
            }
        });
    }

    private void setDecimalNumberInputType(@SuppressWarnings("SameParameterValue") final CharSequence key) {
        safe(() -> {
            final @Nullable EditTextPreference pref = findPreference(key);
            if (pref != null) {
                pref.setOnBindEditTextListener(editText -> safe(() -> {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    editText.setSelection(editText.getText().length());
                }));
            }
        });
    }

    private void setVisibility(final CharSequence key, final boolean visible) {
        safe(() -> {
            final @Nullable Preference pref = findPreference(key);
            if (pref != null) {
                pref.setVisible(visible);
            }
        });
    }
}
