package com.example.custommusicplayer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.custommusicplayer.Adapter.MusicAdapter;
import com.example.custommusicplayer.DAO.MusicData;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private static final int LOADER_ID = 1001;
    private static final int P_CODE = 1002;
    private static final String TAG = "############";
    private boolean isFirstExecuted = true;

    private ArrayList<MusicData> musicDataArrayList = new ArrayList<>();
    private ListView musicListView;
    private MusicAdapter musicAdapter;

    private Disposable disposable;
    private MediaScannerConnection mScannerConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 리스트에 음악 리스트를 연결해줄 어뎁터와 리스트뷰 선언
        musicAdapter = new MusicAdapter(getApplicationContext(), musicDataArrayList);
        musicListView = findViewById(R.id.mainactivity_listview);
        musicListView.setAdapter(musicAdapter);
        musicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), PlayMusicActivity.class);
                intent.putExtra("position", position);
                intent.putExtra("musicList", musicDataArrayList);
                startActivity(intent);
            }
        });

//        mScannerConnection = new MediaScannerConnection(this, mScanClient);

        // OS가 Marshmallow 이상일 경우 권한체크를 해야 합니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, P_CODE);
            } else {
                // READ_EXTERNAL_STORAGE 에 대한 권한이 있음.
                refreshMusicList();
            }
        }
        // OS가 Marshmallow 이전일 경우 권한체크를 하지 않는다.
        else {
            refreshMusicList();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isFirstExecuted) {
            isFirstExecuted = false;
        } else {
            refreshLoader();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == P_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // READ_EXTERNAL_STORAGE 에 대한 권한 획득.
                refreshMusicList();
            }
        }
    }

//    private MediaScannerConnection.MediaScannerConnectionClient mScanClient = new MediaScannerConnection.MediaScannerConnectionClient() {
//        @Override
//        public void onMediaScannerConnected() {
//            File file = Environment.getExternalStorageDirectory();
//
//            File[] fileNames = file.listFiles(new FilenameFilter(){               // 특정 확장자만 가진 파일들을 필터링함
//                public boolean accept(File dir, String name){
//
//                    return name.endsWith(".mp3");
//                }
//            });
//
//            if (fileNames != null) {
//                for (int i = 0; i < fileNames.length ; i++)          //  파일 갯수 만큼   scanFile을 호출함
//                {
//                    mScannerConnection.scanFile(fileNames[i].getAbsolutePath(), null);
//                }
//            }
//        }
//
//        @Override
//        public void onScanCompleted(String path, Uri uri) {           // 정확하게 모든 파일들에 대한 스캔이 끝난 타이밍에 대한 추가 소스가 필요
//            refreshLoader();
//        }
//    };

    // MedisStore에 새로 등록된 파일들에 대한 정보를 받아오는 함수
    private void refreshLoader() {
        musicDataArrayList.clear();

        LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
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
                String selection = MediaStore.Audio.Media.IS_MUSIC + " = 1";
                String sortOrder = MediaStore.Audio.Media.TITLE + " COLLATE LOCALIZED ASC";
                return new CursorLoader(getApplicationContext(), uri, projection, selection, null, sortOrder);
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                if (data != null && data.getCount() > 0) {
                    while (data.moveToNext()) {
                        musicDataArrayList.add(new MusicData(
                                data.getString(data.getColumnIndex(MediaStore.Audio.Media._ID)),
                                data.getString(data.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                                data.getString(data.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                                data.getString(data.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                                data.getString(data.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                                data.getString(data.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                                data.getString(data.getColumnIndex(MediaStore.Audio.Media.DATA))
                        ));
                    }
                }

                musicAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                musicDataArrayList.clear();
            }
        });
    }

    // rxJava를 활용하여 새로 들어온 파일들에 대한 MediaScan 실행
    private void refreshMusicList() {
        disposable = Observable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                MediaScannerConnection
                        .scanFile(
                                getApplicationContext(),
                                new String[]{
                                        getRealPathFromURI(getApplicationContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
                                },
                                null,
                                new MediaScannerConnection.OnScanCompletedListener() {
                                    public void onScanCompleted(String path, Uri uri) {
                                        Log.i(TAG, "스캔이 완료");
                                    }
                                });

                return false;
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object result) throws Exception {
                        disposable.dispose();

                        getAudioListFromMediaDatabase();
                    }
                });
    }

    // MediaStore로부터 오디오 파일들에 대한 정보 획득
    private void getAudioListFromMediaDatabase() {
        musicDataArrayList.clear();

        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
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
                String selection = MediaStore.Audio.Media.IS_MUSIC + " = 1";
                String sortOrder = MediaStore.Audio.Media.TITLE + " COLLATE LOCALIZED ASC";
                return new CursorLoader(getApplicationContext(), uri, projection, selection, null, sortOrder);
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                if (data != null && data.getCount() > 0) {
                    while (data.moveToNext()) {
                        musicDataArrayList.add(new MusicData(
                                data.getString(data.getColumnIndex(MediaStore.Audio.Media._ID)),
                                data.getString(data.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                                data.getString(data.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                                data.getString(data.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                                data.getString(data.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                                data.getString(data.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                                data.getString(data.getColumnIndex(MediaStore.Audio.Media.DATA))
                        ));
                    }
                }

                musicAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                musicDataArrayList.clear();
            }
        });
    }

    // 각각의 파일들에 대한 직접경로를 획득(MediaScan 할때 필요)
    public String getRealPathFromURI(final Context context, final Uri uri) {

        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/"
                            + split[1];
                } else {
                    String SDcardpath = getRemovableSDCardPath(context).split("/Android")[0];
                    return SDcardpath +"/"+ split[1];
                }
            }

            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }

            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };

                return getDataColumn(context, contentUri, selection,
                        selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }


    public static String getRemovableSDCardPath(Context context) {
        File[] storages = ContextCompat.getExternalFilesDirs(context, null);
        if (storages.length > 1 && storages[0] != null && storages[1] != null)
            return storages[1].toString();
        else
            return "";
    }


    public static String getDataColumn(Context context, Uri uri,
                                       String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };

        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                .getAuthority());
    }


    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                .getAuthority());
    }


    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                .getAuthority());
    }


    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri
                .getAuthority());
    }
}