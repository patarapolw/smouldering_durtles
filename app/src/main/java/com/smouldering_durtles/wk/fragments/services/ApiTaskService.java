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

package com.smouldering_durtles.wk.fragments.services;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.model.TaskDefinition;
import com.smouldering_durtles.wk.jobs.TickJob;
import com.smouldering_durtles.wk.livedata.LiveFirstTimeSetup;
import com.smouldering_durtles.wk.model.Session;
import com.smouldering_durtles.wk.tasks.ApiTask;

import java.util.Collection;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * A service for running tasks. Tasks are actions that need to run
 * in the background, don't have to run immediately, may take a long time to
 * complete (usually because they are network calls), and must be persisted
 * so they will be executed even across restarts and when errors occur.
 *
 * <p>
 *     Tasks are recorded in the database. This service will loop over them
 *     one by one in priority order, taking into account the current online
 *     status.
 * </p>
 */
public final class ApiTaskService extends Service {
    /**
     * A single dummy object to synchronize on, to make sure the background sync doesn't
     * overlap with this.
     */
    private static final Object TASK_MONITOR = new Object();

    /**
     * Schedule a run of the service to be executed on a background thread.
     * This is regularly called from job housekeeping.
     */
    public static void schedule() {
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ApiTaskWorker.class).build();
        WorkManager.getInstance(WkApplication.getInstance()).enqueue(workRequest);
    }

    private static void runTasksImpl() throws Exception {
        final AppDatabase db = WkApplication.getDatabase();
        while (db.hasPendingApiTasks()) {
            //noinspection SynchronizationOnStaticField
            synchronized (TASK_MONITOR) {
                final @Nullable TaskDefinition taskDefinition = db.taskDefinitionDao().getNextTaskDefinition();
                if (taskDefinition == null) {
                    break;
                }

                final @Nullable Class<? extends ApiTask> clas = taskDefinition.getTaskClass();
                if (clas == null) {
                    db.taskDefinitionDao().deleteTaskDefinition(taskDefinition);
                }
                else {
                    final ApiTask apiTask = taskDefinition.getTaskClass()
                            .getConstructor(TaskDefinition.class)
                            .newInstance(taskDefinition);

                    if (!apiTask.canRun()) {
                        break;
                    }

                    apiTask.run();
                }
            }
        }
        if (db.taskDefinitionDao().getApiCount() == 0) {
            if (GlobalSettings.getFirstTimeSetup() == 0) {
                GlobalSettings.setFirstTimeSetup(1);
                LiveFirstTimeSetup.getInstance().forceUpdate();
            }
            if (Session.getInstance().isInactive()) {
                final Collection<Long> assignmentSubjectIds = db.subjectViewsDao().getPatchedAssignments();
                if (!assignmentSubjectIds.isEmpty()) {
                    db.assertGetPatchedAssignmentsTask(assignmentSubjectIds);
                }
                final Collection<Long> reviewStatisticsSubjectIds = db.subjectViewsDao().getPatchedReviewStatistics();
                if (!reviewStatisticsSubjectIds.isEmpty()) {
                    db.assertGetPatchedReviewStatisticsTask(reviewStatisticsSubjectIds);
                }
                final Collection<Long> studyMaterialsSubjectIds = db.subjectViewsDao().getPatchedStudyMaterials();
                if (!studyMaterialsSubjectIds.isEmpty()) {
                    db.assertGetPatchedStudyMaterialsTask(studyMaterialsSubjectIds);
                }
                if (db.propertiesDao().getForceLateRefresh()) {
                    db.propertiesDao().setForceLateRefresh(false);
                    db.assertRefreshForAllModels();
                    db.assertGetLevelProgressionTask();
                    JobRunnerService.schedule(TickJob.class, "");
                }
            }
        }
    }

    /**
     * Loop through all available tasks and execute them one by one, taking into
     * account the priority order and online status.
     *
     * <p>
     *     Each task is response for removing itself from the database when
     *     finished. Until then, the task will be retried indefinitely.
     * </p>
     */
    public static void runTasks() {
        safe(ApiTaskService::runTasksImpl);
    }

    @androidx.annotation.Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

