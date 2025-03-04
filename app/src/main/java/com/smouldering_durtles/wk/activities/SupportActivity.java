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

import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.proxy.ViewProxy;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.Constants.SUPPORT_DOCUMENT;

/**
 * Simple activity that only shows a big TextView. Shows the app's 'about' information.
 */
public final class SupportActivity extends AbstractActivity {
    /**
     * The constructor.
     */
    public SupportActivity() {
        super(R.layout.activity_about, R.menu.generic_options_menu);
    }

    @Override
    protected void onCreateLocal(final @Nullable Bundle savedInstanceState) {
        final ViewProxy document = new ViewProxy(this, R.id.document);
        document.setTextHtml(SUPPORT_DOCUMENT);
        document.setLinkMovementMethod();
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
}
