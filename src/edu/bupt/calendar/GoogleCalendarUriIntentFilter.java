/*
**
** Copyright 2009, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** See the License for the specific language governing permissions and
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** limitations under the License.
*/

package edu.bupt.calendar;

import static android.provider.CalendarContract.Attendees.ATTENDEE_STATUS;
import static android.provider.CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED;
import static android.provider.CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED;
import static android.provider.CalendarContract.Attendees.ATTENDEE_STATUS_NONE;
import static android.provider.CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE;
import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.android.calendarcommon.DateException;
import com.android.calendarcommon.Duration;

/**
 * 北邮ANT实验室
 * zzz
 * 
 * 此文件取自codeaurora提供的适用于高通8625Q的android 4.1.2源码，未作修改
 * 
 * */

public class GoogleCalendarUriIntentFilter extends Activity {
    private static final String TAG = "GoogleCalendarUriIntentFilter";
    static final boolean debug = false;

    private static final int EVENT_INDEX_ID = 0;
    private static final int EVENT_INDEX_START = 1;
    private static final int EVENT_INDEX_END = 2;
    private static final int EVENT_INDEX_DURATION = 3;

    private static final String[] EVENT_PROJECTION = new String[] {
        Events._ID,      // 0
        Events.DTSTART,  // 1
        Events.DTEND,    // 2
        Events.DURATION, // 3
    };

