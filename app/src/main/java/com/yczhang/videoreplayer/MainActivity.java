package com.yczhang.videoreplayer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, PopupMenu.OnMenuItemClickListener {

    private ArrayList<Interval> intervals;
    private ArrayList<FileItem> videos;
    private VideoView videoView;
    private MediaController controller;
    private ListView intervalList;
    private ListView fileList;
    private IntervalArrayAdapter intervalArrayAdapter;
    private ImageButton startButton;
    private ImageButton stopButton;
    private ImageButton cancelButton;
    private ImageButton playButton;
    private ImageButton pauseButton;
    private ImageButton backButton;
    private ImageButton addButton;
    private Timer autoProgressTimer;
    private ProgressBar playback;
    private TextView currentPosition;
    private Canvas canvas;
    private Paint paint;
    private Bitmap bitmap;
    private ImageView bar;

    private int tempStart;
    private int tempEnd;
    private boolean started;
    private Interval currInterval;

    private String filename;
    private Handler h;

    private int toDeleteInterval = -1;
    private int toDeleteVideo = -1;
    private final int OPEN_FILE_REQUEST_CODE = 579;
    private final String VIDEO_LIST_FILE = "video_list.txt";
    private FileArrayAdapter fileAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = (VideoView)findViewById(R.id.video_view);
        controller = new MediaController(this);
        intervalList = (ListView)findViewById(R.id.interval_list);
        fileList = (ListView)findViewById(R.id.video_list);

        controller.setAnchorView(videoView);
        videoView.setMediaController(controller);

        bar = (ImageView)findViewById(R.id.bar);
        paint = new Paint();

        playback = (ProgressBar)findViewById(R.id.playback);
        currentPosition = (TextView)findViewById(R.id.current_position);
        autoProgressTimer = new Timer();

        class MyHandler extends Handler {
            @Override
            public void handleMessage(Message msg) {
                int max = playback.getMax();
                int pos = videoView.getCurrentPosition();
                int curr = max*pos/videoView.getDuration();
                playback.setProgress(curr);
                currentPosition.setText(Interval.formatTime(pos));

                if(currInterval != null) {
                    if(videoView.isPlaying() && pos > currInterval.getEnd()) {
                        videoView.pause();
                        currInterval = null;
                    }
                }
            }
        }
        h = new MyHandler();
        autoProgressTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                h.sendEmptyMessage(0);
            }
        }, 0, 100);

        this.intervals = new ArrayList<>();
        intervalArrayAdapter = new IntervalArrayAdapter(this,intervals);
        intervalList.setAdapter(intervalArrayAdapter);
        intervalList.setOnItemClickListener(this);
        intervalList.setOnItemLongClickListener(this);

        startButton = (ImageButton)findViewById(R.id.start_button);
        stopButton = (ImageButton)findViewById(R.id.stop_button);
        cancelButton = (ImageButton)findViewById(R.id.cancel_button);
        playButton = (ImageButton)findViewById(R.id.play_button);
        pauseButton = (ImageButton)findViewById(R.id.pause_button);
        backButton = (ImageButton)findViewById(R.id.back_button);
        addButton = (ImageButton)findViewById(R.id.add_button);

        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
        playButton.setOnClickListener(this);
        pauseButton.setOnClickListener(this);
        backButton.setOnClickListener(this);
        addButton.setOnClickListener(this);

        videos = new ArrayList<>();

        fileAdapter = new FileArrayAdapter(this, videos);
        fileList.setAdapter(fileAdapter);
        fileList.setOnItemClickListener(this);
        fileList.setOnItemLongClickListener(this);
        openVideoList();
    }

    private void openVideoList() {
        File path = getFilesDir();
        File file = new File(path,VIDEO_LIST_FILE);

        videos.clear();
        try {
            FileInputStream stream = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while((line = reader.readLine()) != null) {
                try {
                    videos.add(FileItem.fromString(line));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileAdapter.notifyDataSetInvalidated();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.start_button) {
            started = true;
            tempStart = videoView.getCurrentPosition();
            tempEnd = -1;
            currInterval = null;
            if(!videoView.isPlaying())
                videoView.start();
        } else if(v.getId() == R.id.stop_button) {
            started = false;
            tempEnd = videoView.getCurrentPosition();
            Interval newInterval = new Interval(tempStart,tempEnd);
            intervals.add(newInterval);
            intervalArrayAdapter.notifyDataSetInvalidated();
            save();
            tempStart = -1;
            tempEnd = -1;
            currInterval = null;
        } else if(v.getId() == R.id.cancel_button) {
            started = false;
            tempStart = -1;
            tempEnd = -1;
            currInterval = null;
        } else if(v.getId() == R.id.play_button) {
            if(!videoView.isPlaying())
                videoView.start();
        } else if(v.getId() == R.id.pause_button) {
            if(videoView.isPlaying())
                videoView.pause();
        } else if(v.getId() == R.id.back_button) {
            int pos = videoView.getCurrentPosition();
            pos -= 1000;
            if(pos < 0) pos = 0;
            videoView.seekTo(pos);
        } else if(v.getId() == R.id.add_button) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("video/mp4");
            startActivityForResult(intent, OPEN_FILE_REQUEST_CODE);
        }

        draw();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if(requestCode == OPEN_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if(resultData != null) {
                uri = resultData.getData();
                Log.i("Uri",uri.toString());
                videos.add(new FileItem(uri,uri.getPath()));
                fileAdapter.notifyDataSetChanged();
                save();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(parent.getId() == R.id.interval_list) {
            tempStart = -1;
            tempEnd = -1;
            started = false;
            currInterval = intervals.get(position);
            videoView.seekTo((int) currInterval.getStart());
            if (!videoView.isPlaying()) {
                videoView.start();
            }
        } else if(parent.getId() == R.id.video_list) {
            open(videos.get(position).getUri());
            videoView.start();
        }
        draw();
    }

    private void draw() {
        if(bitmap == null) {
            bitmap = Bitmap.createBitmap(bar.getWidth(),bar.getHeight(),Bitmap.Config.ARGB_8888);
            bar.setImageBitmap(bitmap);
            canvas = new Canvas(bitmap);
        }
        canvas.drawColor(ResourcesCompat.getColor(getResources(),R.color.background,null));
        paint.setColor(ResourcesCompat.getColor(getResources(),R.color.foreground,null));
        if(started) {
            int x = bar.getWidth() * tempStart/videoView.getDuration();
            Rect rect = new Rect(x, 0, bar.getWidth(), bar.getHeight());
            canvas.drawRect(rect,paint);
        } else if(currInterval != null) {
            int x1 = bar.getWidth() * (int)currInterval.getStart()/videoView.getDuration();
            int x2 = bar.getWidth() * (int)currInterval.getEnd()/videoView.getDuration();
            Rect rect = new Rect(x1, 0, x2, bar.getHeight());
            canvas.drawRect(rect,paint);
        }
    }

    private void open(Uri uri) {
        videoView.setVideoURI(uri);
        this.filename = Base64.encodeToString(uri.toString().getBytes(),Base64.DEFAULT);

        File path = getFilesDir();
        File file = new File(path,filename+".txt");

        intervals.clear();
        try {
            FileInputStream stream = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while((line = reader.readLine()) != null) {
                try {
                    Interval interval = Interval.fromString(line);
                    intervals.add(interval);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        intervalArrayAdapter.notifyDataSetInvalidated();
    }

    private void save() {
        File path = getFilesDir();
        if(filename != null) {
            File file = new File(path, filename + ".txt");
            try {
                FileOutputStream stream = new FileOutputStream(file);
                for (Interval interval : intervals) {
                    stream.write(interval.toString().getBytes());
                    stream.write("\n".getBytes());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File videoFile = new File(path,VIDEO_LIST_FILE);
        try {
            FileOutputStream stream = new FileOutputStream(videoFile);
            for(FileItem item: videos) {
                stream.write(item.toString().getBytes());
                stream.write("\n".getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if(parent.getId() == R.id.interval_list) {
            PopupMenu popupMenu = new PopupMenu(this, view);
            popupMenu.setOnMenuItemClickListener(this);
            MenuInflater inflater = popupMenu.getMenuInflater();
            inflater.inflate(R.menu.interval_edit_menu, popupMenu.getMenu());
            popupMenu.show();
            toDeleteInterval = position;
            return true;
        } else if(parent.getId() == R.id.video_list) {
            PopupMenu popupMenu = new PopupMenu(this, view);
            popupMenu.setOnMenuItemClickListener(this);
            MenuInflater inflater = popupMenu.getMenuInflater();
            inflater.inflate(R.menu.video_edit_menu, popupMenu.getMenu());
            popupMenu.show();
            toDeleteVideo = position;
            return true;
        }
        return false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if(item.getItemId() == R.id.interval_delete) {
            if(toDeleteInterval >= 0 && toDeleteInterval < intervals.size()) {
                intervals.remove(toDeleteInterval);
                intervalArrayAdapter.notifyDataSetInvalidated();
                toDeleteInterval = -1;
                save();
                return true;
            }
        }
        if(item.getItemId() == R.id.video_delete) {
            if (toDeleteVideo >= 0 && toDeleteVideo < videos.size()) {
                videos.remove(toDeleteVideo);
                fileAdapter.notifyDataSetChanged();
                toDeleteVideo = -1;
                save();
                return true;
            }
        }
        return false;
    }
}
