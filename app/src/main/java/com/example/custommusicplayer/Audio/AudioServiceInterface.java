package com.example.custommusicplayer.Audio;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.custommusicplayer.DAO.MusicData;

import java.util.ArrayList;

// AudioService와 Activity 간의 상호작용을 하는 인터페이스 선언(매개체 역할을 해서 인터페이스라 이름을 지었지만 interface를 상속하지는 않습니다)
public class AudioServiceInterface {
    private ServiceConnection mServiceConnection;
    private AudioService mService;

    // 생성자를 통해 해당 인터페이스와 AuioService간의 연동을 실행합니다
    public AudioServiceInterface(Context context) {
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mService = ((AudioService.AudioServiceBinder) service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mServiceConnection = null;
                mService = null;
            }
        };
        context.bindService(new Intent(context, AudioService.class)
                .setPackage(context.getPackageName()), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    // 이 밑의 내용들은 구현 액티비티에서 AudioService의 함수 및 필드를 사용하기 위해 선언한 함수들입니다.
    public MusicData getAudioItem() {
        if(mService != null) {
            return mService.getAudioItem();
        }
        return null;
    }

    public void togglePlay() {
        if(isPlaying()) {
            mService.pause();
        } else {
            mService.play();
        }
    }

    public boolean isPlaying() {
        if (mService != null) {
            return mService.isPlaying();
        }
        return false;
    }

    public int getCurrentDuration() {
        if (mService != null) {
            return mService.getCurrentDuration();
        }
        return 0;
    }

    public int getDuration() {
        if (mService != null) {
            return mService.getDuration();
        }
        return 0;
    }

    public void setPlayList(ArrayList<Long> audioIds) {
        if (mService != null) {
            mService.setPlayList(audioIds);
        }
    }

    public void play(int position) {
        if (mService != null) {
            mService.play(position);
        }
    }


    public void pause() {
        if (mService != null) {
            mService.pause();
        }
    }

    public void forward() {
        if (mService != null) {
            mService.forward();
        }
    }

    public void rewind() {
        if (mService != null) {
            mService.rewind();
        }
    }

    public void stop() {
        if (mService != null) {
            mService.removeNotificationPlayer();
            mService.stop();
        }
    }
}