    /**
     * Extracts the ID and calendar email from the eid parameter of a URI.
     *
     * The URI contains an "eid" parameter, which is comprised of an ID, followed
     * by a space, followed by the calendar email address. The domain is sometimes
     * shortened. See the switch statement. This is Base64-encoded before being
     * added to the URI.
     *
     * @param uri incoming request
     * @return the decoded event ID and calendar email
     */
    private String[] extractEidAndEmail(Uri uri) {
        try {
            String eidParam = uri.getQueryParameter("eid");
            if (debug) Log.d(TAG, "eid=" + eidParam );
            if (eidParam == null) {
                return null;
            }

            byte[] decodedBytes = Base64.decode(eidParam, Base64.DEFAULT);
            if (debug) Log.d(TAG, "decoded eid=" + new String(decodedBytes) );

            for (int spacePosn = 0; spacePosn < decodedBytes.length; spacePosn++) {
                if (decodedBytes[spacePosn] == ' ') {
                    int emailLen = decodedBytes.length - spacePosn - 1;
                    if (spacePosn == 0 || emailLen < 3) {
                        break;
                    }

                    String domain = null;
                    if (decodedBytes[decodedBytes.length - 2] == '@') {
                        // Drop the special one character domain
                        emailLen--;

                        switch(decodedBytes[decodedBytes.length - 1]) {
                            case 'm':
                                domain = "gmail.com";
                                break;
                            case 'g':
                                domain = "group.calendar.google.com";
                                break;
                            case 'h':
                                domain = "holiday.calendar.google.com";
                                break;
                            case 'i':
                                domain = "import.calendar.google.com";
                                break;
                            case 'v':
                                domain = "group.v.calendar.google.com";
                                break;
                            default:
                                Log.wtf(TAG, "Unexpected one letter domain: "
                                        + decodedBytes[decodedBytes.length - 1]);
                                // Add sql wild card char to handle new cases
                                // that we don't know about.
                                domain = "%";
                                break;
                        }
                    }

                    String eid = new String(decodedBytes, 0, spacePosn);
                    String email = new String(decodedBytes, spacePosn + 1, emailLen);
                    if (debug) Log.d(TAG, "eid=   " + eid );
                    if (debug) Log.d(TAG, "email= " + email );
                    if (debug) Log.d(TAG, "domain=" + domain );
                    if (domain != null) {
                        email += domain;
                    }

                    return new String[] { eid, email };
                }
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Punting malformed URI " + uri);
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = getIntent();
        if (intent != null) {
            Uri uri = intent.getData();
            if (uri != null) {
                String[] eidParts = extractEidAndEmail(uri);
                if (debug) Log.d(TAG, "eidParts=" + eidParts );

                if (eidParts != null) {
                    final String selection = Events._SYNC_ID + " LIKE \"%" + eidParts[0]
                            + "\" AND " + Calendars.OWNER_ACCOUNT + " LIKE \"" + eidParts[1] + "\"";

                    if (debug) Log.d(TAG, "selection: " + selection);
                    Cursor eventCursor = getContentResolver().query(Events.CONTENT_URI,
                            EVENT_PROJECTION, selection, null,
                            Calendars.CALENDAR_ACCESS_LEVEL + " desc");
                    if (debug) Log.d(TAG, "Found: " + eventCursor.getCount());

                    if (eventCursor != null && eventCursor.getCount() > 0) {
                        if (eventCursor.getCount() > 1) {
                            Log.i(TAG, "NOTE: found " + eventCursor.getCount()
                                    + " matches on event with id='" + eidParts[0] + "'");
                            // Don't print eidPart[1] as it contains the user's PII
                        }

                        // Get info from Cursor
                        while (eventCursor.moveToNext()) {
                            int eventId = eventCursor.getInt(EVENT_INDEX_ID);
                            long startMillis = eventCursor.getLong(EVENT_INDEX_START);
                            long endMillis = eventCursor.getLong(EVENT_INDEX_END);
                            if (debug) Log.d(TAG, "_id: " + eventCursor.getLong(EVENT_INDEX_ID));
                            if (debug) Log.d(TAG, "startMillis: " + startMillis);
                            if (debug) Log.d(TAG, "endMillis:   " + endMillis);

                            if (endMillis == 0) {
                                String duration = eventCursor.getString(EVENT_INDEX_DURATION);
                                if (debug) Log.d(TAG, "duration:    " + duration);
                                if (TextUtils.isEmpty(duration)) {
                                    continue;
                                }

                                try {
                                    Duration d = new Duration();
                                    d.parse(duration);
                                    endMillis = startMillis + d.getMillis();
                                    if (debug) Log.d(TAG, "startMillis! " + startMillis);
                                    if (debug) Log.d(TAG, "endMillis!   " + endMillis);
                                    if (endMillis < startMillis) {
                                        continue;
                                    }
                                } catch (DateException e) {
                                    if (debug) Log.d(TAG, "duration:" + e.toString());
                                    continue;
                                }
                            }

                            eventCursor.close();
                            eventCursor = null;

                            // Pick up attendee status action from uri clicked
                            int attendeeStatus = ATTENDEE_STATUS_NONE;
                            if ("RESPOND".equals(uri.getQueryParameter("action"))) {
                                try {
                                    switch (Integer.parseInt(uri.getQueryParameter("rst"))) {
                                    case 1: // Yes
                                        attendeeStatus = ATTENDEE_STATUS_ACCEPTED;
                                        break;
                                    case 2: // No
                                        attendeeStatus = ATTENDEE_STATUS_DECLINED;
                                        break;
                                    case 3: // Maybe
                                        attendeeStatus = ATTENDEE_STATUS_TENTATIVE;
                                        break;
                                    }
                                } catch (NumberFormatException e) {
                                    // ignore this error as if the response code
                                    // wasn't in the uri.
                                }
                            }

                            // Send intent to calendar app
                            Uri calendarUri = ContentUris.withAppendedId(Events.CONTENT_URI,
                                    eventId);
                            intent = new Intent(Intent.ACTION_VIEW, calendarUri);
                            intent.setClass(this, EventInfoActivity.class);
                            intent.putExtra(EXTRA_EVENT_BEGIN_TIME, startMillis);
                            intent.putExtra(EXTRA_EVENT_END_TIME, endMillis);
                            if (attendeeStatus != ATTENDEE_STATUS_NONE) {
                                intent.putExtra(ATTENDEE_STATUS, attendeeStatus);
                            }
                            startActivity(intent);
                            finish();
                            return;
                        }
                    }
                    if (eventCursor != null) eventCursor.close();
                }
            }

            // Can't handle the intent. Pass it on to the next Activity.
            try {
                startNextMatchingActivity(intent);
            } catch (ActivityNotFoundException ex) {
                // no browser installed? Just drop it.
            }
        }
        finish();
    }
}
