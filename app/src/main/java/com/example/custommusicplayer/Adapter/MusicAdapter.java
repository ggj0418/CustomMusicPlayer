package com.example.custommusicplayer.Adapter;

import android.content.ContentUris;
import android.content.Context;

import android.net.Uri;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.custommusicplayer.DAO.MusicData;
import com.example.custommusicplayer.R;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import java.util.ArrayList;

public class MusicAdapter extends BaseAdapter {

    private final Uri artworkUri = Uri.parse("content://media/external/audio/albumart");

    Context mContext;
    LayoutInflater layoutInflater;
    ArrayList<MusicData> musicDataArrayList = new ArrayList<>();

    private ImageLoader imageLoader;

    public MusicAdapter(Context mContext, ArrayList<MusicData> musicDataArrayList) {
        this.mContext = mContext;
        this.musicDataArrayList = musicDataArrayList;
        layoutInflater = (LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // 리스트뷰 스크롤 시, 항상 새로 이미지를 불러오기 때문에 버벅거리는 현상을 해소하기 위한 이미지 캐싱 코드
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(mContext)
                .threadPriority(10)
                .denyCacheImageMultipleSizesInMemory()
                .diskCacheFileNameGenerator(new Md5FileNameGenerator())
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .writeDebugLogs()
                .build();

        imageLoader = ImageLoader.getInstance();
        imageLoader.init(config);
    }

    @Override
    public int getCount() {
        return musicDataArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return musicDataArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return musicDataArrayList.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.items_music, parent, false);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            convertView.setLayoutParams(layoutParams);
        }

        ImageView musicImageView = convertView.findViewById(R.id.img_albumart);
        TextView titleTextView = convertView.findViewById(R.id.txt_title);
        TextView subTitleTextView = convertView.findViewById(R.id.txt_sub_title);
        TextView durationTextView = convertView.findViewById(R.id.txt_duration);

        Uri albumPhotoUri = ContentUris.withAppendedId(artworkUri, musicDataArrayList.get(position).getAlbumId());
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showImageForEmptyUri(R.drawable.ic_default_music) // Uri주소가 잘못되었을경우(이미지없을때)
                .resetViewBeforeLoading(false)  // 로딩전에 뷰를 리셋하는건데 false로 하세요 과부하!
                .cacheInMemory(true) // 메모리케시 사용여부
                .cacheOnDisk(true)
                .build();

        imageLoader.displayImage(albumPhotoUri.toString(), musicImageView, options);
        titleTextView.setText(musicDataArrayList.get(position).getTitle());
        subTitleTextView.setText(musicDataArrayList.get(position).getArtist() + " ( " + musicDataArrayList.get(position).getAlbum() + " )");
        durationTextView.setText(DateFormat.format("mm:ss", musicDataArrayList.get(position).getDuration()));

        return convertView;
    }
}
