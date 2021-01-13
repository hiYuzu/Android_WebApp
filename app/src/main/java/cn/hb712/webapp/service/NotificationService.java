package cn.hb712.webapp.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cn.hb712.webapp.LoginActivity;
import cn.hb712.webapp.MainApplication;
import cn.hb712.webapp.R;
import cn.hb712.webapp.WebServiceClient;

public class NotificationService extends Service {

    private String channelId = "channel_001";
    private CharSequence channelName = "channelName";
    private String alarmMessageParam = "";

    // 获取消息线程
    private MessageThread messageThread = null;

    // 点击查看
    Notification.Builder builder = null;
    private PendingIntent messagePendingIntent = null;

    // 通知栏消息
    private int messageNotificationID = 1;
    private Notification messageNotification = null;
    private NotificationManager messageNotificationManager = null;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 初始化
        messageNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent messageIntent = new Intent(this, LoginActivity.class);
        messageIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        messagePendingIntent = PendingIntent.getActivity(this, 0, messageIntent, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            messageNotificationManager.createNotificationChannel(mChannel);

        }
        builder = new Notification.Builder(this);
        // 开启线程
        messageThread = new MessageThread();
        messageThread.start();

        return super.onStartCommand(intent, flags, startId);
    }

//    /**
//     * reboot service when destroy
//     */
//    @Override
//    public void onDestroy(){
//        messageThread.interrupt();
//        Intent service = new Intent(this, NotificationService.class);
//        this.startService(service);
//        super.onDestroy();
//    }

    /**
     * 从服务器端获取消息
     */
    class MessageThread extends Thread {
        // 设置是否循环推送
        private boolean isRunning = true;

        @RequiresApi(api = Build.VERSION_CODES.O)
        public void run() {
            while (isRunning) {
                try {
                    //间隔60秒
                    Thread.sleep(60000);
                    // 获取服务器消息
                    SharedPreferences sp = getSharedPreferences("alarm", Context.MODE_PRIVATE);
                    String alarmId = sp.getString("alarmId", null);
                    getServerMessage(alarmId);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取报警消息
     *
     * @return 返回服务器要推送的消息，否则如果为空的话，不推送
     */
    public String getServerMessage(final String alarmId) {
        MainApplication.getInstance().getWebServiceClient().runGetAlarmTask(new WebServiceClient.TaskHandler() {
            @Override
            public void onStart() {

            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onSuccess(JSONObject obj) {
                alarmMessageParam = loadAlarm(alarmId, obj);
                sendNotifyMessage(alarmMessageParam);
            }

            @Override
            public void onFailed(String errMsg) {

            }
        }, alarmId);
        return alarmMessageParam;
    }

    private String loadAlarm(String alarmId, JSONObject viewJson) {
        String alarmMessage = "";
        try {
            String maxAlarmId = alarmId;
            String total = viewJson.getString("total");
            JSONArray sub_alarms = viewJson.getJSONArray("rows");
            for (int i = 0; i < sub_alarms.length(); i++) {
                JSONObject sub_page = sub_alarms.getJSONObject(i);
                String alarmTempId = "";
                if (!sub_page.isNull("maxAlarmId")) {
                    alarmTempId = sub_page.getString("maxAlarmId");
                }
                String deviceName = sub_page.getString("deviceName");
                String statusInfo = sub_page.getString("statusInfo");
                alarmMessage += deviceName + ":" + statusInfo + ".";
                if (alarmTempId != null && alarmTempId != "") {
                    if (maxAlarmId != null && maxAlarmId != "") {
                        if (Double.valueOf(alarmTempId) > Double.valueOf(maxAlarmId)) {
                            maxAlarmId = alarmTempId;
                        }
                    } else {
                        maxAlarmId = alarmTempId;
                    }
                }
            }
            if (maxAlarmId != null && maxAlarmId != "") {
                SharedPreferences sp = getSharedPreferences("alarm", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("alarmId", maxAlarmId);
                editor.commit();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return alarmMessage;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendNotifyMessage(String serverMessage) {
        if (serverMessage != null && !"".equals(serverMessage)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                messageNotification = builder.setChannelId(channelId).setContentTitle("存在一条新的报警")
                        .setDefaults( Notification.DEFAULT_ALL)
                        .setContentText(serverMessage)
                        .setSmallIcon(R.mipmap.ic_launcher_round).build();
            } else {
                builder.setContentTitle("存在一条新的报警").setContentText(serverMessage)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setOngoing(true)
                        .setChannelId(channelId)
                        .setDefaults(Notification.DEFAULT_ALL);
            }
            builder.setContentIntent(messagePendingIntent);//点击进入应用
            messageNotification.ledARGB = Color.GREEN;
            messageNotification.ledOnMS = 1000;
            messageNotification.ledOffMS = 1000;
            messageNotification.flags = Notification.FLAG_SHOW_LIGHTS;//闪灯
            messageNotification.defaults = Notification.DEFAULT_ALL;// 设置默认为系统声音
            messageNotification.flags = Notification.FLAG_AUTO_CANCEL;//点击通知后通知消失
            messageNotificationManager.notify(messageNotificationID, messageNotification);
            messageNotificationID++;
        }
    }

}
