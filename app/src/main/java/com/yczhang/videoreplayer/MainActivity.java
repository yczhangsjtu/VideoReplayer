package com.yczhang.videoreplayer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.MediaController;
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
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private ArrayList<Interval> intervals;
    private ArrayList<String> videos;
    private VideoView videoView;
    private MediaController controller;
    private ListView intervalList;
    private ListView fileList;
    private IntervalArrayAdapter intervalArrayAdapter;
    private ImageButton startButton;
    private ImageButton stopButton;
    private ImageButton cancelButton;
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
        }, 500, 500);

        this.intervals = new ArrayList<>();
        intervalArrayAdapter = new IntervalArrayAdapter(this,intervals);
        intervalList.setAdapter(intervalArrayAdapter);
        intervalList.setOnItemClickListener(this);

        startButton = (ImageButton)findViewById(R.id.start_button);
        stopButton = (ImageButton)findViewById(R.id.stop_button);
        cancelButton = (ImageButton)findViewById(R.id.cancel_button);

        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        File path = getFilesDir();
        String[] files = path.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mp4");
            }
        });
        videos = new ArrayList<>();
        for(String s: files) {
            videos.add(s.substring(0,s.length()-4));
        }

        FileArrayAdapter fileAdapter = new FileArrayAdapter(this, videos);
        fileList.setAdapter(fileAdapter);
        fileList.setOnItemClickListener(this);
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
        }

        draw();
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
            open(videos.get(position));
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

    private void open(String filename) {
        videoView.setVideoPath(getFilesDir().getAbsolutePath()+"/"+filename+".mp4");
        this.filename = filename;

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

        videoView.start();
    }

    private void save() {
        File path = getFilesDir();
        File file = new File(path,filename+".txt");
        try {
            FileOutputStream stream = new FileOutputStream(file);
            for(Interval interval: intervals) {
                stream.write(interval.toString().getBytes());
                stream.write("\n".getBytes());
                Log.d("Save",interval.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
