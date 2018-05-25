package com.yczhang.videoreplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ArrayList<Interval> intervals;
    private VideoView videoView;
    private MediaController controller;
    private ListView intervalList;
    private IntervalArrayAdapter adapter;
    private ImageButton startButton;
    private ImageButton stopButton;
    private ImageButton cancelButton;
    private Timer autoProgressTimer;
    private ProgressBar playback;

    private int tempStart;
    private int tempEnd;
    private boolean started;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = (VideoView)findViewById(R.id.video_view);
        controller = new MediaController(this);
        intervalList = (ListView)findViewById(R.id.interval_list);

        videoView.setVideoPath(getFilesDir().getAbsolutePath()+"/test.mp4");
        controller.setAnchorView(videoView);
        videoView.setMediaController(controller);
        videoView.getCurrentPosition();

        playback = (ProgressBar)findViewById(R.id.playback);
        autoProgressTimer = new Timer();
        autoProgressTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int max = playback.getMax();
                int curr = max*videoView.getCurrentPosition()/videoView.getDuration();
                playback.setProgress(curr);
            }
        }, 0, 500);

        this.intervals = new ArrayList<>();
        adapter = new IntervalArrayAdapter(this,intervals);
        intervalList.setAdapter(adapter);

        startButton = (ImageButton)findViewById(R.id.start_button);
        stopButton = (ImageButton)findViewById(R.id.stop_button);
        cancelButton = (ImageButton)findViewById(R.id.cancel_button);

        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        videoView.start();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.start_button) {
            started = true;
            tempStart = videoView.getCurrentPosition();
            tempEnd = -1;
            if(!videoView.isPlaying())
                videoView.start();
        } else if(v.getId() == R.id.stop_button) {
            started = false;
            tempEnd = videoView.getCurrentPosition();
            Interval newInterval = new Interval(tempStart,tempEnd);
            intervals.add(newInterval);
            adapter.notifyDataSetInvalidated();
        } else if(v.getId() == R.id.cancel_button) {
            Log.d("Button","Cancel button clicked!");
        }
    }
}
