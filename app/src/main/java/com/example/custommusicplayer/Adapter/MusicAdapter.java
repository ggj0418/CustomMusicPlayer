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

import com.bumptech.glide.Glide;
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

    public MusicAdapter(Context mContext, ArrayList<MusicData> musicDataArrayList) {
        this.mContext = mContext;
        this.musicDataArrayList = musicDataArrayList;
        layoutInflater = (LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
//            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//            convertView.setLayoutParams(layoutParams);
        }

        ImageView musicImageView = convertView.findViewById(R.id.img_albumart);
        TextView titleTextView = convertView.findViewById(R.id.txt_title);
        TextView subTitleTextView = convertView.findViewById(R.id.txt_sub_title);
        TextView durationTextView = convertView.findViewById(R.id.txt_duration);

        Uri albumPhotoUri = ContentUris.withAppendedId(artworkUri, musicDataArrayList.get(position).getAlbumId());
        Glide
                .with(mContext)
                .load(albumPhotoUri)
                .placeholder(R.drawable.ic_default_music)
                .error(R.drawable.ic_default_music)
                .fallback(R.drawable.ic_default_music)
                .into(musicImageView);
        titleTextView.setText(musicDataArrayList.get(position).getTitle());
        subTitleTextView.setText(musicDataArrayList.get(position).getArtist() + " ( " + musicDataArrayList.get(position).getAlbum() + " )");
        durationTextView.setText(DateFormat.format("mm:ss", musicDataArrayList.get(position).getDuration()));

        return convertView;
    }
}
