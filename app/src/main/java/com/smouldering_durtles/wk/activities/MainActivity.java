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

import android.os.Bundle;
import android.view.View;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.api.ApiState;
import com.smouldering_durtles.wk.jobs.RetryApiErrorJob;
import com.smouldering_durtles.wk.livedata.LiveApiState;
import com.smouldering_durtles.wk.livedata.LiveBurnedItems;
import com.smouldering_durtles.wk.livedata.LiveCriticalCondition;
import com.smouldering_durtles.wk.livedata.LiveJlptProgress;
import com.smouldering_durtles.wk.livedata.LiveJoyoProgress;
import com.smouldering_durtles.wk.livedata.LiveLevelDuration;
import com.smouldering_durtles.wk.livedata.LiveLevelProgress;
import com.smouldering_durtles.wk.livedata.LiveRecentUnlocks;
import com.smouldering_durtles.wk.livedata.LiveSrsBreakDown;
import com.smouldering_durtles.wk.livedata.LiveTimeLine;
import com.smouldering_durtles.wk.model.Session;
import com.smouldering_durtles.wk.model.TimeLine;
import com.smouldering_durtles.wk.proxy.ViewProxy;
import com.smouldering_durtles.wk.fragments.services.BackgroundAlarmReceiver;
import com.smouldering_durtles.wk.fragments.services.BackgroundAlarmReceiverPost19;
import com.smouldering_durtles.wk.fragments.services.BackgroundAlarmReceiverPost23;
import com.smouldering_durtles.wk.fragments.services.BackgroundSyncWorker;
import com.smouldering_durtles.wk.fragments.services.JobRunnerService;
import com.smouldering_durtles.wk.views.AvailableSessionsView;
import com.smouldering_durtles.wk.views.FirstTimeSetupView;
import com.smouldering_durtles.wk.views.JlptProgressView;
import com.smouldering_durtles.wk.views.JoyoProgressView;
import com.smouldering_durtles.wk.views.LessonReviewBreakdownView;
import com.smouldering_durtles.wk.views.LevelDurationView;
import com.smouldering_durtles.wk.views.LevelProgressView;
import com.smouldering_durtles.wk.views.LiveBurnedItemsSubjectTableView;
import com.smouldering_durtles.wk.views.LiveCriticalConditionSubjectTableView;
import com.smouldering_durtles.wk.views.LiveRecentUnlocksSubjectTableView;
import com.smouldering_durtles.wk.views.Post60ProgressView;
import com.smouldering_durtles.wk.views.SessionButtonsView;
import com.smouldering_durtles.wk.views.SrsBreakDownView;
import com.smouldering_durtles.wk.views.SyncProgressView;
import com.smouldering_durtles.wk.views.TimeLineBarChart;
import com.smouldering_durtles.wk.views.UpcomingReviewsView;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.runAsync;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * The dashboard activity.
 *
 * <p>
 *     This has by far the most complex layout. The activity contains a multitude
 *     of views that display database state. It is informed of changes via LiveData
 *     instances.
 * </p>
 */
public final class MainActivity extends AbstractActivity {
    private final ViewProxy apiErrorView = new ViewProxy();
    private final ViewProxy apiKeyRejectedView = new ViewProxy();
    private final ViewProxy keyboardHelpView = new ViewProxy();

    /**
     * The constructor.
     */
    public MainActivity() {
        super(R.layout.activity_main, R.menu.main_options_menu);
    }

