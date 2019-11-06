package com.sjtuopennetwork.shareit.util;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.sjtuopennetwork.shareit.share.HomeActivity;

public class ForeGroundService extends Service {
    public ForeGroundService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        System.out.println("========启动前台服务");

        Intent it=new Intent(this, HomeActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,0,it,0);
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext());
        builder.setContentIntent(pi)
                .setContentTitle("Textile")
                .setContentText("Textile运行中")
                .setWhen(System.currentTimeMillis());
        Notification notification=builder.build();
        notification.defaults=Notification.DEFAULT_SOUND;
        startForeground(108,notification);
    }
}
