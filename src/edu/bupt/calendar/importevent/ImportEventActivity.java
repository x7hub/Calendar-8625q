package edu.bupt.calendar.importevent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.android.calendarcommon.ICalendar;
import com.android.calendarcommon.ICalendar.FormatException;

import edu.bupt.calendar.CalendarEventModel;
import edu.bupt.calendar.GeneralPreferences;
import edu.bupt.calendar.R;
import edu.bupt.calendar.Utils;
import edu.bupt.calendar.event.EditEventHelper;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ImportEventActivity extends Activity {
    private String TAG = "ImportEventActivity";
    private View view;
    private TextView textviewTitle;
    private TextView textViewDatetime;
    private TextView textViewDateendtime;
    private TextView textViewRepeat;
    private TextView textViewWhere;
    private TextView textViewDisc;
    private ICalendar.Component parent;
    private ICalendar.Component child;
    private Context context = this;
    private CalendarEventModel model;
    private int event_id = 1;
    private String event_title = null;
    private long event_datetime = 0;
    private String event_where = null;
    private String event_disc = null;
    private String event_tz = null;
    private long event_dateendtime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_event);

        textviewTitle = (TextView) findViewById(R.id.title);
        textViewDatetime = (TextView) findViewById(R.id.when_datetime);
        textViewWhere = (TextView) findViewById(R.id.where);
        textViewDisc = (TextView) findViewById(R.id.disc);

        parent = getEventFromString(getStringFromFile());
        // for (ICalendar.Component child : parent.getComponents()) {
        // }

        // just test first event
        child = parent.getComponents().get(0);
        getDetails(child);
        textviewTitle.setText(event_title);
        textViewDatetime.setText(String.valueOf(event_datetime));
        textViewWhere.setText(event_where);
        textViewDisc.setText(event_disc);
    }

    /** zzz */
    private String getStringFromFile() {
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            Uri uri = Uri.parse(data.toString());
            String filename = uri.getPath();
            File file = new File(filename);
            if (file.exists()) {
                FileInputStream fin;
                try {
                    fin = new FileInputStream(file);
                    int buffersize = fin.available();
                    byte buffer[] = new byte[buffersize];
                    fin.read(buffer);
                    fin.close();
                    String vcal = new String(buffer);

                    // ICalendar.Component parent =
                    // ICalendar.parseCalendar(vcal);
                    //
                    // Log.i(TAG, parent.getName());
                    // Log.i(TAG, parent.getPropertyNames().toString());
                    // for (ICalendar.Component child : parent.getComponents())
                    // {
                    // Log.i(TAG, child.toString());
                    //
                    // }
                    Log.i(TAG, vcal);
                    return vcal;

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private void getDetails(ICalendar.Component c) {
        event_title = c.getFirstProperty("SUMMARY").getValue();

        ICalendar.Property dtstart_prop = c.getFirstProperty("DTSTART");
        ICalendar.Property dtend_prop = c.getFirstProperty("DTEND");
        event_tz = dtstart_prop.getFirstParameter("TZID").value;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                "yyyyMMdd'T'HHmmss");
        try {
            event_datetime = simpleDateFormat.parse(dtstart_prop.getValue())
                    .getTime();
            event_dateendtime = simpleDateFormat.parse(dtend_prop.getValue())
                    .getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        event_where = child.getFirstProperty("LOCATION").getValue();
        event_disc = child.getFirstProperty("DISCRIPTION").getValue();
        return;
    }

    private ICalendar.Component getEventFromString(String s) {
        ICalendar.Component c = null;
        try {
            c = ICalendar.parseCalendar(s);
        } catch (FormatException e) {
            e.printStackTrace();
        }
        return c;
    }

    // private CalendarEventModel buildTestModel() {
    // model = new CalendarEventModel();
    // model.mId = event_id;
    // model.mTitle = event_title;
    // model.mDescription = event_disc;
    // model.mLocation = event_where;
    // // model.mAllDay = true;
    // // model.mHasAlarm = false;
    // model.mCalendarId = 0;
    // model.mStart = event_datetime; // Monday, May 3rd, local Time
    // // model.mDuration = "P3652421990D";
    // // The model uses the local timezone for allday
    // model.mTimezone = "UTC";
    // // model.mRrule = "FREQ=DAILY;WKST=SU";
    // // model.mSyncId = "unique per calendar stuff";
    // model.mAvailability = 0;
    // model.mAccessLevel = 2; // This is one less than the values written if
    // // >0
    // model.mOwnerAccount = Utils.getSharedPreference(context,
    // GeneralPreferences.KEY_DEFAULT_CALENDAR, "");
    // // model.mHasAttendeeData = true;
    // // model.mEventStatus = Events.STATUS_CONFIRMED;
    // // model.mOrganizer = "organizer@gmail.com";
    // // model.mGuestsCanModify = false;
    // // model.mModelUpdatedWithEventCursor = true;
    // return model;
    // }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.import_event, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_done) {
            Log.d(TAG, "action_done");
            // save
            String calId = "";
            Cursor userCursor = getContentResolver().query(
                    Uri.parse("content://com.android.calendar/calendars"),
                    null, null, null, null);
            if (userCursor.getCount() > 0) {
                userCursor.moveToFirst();
                calId = userCursor.getString(userCursor.getColumnIndex("_id"));

            }
            ContentValues event = new ContentValues();
            event.put("title", event_title);
            event.put("description", event_disc);
            event.put("eventLocation", event_where);
            event.put("calendar_id", calId);
            event.put("dtstart", event_datetime);
            event.put("dtend", event_dateendtime);
            event.put("hasAlarm", 1);
            event.put("eventTimezone", event_tz);

            Uri newEvent = getContentResolver().insert(
                    Uri.parse("content://com.android.calendar/events"), event);
            long id = Long.parseLong(newEvent.getLastPathSegment());
            ContentValues values = new ContentValues();
            values.put("event_id", id);
            // reminder
            values.put("minutes", 10);
            getContentResolver().insert(
                    Uri.parse("content://com.android.calendar/reminders"),
                    values);

            Log.d(TAG, "import success");
            Toast.makeText(this, R.string.title_activity_import_event, Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}