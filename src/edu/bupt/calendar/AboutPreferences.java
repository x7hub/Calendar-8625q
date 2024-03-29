/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.bupt.calendar;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * 北邮ANT实验室
 * zzz
 * 
 * '设置->关于'指向的页面，展示出软件版本号
 * 
 * 此文件取自codeaurora提供的适用于高通8625Q的android 4.1.2源码，未作修改
 * 
 * */

public class AboutPreferences extends PreferenceFragment {
    private static final String BUILD_VERSION = "build_version";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.about_preferences);

        final Activity activity = getActivity();
        try {
            final PackageInfo packageInfo =
                activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            findPreference(BUILD_VERSION).setSummary(packageInfo.versionName);
        } catch (NameNotFoundException e) {
            findPreference(BUILD_VERSION).setSummary("?");
        }
    }
}