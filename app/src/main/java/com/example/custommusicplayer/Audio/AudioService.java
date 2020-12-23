package com.example.custommusicplayer.Audio;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;

import com.example.custommusicplayer.DAO.BroadCastActions;
import com.example.custommusicplayer.DAO.CommandActions;
import com.example.custommusicplayer.DAO.MusicData;
import com.example.custommusicplayer.Notification.NotificationPlayer;

import java.util.ArrayList;

public class AudioService extends Service {
    private final IBinder mBinder = new AudioServiceBinder();
    private MediaPlayer mMediaPlayer;
    private NotificationPlayer notificationPlayer;

    private boolean isPrepared;
    private ArrayList<Long> mAudioIds = new ArrayList<>();
    private int mCurrentPosition;
    private MusicData musicData;

    // AudioService에서 선언된 함수들을 interface에서 사용할 수 있게 하기 위한 통신채널을 생성하는 부분입니다
    public class AudioServiceBinder extends Binder {
        AudioService getService() {
            return AudioService.this;
        }
    }

    public void setPlayList(ArrayList<Long> audioIds) {
        if(mAudioIds.size() != audioIds.size()) {
            if(!mAudioIds.equals(audioIds)) {
                mAudioIds.clear();
                mAudioIds.addAll(audioIds);
            }
        }
    }

    // MainActivity에서 선택된 위치의 노래를 재생하기 위한 함수입니다
    private void queryAudioItem(int position) {
        mCurrentPosition = position;
        long audioId = mAudioIds.get(position);
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media._ID + " = ?";
        String[] selectionArgs = {String.valueOf(audioId)};
        Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                musicData = new MusicData(
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns._ID)),
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE)),
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)),
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)),
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID)),
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)),
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA))
                );
            }
            cursor.close();
        }
    }

    // PlayMusicActivity에서 Progressbar와 현재 재생시간을 다루기 위한 함수입니다
    public int getCurrentDuration() {
        if(mMediaPlayer != null) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    // PlayMusicActivity에서 Progressbar와 현재 재생시간을 다루기 위한 함수입니다
    public int getDuration() {
        if(mMediaPlayer != null) {
            return mMediaPlayer.getDuration();
        }
        return 0;
    }

    public MusicData getAudioItem() {
        return musicData;
    }

    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    // 현재 재생하고 싶은 노래를 mediaPlayer에 준비시키는 함수입니다.
    // prepareAsync를 통해 비동기적으로 처리하였습니다.
    private void prepare() {
        try {
            Uri musicURI = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + musicData.getId());
            mMediaPlayer.setDataSource(this, musicURI);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 실질적으로 노래를 재생시키는 함수가 아닌, 해당 노래를 재생시키기 위한 준비를 하는 함수입니다.
    public void play(int position) {
        queryAudioItem(position);
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        prepare();
    }

    // 실질적으로 노래를 재생시키는 함수입니다.
    public void play() {
        if (isPrepared) {
            mMediaPlayer.start();
            sendBroadcast(new Intent(BroadCastActions.PLAY_STATE_CHANGED));
            updateNotificationPlayer();
        }
    }

    public void pause() {
        if (isPrepared) {
            mMediaPlayer.pause();
            sendBroadcast(new Intent(BroadCastActions.PLAY_STATE_CHANGED));
            updateNotificationPlayer();
        }
    }

    public void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
    }

    public void forward() {
        if (mAudioIds.size() - 1 > mCurrentPosition) {
            mCurrentPosition++; // 다음 포지션으로 이동.
        } else {
            mCurrentPosition = 0; // 처음 포지션으로 이동.
        }
        play(mCurrentPosition);
        updateNotificationPlayer();
        // 노래가 바뀌었을 때, PlayMusicActivity의 UI를 업데이트하기 위한 코드입니다
        sendBroadcast(new Intent(BroadCastActions.CHANGE_MUSIC));
    }

    public void rewind() {
        if (mCurrentPosition > 0) {
            mCurrentPosition--; // 이전 포지션으로 이동.
        } else {
            mCurrentPosition = mAudioIds.size() - 1; // 마지막 포지션으로 이동.
        }
        play(mCurrentPosition);
        updateNotificationPlayer();
        // 노래가 바뀌었을 때, PlayMusicActivity의 UI를 업데이트하기 위한 코드입니다
        sendBroadcast(new Intent(BroadCastActions.CHANGE_MUSIC));
    }

    private void updateNotificationPlayer() {
        if(notificationPlayer != null) {
            notificationPlayer.updateNotificationPlayer();
        }
    }

    public void removeNotificationPlayer() {
        if (notificationPlayer != null) {
            notificationPlayer.removeNotificationPlayer();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // RemoteViews에서 들어오는 이벤트들을 처리하는 부분입니다.
        if (intent != null) {
            String action = intent.getAction();
            if (CommandActions.TOGGLE_PLAY.equals(action)) {
                if (isPlaying()) {
                    pause();
                } else {
                    play();
                }
            } else if (CommandActions.REWIND.equals(action)) {
                rewind();
            } else if (CommandActions.FORWARD.equals(action)) {
                forward();
            } else if (CommandActions.CLOSE.equals(action)) {
                pause();
                removeNotificationPlayer();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMediaPlayer = new MediaPlayer();
        // 잠금모드여도 노래가 실행될 수 있게 설정하는 코드입니다(Manifest에 관련된 추가 코드가 있습니다)
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        // 하나의 노래가 다 재생되었을 때 실행되는 리스너입니다
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                isPrepared = false;
                forward();
                updateNotificationPlayer();
            }
        });
        // MediaPlayer의 준비가 완료되면 주어진 노래를 실행시키는 코드입니다.
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                isPrepared = true;
                mp.start();
                sendBroadcast(new Intent(BroadCastActions.PREPARED));
                updateNotificationPlayer();
            }
        });
        // 에러처리 코드입니다
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                isPrepared = false;
                sendBroadcast(new Intent(BroadCastActions.PLAY_STATE_CHANGED));
                updateNotificationPlayer();
                return false;
            }
        });

        notificationPlayer = new NotificationPlayer(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
}
