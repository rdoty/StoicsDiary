package com.appollonius.stoicsdiary;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * 2018.02.21 rdoty
 * From: https://github.com/BILLyTheLiTTle/AndroidProject_Shortcuts
 */

public class ChoiceNotification extends Notification {
    private Context ctx;
    private NotificationManager mNotificationManager;

    public ChoiceNotification(Context ctx){
        super();
        this.ctx=ctx;
        String ns = Context.NOTIFICATION_SERVICE;
        mNotificationManager = (NotificationManager) ctx.getSystemService(ns);

        long when = System.currentTimeMillis();
        Notification.Builder builder = new Notification.Builder(ctx, "some_channel_id");
        Notification notification = builder.build();
        notification.when = when;
        notification.tickerText = tickerText;
        //notification.setSmallIcon(R.drawable.ic_info_white_24dp);

        RemoteViews contentView=new RemoteViews(ctx.getPackageName(), R.layout.notification_selection);

        //set the button listeners
        setListeners(contentView);

        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        CharSequence contentTitle = "Make your choice";
        mNotificationManager.notify(548853, notification);
    }

    private void setListeners(RemoteViews view) {
        // Choose yes listener
        Intent yesChoice = new Intent(ctx, com.appollonius.stoicsdiary.StoicActivity.class);
        yesChoice.putExtra("DO", "yes");
        PendingIntent pChooseYes = PendingIntent.getActivity(ctx, 5, yesChoice, 0);
        view.setOnClickPendingIntent(R.id.BUTTON_YES, pChooseYes);

        // Choose no listener
        Intent noChoice = new Intent(ctx, com.appollonius.stoicsdiary.StoicActivity.class);
        noChoice.putExtra("DO", "no");
        PendingIntent pChooseNo = PendingIntent.getActivity(ctx, 3, noChoice, 0);
        view.setOnClickPendingIntent(R.id.BUTTON_NO, pChooseNo);
    }
}
