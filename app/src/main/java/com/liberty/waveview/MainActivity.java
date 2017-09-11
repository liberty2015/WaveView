package com.liberty.waveview;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import static android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED;

public class MainActivity extends AppCompatActivity {

    AudioRecord audioRecord;

    MediaRecorder recorder;

    boolean isRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int buff = AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
//        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
//                16000, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,buff);
        recorder = new MediaRecorder();
        recorder.setAudioEncodingBitRate(AudioFormat.ENCODING_PCM_16BIT);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        recorder.setAudioEncoder(AudioFormat.CHANNEL_IN_MONO);
        recorder.setMaxDuration(MEDIA_RECORDER_INFO_MAX_DURATION_REACHED);
        findViewById(R.id.record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording){
//                    audioRecord.startRecording();
                    recorder.start();
                    recorder.getMaxAmplitude();
                }else {
                    recorder.stop();
                }
            }
        });
    }
}
