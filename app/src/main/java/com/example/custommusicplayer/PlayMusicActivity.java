package com.example.custommusicplayer;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.custommusicplayer.Audio.AudioApplication;
import com.example.custommusicplayer.Audio.AudioServiceInterface;
import com.example.custommusicplayer.DAO.BroadCastActions;
import com.example.custommusicplayer.DAO.MusicData;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class PlayMusicActivity extends AppCompatActivity implements View.OnClickListener {

    private ArrayList<MusicData> musicDataArrayList;
    private int position;
    boolean isPlaying = true, isFirstExecuted = true;

    private TextView title, currentDuration, maxDuration;
    private ImageView album, previous, next, play;
    private ProgressBar progressBar;

    private static final Uri artworkUri = Uri.parse("content://media/external/audio/albumart");
    private AudioServiceInterface audioServiceInterface;

    private TimerTask timerTask;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_music);

        // AudioService로부터 특정 신호를 받을 broadcast 등록
        registerBroadcast();

        // 실행할 노래의 순서와 앞뒤로 이동하기 위한 musicList get
        Intent intent = getIntent();
        position = intent.getIntExtra("position", 0);
        musicDataArrayList = (ArrayList<MusicData>) intent.getSerializableExtra("musicList");

        title = findViewById(R.id.title);
        currentDuration = findViewById(R.id.current_duration_tv);
        maxDuration = findViewById(R.id.max_duration_tv);
        album = findViewById(R.id.album);

        previous = findViewById(R.id.pre);
        play = findViewById(R.id.play_or_pause);
        next = findViewById(R.id.next);

        previous.setOnClickListener(this);
        play.setOnClickListener(this);
        next.setOnClickListener(this);

        progressBar = findViewById(R.id.progressbar);

        // AudioService와 상호작용을 위한 인터페이스 객체 선언
        audioServiceInterface = AudioApplication.getInstance().getServiceInterface();
        audioServiceInterface.setPlayList(getAudioIds(musicDataArrayList));
        audioServiceInterface.play(position);
        updateUI();
        startProgressbarTimer();
    }

    // ProgressBar를 실시간으로 업데이트 시키는 타이머 작동(PlayMusicActivity가 살아있는 한 계속 돌면서 재생되는 모든 노래에 대한 Progressbar 업데이트 진행)
    private void startProgressbarTimer() {
        timer = new Timer();

        timerTask = new TimerTask() {
            @Override
            public void run() {
                if(audioServiceInterface.isPlaying()) {
                    int duration = audioServiceInterface.getDuration();
                    int currentPosition = audioServiceInterface.getCurrentDuration();
                    
                    // 현재 재생되는 퍼센티지 계산
                    double percent = (int) ((double) currentPosition / (double) duration * 100.00);
                    int result = (int) Math.round(percent);

                    progressBar.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(result);
                        }
                    });

                    currentDuration.post(new Runnable() {
                        @Override
                        public void run() {
                            currentDuration.setText(String.valueOf(DateFormat.format("mm:ss", currentPosition)));
                        }
                    });
                }
            }
        };

        // 0.5초 단위로 task 실행
        timer.schedule(timerTask, 0, 500);
    }

    private ArrayList<Long> getAudioIds(ArrayList<MusicData> musicDataArrayList) {
        ArrayList<Long> audioIds = new ArrayList<>();
        for (int i = 0; i < musicDataArrayList.size(); i++) {
            audioIds.add(musicDataArrayList.get(i).getId());
        }

        return audioIds;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // MedioPlayer를 재생 -> 일시정지, 일시정지 -> 재생으로 변경
            case R.id.play_or_pause:
                audioServiceInterface.togglePlay();
                break;
            // 이전 노래 재생    
            case R.id.pre:
                audioServiceInterface.rewind();
                isFirstExecuted = true;
                break;
            // 다음 노래 재생    
            case R.id.next:
                audioServiceInterface.forward();
                isFirstExecuted = true;
                break;
        }
    }

    // 새로운 노래가 시작될 때에 화면에 해당 노래의 정보를 출력해주는 함수
    private void updateUI() {
        if (audioServiceInterface.isPlaying()) {
            play.setImageResource(R.drawable.ic_pause_music);
        } else {
            if (isFirstExecuted) {
                play.setImageResource(R.drawable.ic_pause_music);
                isFirstExecuted = false;
            } else {
                play.setImageResource(R.drawable.ic_play_music2);
            }
        }

        MusicData musicData = audioServiceInterface.getAudioItem();
        if (musicData != null) {
            Uri albumPhotoUri = ContentUris.withAppendedId(artworkUri, musicData.getAlbumId());
            Glide
                    .with(getApplicationContext())
                    .load(albumPhotoUri)
                    .placeholder(R.drawable.ic_default_music)
                    .error(R.drawable.ic_default_music)
                    .fallback(R.drawable.ic_default_music)
                    .into(album);
            title.setText(musicData.getTitle());
            maxDuration.setText(String.valueOf(DateFormat.format("mm:ss", musicData.getDuration())));
        } else {
            album.setImageResource(R.drawable.ic_default_music);
            title.setText("재생중인 음악이 없습니다.");
        }
    }

    // AudioService와의 통신을 위한 브로드캐스트 리시버 선언
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI();
        }
    };
    
    // 브로드캐스터에 특정 단어만을 받는 필터 생성
    public void registerBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BroadCastActions.PLAY_STATE_CHANGED);
        registerReceiver(broadcastReceiver, filter);
    }

    // 브로드캐스터 해제
    public void unregisterBroadcast() {
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // 뒤로가기를 누르면 노래도 꺼지고, RemoteViews도 꺼지도록 설정하였습니다.
        if (audioServiceInterface != null) {
            audioServiceInterface.stop();
        }
        if(timer != null) {
            timer.cancel();
            timer = null;
            timerTask.cancel();
            timerTask = null;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isPlaying = false;
        audioServiceInterface = null;
        unregisterBroadcast();
    }
}