    @Override
    protected void onCreateLocal(final @Nullable Bundle savedInstanceState) {
        apiErrorView.setDelegate(this, R.id.apiErrorView);
        apiKeyRejectedView.setDelegate(this, R.id.apiKeyRejectedView);
        keyboardHelpView.setDelegate(this, R.id.keyboardHelpView);

        LiveApiState.getInstance().observe(this, t -> safe(() -> {
            apiErrorView.setVisibility(t == ApiState.ERROR);
            apiKeyRejectedView.setVisibility(t == ApiState.API_KEY_REJECTED);
        }));

        final @Nullable AvailableSessionsView availableSessionsView = findViewById(R.id.availableSessionsView);
        if (availableSessionsView != null) {
            availableSessionsView.setLifecycleOwner(this);
        }

        final @Nullable LessonReviewBreakdownView lessonReviewBreakdownView = findViewById(R.id.lessonReviewBreakdownView);
        if (lessonReviewBreakdownView != null) {
            lessonReviewBreakdownView.setLifecycleOwner(this);
        }

        final @Nullable FirstTimeSetupView firstTimeSetupView = findViewById(R.id.firstTimeSetupView);
        if (firstTimeSetupView != null) {
            firstTimeSetupView.setLifecycleOwner(this);
        }

        final @Nullable LevelDurationView levelDurationView = findViewById(R.id.levelDurationView);
        if (levelDurationView != null) {
            levelDurationView.setLifecycleOwner(this);
        }

        final @Nullable LevelProgressView levelProgressView = findViewById(R.id.levelProgressView);
        if (levelProgressView != null) {
            levelProgressView.setLifecycleOwner(this);
        }

        final @Nullable Post60ProgressView post60ProgressView = findViewById(R.id.post60ProgressView);
        if (post60ProgressView != null) {
            post60ProgressView.setLifecycleOwner(this);
        }

        final @Nullable JoyoProgressView joyoProgressView = findViewById(R.id.joyoProgressView);
        if (joyoProgressView != null) {
            joyoProgressView.setLifecycleOwner(this);
        }

        final @Nullable JlptProgressView jlptProgressView = findViewById(R.id.jlptProgressView);
        if (jlptProgressView != null) {
            jlptProgressView.setLifecycleOwner(this);
        }

        final @Nullable LiveRecentUnlocksSubjectTableView recentUnlocksView = findViewById(R.id.recentUnlocksView);
        if (recentUnlocksView != null) {
            recentUnlocksView.setLifecycleOwner(this);
        }

        final @Nullable LiveCriticalConditionSubjectTableView criticalConditionView = findViewById(R.id.criticalConditionView);
        if (criticalConditionView != null) {
            criticalConditionView.setLifecycleOwner(this);
        }

        final @Nullable LiveBurnedItemsSubjectTableView burnedItemsView = findViewById(R.id.burnedItemsView);
        if (burnedItemsView != null) {
            burnedItemsView.setLifecycleOwner(this);
        }

        final @Nullable SessionButtonsView sessionButtonsView = findViewById(R.id.sessionButtonsView);
        if (sessionButtonsView != null) {
            sessionButtonsView.setLifecycleOwner(this);
        }

        final @Nullable SrsBreakDownView srsBreakDownView = findViewById(R.id.srsBreakDownView);
        if (srsBreakDownView != null) {
            srsBreakDownView.setLifecycleOwner(this);
        }

        final @Nullable SyncProgressView syncProgressView = findViewById(R.id.syncProgressView);
        if (syncProgressView != null) {
            syncProgressView.setLifecycleOwner(this);
        }

        final @Nullable UpcomingReviewsView upcomingReviewsView = findViewById(R.id.upcomingReviewsView);
        if (upcomingReviewsView != null) {
            upcomingReviewsView.setLifecycleOwner(this);
        }

        final @Nullable TimeLineBarChart timeLineBarChart = findViewById(R.id.timeLineBarChart);
        if (timeLineBarChart != null) {
            timeLineBarChart.setLifecycleOwner(this);
        }
    }

