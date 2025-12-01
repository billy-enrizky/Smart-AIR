package com.example.myapplication;
import android.content.Intent;
import android.net.Uri;
import android.widget.MediaController;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;

public class ChildInhalerVideo extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inhaler_video);

        VideoView videoView = findViewById(R.id.videoView);
        Button backButton = findViewById(R.id.button8);

        String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.inhalervideo;
        Uri uri = Uri.parse(videoPath);
        videoView.setVideoURI(uri);

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        videoView.start();
        videoView.setOnCompletionListener(mp -> videoView.start());

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(ChildInhalerVideo.this, ChildInhalerInstructions.class);
            startActivity(intent);
        });
    }
}