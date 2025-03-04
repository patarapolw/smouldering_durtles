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

package com.smouldering_durtles.wk.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.Converters;
import com.smouldering_durtles.wk.db.model.SearchPreset;
import com.smouldering_durtles.wk.livedata.LiveSearchPresets;
import com.smouldering_durtles.wk.model.SearchPresetExport;
import com.smouldering_durtles.wk.model.StarRatingsExport;
import com.smouldering_durtles.wk.proxy.ViewProxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.runAsync;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * An activity for importing and exporting some data that is not stored on the WK servers, and would
 * be lost during a database reset.
 */
public final class DataImportExportActivity extends AbstractActivity {
    private final ViewProxy exportSearchPresets = new ViewProxy();
    private final ViewProxy importSearchPresets = new ViewProxy();
    private final ViewProxy exportStarRatings = new ViewProxy();
    private final ViewProxy importStarRatings = new ViewProxy();
    private ActivityResultLauncher<Intent> importSearchPresetsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    importSearchPresetsResult(result.getData().getData());
                }
            });

    private ActivityResultLauncher<Intent> importStarRatingsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    importStarRatingsResult(result.getData().getData());
                }
            });

    /**
     * The constructor.
     */
    public DataImportExportActivity() {
        super(R.layout.activity_data_import_export, R.menu.generic_options_menu);
    }

    @Override
    protected void onCreateLocal(final @Nullable Bundle savedInstanceState) {
        exportSearchPresets.setDelegate(this, R.id.exportSearchPresets);
        importSearchPresets.setDelegate(this, R.id.importSearchPresets);
        exportStarRatings.setDelegate(this, R.id.exportStarRatings);
        importStarRatings.setDelegate(this, R.id.importStarRatings);

        exportSearchPresets.setOnClickListener(v -> safe(this::exportSearchPresets));
        importSearchPresets.setOnClickListener(v -> safe(this::importSearchPresets));
        exportStarRatings.setOnClickListener(v -> safe(this::exportStarRatings));
        importStarRatings.setOnClickListener(v -> safe(this::importStarRatings));

        importSearchPresetsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        importSearchPresetsResult(result.getData().getData());
                    }
                });

        importStarRatingsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        importStarRatingsResult(result.getData().getData());
                    }
                });
    }

    @Override
    protected void onResumeLocal() {
        //
    }

    @Override
    protected void onPauseLocal() {
        //
    }

    @Override
    protected void enableInteractionLocal() {
        //
    }

    @Override
    protected void disableInteractionLocal() {
        //
    }


    private void exportSearchPresets() throws IOException {
        final File baseDir = getFilesDir();
        final File sharedDir = new File(baseDir, "shared");
        if (!sharedDir.exists()) {
            if (!sharedDir.mkdir()) {
                return;
            }
        }

        final SearchPresetExport data = new SearchPresetExport();
        for (final String presetName: LiveSearchPresets.getInstance().getNames()) {
            final @Nullable SearchPreset preset = LiveSearchPresets.getInstance().getByName(presetName);
            data.getNamedPresets().add(preset);
        }
        data.setSelfStudyPreset(LiveSearchPresets.getInstance().getByName("\u0000SELF_STUDY_DEFAULT"));

        final File exportFile = new File(sharedDir, "search_presets.json");
        try (final FileOutputStream fos = new FileOutputStream(exportFile)) {
            Converters.getObjectMapper().writeValue(fos, data);
        }

        final Intent intent = new Intent(Intent.ACTION_SEND);
        final Uri uri = FileProvider.getUriForFile(this, "com.smouldering_durtles.wk.fileprovider", exportFile);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setType("application/json");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    @SuppressLint("NewApi")
    private void importSearchPresetsResult(final Uri uri) {
        runAsync(this, () -> {
            final AppDatabase db = WkApplication.getDatabase();
            try (final @Nullable InputStream is = WkApplication.getInstance().getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    final SearchPresetExport data = Converters.getObjectMapper().readValue(is, SearchPresetExport.class);
                    data.getNamedPresets().forEach(preset -> db.searchPresetDao().setPreset(preset.name, preset.type, preset.data));
                    if (data.getSelfStudyPreset() != null) {
                        db.searchPresetDao().setPreset(data.getSelfStudyPreset().name, data.getSelfStudyPreset().type, data.getSelfStudyPreset().data);
                    }
                    return true;
                }
            }
            return false;
        }, result -> {
            if (result != null && result) {
                Toast.makeText(this, "Import finished", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void importSearchPresets() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        importSearchPresetsLauncher.launch(Intent.createChooser(intent, "Select a search presets JSON file to import"));
    }

    private void exportStarRatings() {
        final File baseDir = getFilesDir();
        final File sharedDir = new File(baseDir, "shared");
        if (!sharedDir.exists()) {
            if (!sharedDir.mkdir()) {
                return;
            }
        }

        runAsync(this, () -> {
            final AppDatabase db = WkApplication.getDatabase();
            final StarRatingsExport data = new StarRatingsExport();
            data.setStars1(db.subjectCollectionsDao().getStarredSubjectIds(1));
            data.setStars2(db.subjectCollectionsDao().getStarredSubjectIds(2));
            data.setStars3(db.subjectCollectionsDao().getStarredSubjectIds(3));
            data.setStars4(db.subjectCollectionsDao().getStarredSubjectIds(4));
            data.setStars5(db.subjectCollectionsDao().getStarredSubjectIds(5));

            final File exportFile = new File(sharedDir, "star_ratings.json");
            try (final FileOutputStream fos = new FileOutputStream(exportFile)) {
                Converters.getObjectMapper().writeValue(fos, data);
            }

            return exportFile;
        }, result -> {
            if (result == null) {
                return;
            }
            final Intent intent = new Intent(Intent.ACTION_SEND);
            final Uri uri = FileProvider.getUriForFile(this, "com.smouldering_durtles.wk.fileprovider", result);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setType("application/json");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        });
    }

    @SuppressLint("NewApi")
    private void importStarRatingsResult(final Uri uri) {
        runAsync(this, () -> {
            final AppDatabase db = WkApplication.getDatabase();
            try (final @Nullable InputStream is = WkApplication.getInstance().getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    final StarRatingsExport data = Converters.getObjectMapper().readValue(is, StarRatingsExport.class);
                    data.getStars1().forEach(id -> db.subjectDao().updateStars(id, 1));
                    data.getStars2().forEach(id -> db.subjectDao().updateStars(id, 2));
                    data.getStars3().forEach(id -> db.subjectDao().updateStars(id, 3));
                    data.getStars4().forEach(id -> db.subjectDao().updateStars(id, 4));
                    data.getStars5().forEach(id -> db.subjectDao().updateStars(id, 5));
                    return true;
                }
            }
            return false;
        }, result -> {
            if (result != null && result) {
                Toast.makeText(this, "Import finished", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void importStarRatings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        importStarRatingsLauncher.launch(Intent.createChooser(intent, "Select a star ratings JSON file to import"));
    }
}

