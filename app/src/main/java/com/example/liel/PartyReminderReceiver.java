
package com.example.liel;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class PartyReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "party_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {

        String partyName = intent.getStringExtra("partyName");
        String partyDate = intent.getStringExtra("partyDate");
        String partyId = intent.getStringExtra("partyId");

        if (partyName == null) {
            partyName = "המסיבה שלך";
        }

        if (partyDate == null) {
            partyDate = "";
        }

        createNotificationChannel(context);

        // בדיקת הרשאה באנדרואיד 13 ומעלה
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent openAppIntent =
                new Intent(context, HomeActivity.class);

        openAppIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                200,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        String message =
                "מחר המסיבה של " + partyName
                        + (partyDate.isEmpty()
                        ? ""
                        : "\nתאריך: " + partyDate)
                        + "\nכדאי לבדוק שהכול מוכן 💗";

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("🎉 תזכורת למסיבה!")
                        .setContentText("מחר המסיבה של " + partyName)
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(message)
                        )
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(contentIntent);

        int notificationId = partyId == null
                ? 100
                : partyId.hashCode();

        NotificationManagerCompat.from(context).notify(
                notificationId,
                builder.build()
        );
    }

    private void createNotificationChannel(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "תזכורות למסיבות",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    "תזכורות לפני מסיבות באפליקציית Party Planner"
            );

            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