    @Override
    protected void onResumeLocal() {
        BackgroundAlarmReceiver.scheduleOrCancelAlarm();
        BackgroundAlarmReceiverPost19.scheduleOrCancelAlarm();
        BackgroundAlarmReceiverPost23.scheduleOrCancelAlarm();
        BackgroundSyncWorker.scheduleOrCancelWork();

        runAsync(() -> {
            LiveBurnedItems.getInstance().forceUpdate();
            LiveCriticalCondition.getInstance().forceUpdate();
            LiveLevelDuration.getInstance().forceUpdate();
            LiveLevelProgress.getInstance().forceUpdate();
            LiveRecentUnlocks.getInstance().forceUpdate();
            LiveSrsBreakDown.getInstance().forceUpdate();
            LiveTimeLine.getInstance().forceUpdate();
            LiveJoyoProgress.getInstance().forceUpdate();
            LiveJlptProgress.getInstance().forceUpdate();
            BackgroundAlarmReceiver.processAlarm(null);
        });

        keyboardHelpView.setVisibility(!GlobalSettings.Tutorials.getKeyboardHelpDismissed());

        collapseSearchBox();
    }

    @Override
    protected void onPauseLocal() {
        //
    }

    @Override
    protected void enableInteractionLocal() {
        final @Nullable SessionButtonsView view = findViewById(R.id.sessionButtonsView);
        if (view != null) {
            view.enableInteraction();
        }
    }

    @Override
    protected void disableInteractionLocal() {
        final @Nullable SessionButtonsView view = findViewById(R.id.sessionButtonsView);
        if (view != null) {
            view.disableInteraction();
        }
    }

    /**
     * Handler for the API error retry button.
     *
     * @param view the button
     */
    @SuppressWarnings("MethodMayBeStatic")
    public void retryApiError(@SuppressWarnings("unused") final View view) {
        safe(() -> JobRunnerService.schedule(RetryApiErrorJob.class, ""));
    }

    /**
     * Handler for the API error settings button.
     *
     * @param view the button
     */
    public void goToSettings(@SuppressWarnings("unused") final View view) {
        safe(() -> goToPreferencesActivity("api_settings"));
    }

    /**
     * Handler for the keyboard help button.
     *
     * @param view the button
     */
    public void viewKeyboardHelp(@SuppressWarnings("unused") final View view) {
        safe(() -> goToActivity(KeyboardHelpActivity.class));
    }

    /**
     * Handler for dismissing the keyboard help view.
     *
     * @param view the button
     */
    public void dismissKeyboardHelp(@SuppressWarnings("unused") final View view) {
        safe(() -> {
            GlobalSettings.Tutorials.setKeyboardHelpDismissed(true);
            keyboardHelpView.setVisibility(false);
        });
    }

    /**
     * Handler for the start lessons button.
     *
     * @param view the button
     */
    public void startLessonSession(@SuppressWarnings("unused") final View view) {
        safe(() -> {
            if (!interactionEnabled) {
                return;
            }
            disableInteraction();
            final TimeLine timeLine = LiveTimeLine.getInstance().get();
            if (timeLine.hasAvailableLessons()) {
                runAsync(this, () -> {
                    Session.getInstance().startNewLessonSession(timeLine.getAvailableLessons());
                    return null;
                }, result -> goToActivity(SessionActivity.class));
            }
            else {
                enableInteraction();
            }
        });
    }

    /**
     * Handler for the start reviews button.
     *
     * @param view the button
     */
    public void startReviewSession(@SuppressWarnings("unused") final View view) {
        safe(() -> {
            if (!interactionEnabled) {
                return;
            }
            disableInteraction();
            final TimeLine timeLine = LiveTimeLine.getInstance().get();
            if (timeLine.hasAvailableReviews()) {
                runAsync(this, () -> {
                    Session.getInstance().startNewReviewSession(timeLine.getAvailableReviews());
                    return null;
                }, result -> goToActivity(SessionActivity.class));
            }
            else {
                enableInteraction();
            }
        });
    }

    /**
     * Handler for the resume session button.
     *
     * @param view the button
     */
    public void resumeSession(@SuppressWarnings("unused") final View view) {
        safe(() -> {
            if (!interactionEnabled) {
                return;
            }
            disableInteraction();
            if (Session.getInstance().isInactive()) {
                enableInteraction();
            }
            else {
                goToActivity(SessionActivity.class);
            }
        });
    }
}
