package com.example.custommusicplayer.Notification;

import android.annotation.SuppressLint;
//import android.app.Notification;
//import android.app.NotificationManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bumptech.glide.Glide;
import com.example.custommusicplayer.Audio.AudioService;
import com.example.custommusicplayer.DAO.CommandActions;
import com.example.custommusicplayer.PlayMusicActivity;
import com.example.custommusicplayer.R;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class NotificationPlayer {
    private final static int NOTIFICATION_PLAYER_ID = 0x342;
    private AudioService mService;
    private NotificationManager mNotificationManager;
    private NotificationManagerBuilder mNotificationManagerBuilder;

    private boolean isForeground;
    private final static String CHANNEL_ID = "TOY_PROJECT";

    public NotificationPlayer(AudioService service) {
        mService = service;
        mNotificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        // 오레오 버전 이상은 notification channel을 필수로 만들어 주어야 합니다
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "test", NotificationManager.IMPORTANCE_LOW);
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    // AudioService와 notification 연동
    public void updateNotificationPlayer() {
        cancel();
        mNotificationManagerBuilder = new NotificationManagerBuilder();
        mNotificationManagerBuilder.execute();
    }

    // AudioService와 notification 연동 해제
    public void removeNotificationPlayer() {
        cancel();
        mService.stopForeground(true);
        isForeground = false;
    }

    private void cancel() {
        if (mNotificationManagerBuilder != null) {
            mNotificationManagerBuilder.cancel(true);
            mNotificationManagerBuilder = null;
        }
    }

    // notification_player.xml을 RemoteViews로 만들고 각각의 버튼들에 대한 이벤트를 설정한 뒤 알림바에 등록하는 역할과 최초 등록시 서비스를 Foreground로 변경하는 클래스
    // notification builder 설정합니다
    @SuppressLint("StaticFieldLeak")
    private class NotificationManagerBuilder extends AsyncTask<Void, Void, Notification> {
        private RemoteViews mRemoteViews;
        private NotificationCompat.Builder mNotificationBuilder;
        private PendingIntent mMainPendingIntent;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Intent mainActivity = new Intent(mService, PlayMusicActivity.class);
            // notification을 클릭해서 기존에 실행중이던 액티비티를 그대로 띄워주기 위하여 필요한 설정합니다
            mainActivity.setAction(Intent.ACTION_MAIN);
            mainActivity.addCategory(Intent.CATEGORY_LAUNCHER);
            mainActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

            mMainPendingIntent = PendingIntent.getActivity(mService, 0, mainActivity, PendingIntent.FLAG_UPDATE_CURRENT);
            mRemoteViews = createRemoteView(R.layout.notification_player);
            mNotificationBuilder = new NotificationCompat.Builder(mService, CHANNEL_ID);

            // 노래가 재생중이라면 status bar에 뜨는 아이콘을 재생 아이콘으로 설정, 아니라면 일시정지 아이콘으로 설정합니다
            if(mService.isPlaying()) {
                mNotificationBuilder
                        .setSmallIcon(R.drawable.ic_play_music2)
                        .setOngoing(true)
                        .setContentIntent(mMainPendingIntent)
                        .setContent(mRemoteViews);
            } else {
                mNotificationBuilder
                        .setSmallIcon(R.drawable.ic_pause_music)
                        .setOngoing(true)
                        .setContentIntent(mMainPendingIntent)
                        .setContent(mRemoteViews);
            }

            Notification notification = mNotificationBuilder.build();
            notification.contentIntent = mMainPendingIntent;
            if (!isForeground) {
                isForeground = true;
                // 서비스를 Foreground 상태로 만든다
                mService.startForeground(NOTIFICATION_PLAYER_ID, notification);
            }
        }

        // view와 관련된 부분이기 때문에
        @Override
        protected Notification doInBackground(Void... params) {
            mNotificationBuilder.setContent(mRemoteViews);
            mNotificationBuilder.setContentIntent(mMainPendingIntent);
            Notification notification = mNotificationBuilder.build();
            updateRemoteView(mRemoteViews, notification);
            return notification;
        }

        @Override
        protected void onPostExecute(Notification notification) {
            super.onPostExecute(notification);
            try {
                mNotificationManager.notify(NOTIFICATION_PLAYER_ID, notification);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 생성자로 받은 AudioService와 notification_player.xml을 이용하여 RemoteViews를 생성한 뒤, 생성된 RemoteViews에 포함 된 버튼들에 대한 클릭 이벤트도 등록합니다
        // 일반 View들과는 다르게 RemoteViews에 클릭이벤트를 연결할경우 PendingIntent를 사용하게 됩니다. (버튼을 클릭했을 때 호출됩니다.)
        private RemoteViews createRemoteView(int layoutId) {
            // 이 코드를 통해 RemoteViews와 AudioService가 연동되어 remoteView의 버튼들을 클릭할 경우 AudioService의 onStartCommand로 이벤트가 들어오게 됩니다
            RemoteViews remoteView = new RemoteViews(mService.getPackageName(), layoutId);
            // 각각의 CommandActions 들은 manifext에 service의 태그로 등록해주어야 합니다.
            Intent actionTogglePlay = new Intent(CommandActions.TOGGLE_PLAY);
            Intent actionForward = new Intent(CommandActions.FORWARD);
            Intent actionRewind = new Intent(CommandActions.REWIND);
            Intent actionClose = new Intent(CommandActions.CLOSE);
            PendingIntent togglePlay = PendingIntent.getService(mService, 0, actionTogglePlay, 0);
            PendingIntent forward = PendingIntent.getService(mService, 0, actionForward, 0);
            PendingIntent rewind = PendingIntent.getService(mService, 0, actionRewind, 0);
            PendingIntent close = PendingIntent.getService(mService, 0, actionClose, 0);

            remoteView.setOnClickPendingIntent(R.id.btn_play_pause, togglePlay);
            remoteView.setOnClickPendingIntent(R.id.btn_forward, forward);
            remoteView.setOnClickPendingIntent(R.id.btn_rewind, rewind);
            remoteView.setOnClickPendingIntent(R.id.btn_close, close);
            return remoteView;
        }

        // AudioService의 MediaPlayer에서 현재 진행되고 있는 노래의 변화를 인식하고 업데이트하는 함수입니다
        private void updateRemoteView(RemoteViews remoteViews, Notification notification) {
            String title = mService.getAudioItem().getTitle();
            remoteViews.setTextViewText(R.id.txt_title, title);
            Uri albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mService.getAudioItem().getAlbumId());

            // 이미지와 관련된 UI를 바꾸는 작업은 메인 스레드에서만 진행해야 하기 때문에 Handler로 진행합니다
            Handler uiHandler = new Handler(Looper.getMainLooper());
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.i("&&&&&&&&&&&& mService.isPlaying ", (mService.isPlaying()) ? "true" : "false");
                    Picasso
                            .with(mService)
                            .load(albumArtUri)
                            .error(R.drawable.ic_default_music)
                            .into(remoteViews, R.id.img_albumart, NOTIFICATION_PLAYER_ID, notification);

                    if (mService.isPlaying()) {
                        remoteViews.setImageViewResource(R.id.btn_play_pause, R.drawable.ic_pause_music);
                    } else {
                        remoteViews.setImageViewResource(R.id.btn_play_pause, R.drawable.ic_play_music2);
                    }
                }
            });


        }

    }
}
