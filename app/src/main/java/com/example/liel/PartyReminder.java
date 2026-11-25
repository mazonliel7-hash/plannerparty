
package com.example.liel;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PartyReminder {

    // מצב בדיקה: true = התראה תוך דקה
    // אחרי הבדיקה לשנות ל-false
    private static final boolean TEST_MODE = true;

    public static void schedule(
            Context context,
            String partyId,
            String partyName,
            String partyDate) {

        long reminderTime;

        if (TEST_MODE) {

            // בדיקה: התראה בעוד דקה
            reminderTime = System.currentTimeMillis()
                    + 60 * 1000;

        } else {

            try {
                SimpleDateFormat format =
                        new SimpleDateFormat(
                                "dd/MM/yyyy",
                                Locale.getDefault());

                format.setLenient(false);

                Date parsedDate = format.parse(partyDate);

                if (parsedDate == null) {
                    return;
                }

                Calendar reminder = Calendar.getInstance();
                reminder.setTime(parsedDate);

                // יום לפני המסיבה בשעה 10:00
                reminder.add(Calendar.DAY_OF_YEAR, -1);
                reminder.set(Calendar.HOUR_OF_DAY, 10);
                reminder.set(Calendar.MINUTE, 0);
                reminder.set(Calendar.SECOND, 0);
                reminder.set(Calendar.MILLISECOND, 0);

                reminderTime = reminder.getTimeInMillis();

                // אם זמן התזכורת כבר עבר, לא קובעים אותה
                if (reminderTime <= System.currentTimeMillis()) {
                    return;
                }

            } catch (ParseException e) {
                e.printStackTrace();
                return;
            }
        }

        Intent intent = new Intent(
                context,
                PartyReminderReceiver.class);

        intent.putExtra("partyId", partyId);
        intent.putExtra("partyName", partyName);
        intent.putExtra("partyDate", partyDate);

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        partyId.hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent);
        }
    }

    public static void cancel(
            Context context,
            String partyId) {

        Intent intent = new Intent(
                context,
                PartyReminderReceiver.class);

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        partyId.hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        pendingIntent.cancel();
    }
}
