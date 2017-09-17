package com.liberty.waveview;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.liberty.waveview.view.WaveView;

import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    boolean isRecording;
    private static final String TAG = MainActivity.class.getSimpleName();


    String url = "http://dl.stream.qqmusic.qq.com/C4000011HJEz1KxzDp.m4a?vkey=044B2E2FD49DF4A3F23FE" +
            "A246CA3E9A5479322CA08758C2CDC1316947ABFC0901D80E3EE74F748B436DA69B08DF69906D743B23695DB4B3F&guid=1888161504&uin=0&fromtag=66";

    private AudioManager audioManager;
    private Visualizer mVisualizer;
    private MediaPlayer player;
    private AudioRecord record;
    private int bufferSize;

    private WaveView waveView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        player = new MediaPlayer();
//        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
//
//        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//            @Override
//            public void onPrepared(MediaPlayer mp) {
//                mp.start();
//            }
//        });
//
//        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
//            @Override
//            public boolean onError(MediaPlayer mp, int what, int extra) {
//                Logger.e(TAG,"error what = "+what+"  extra = "+extra);
//                return false;
//            }
//        });
//        try {
//            player.setDataSource(url);
//            player.prepareAsync();
//            mVisualizer = new Visualizer(player.getAudioSessionId());
//            mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
//            mVisualizer.setDataCaptureListener(
//                    new Visualizer.OnDataCaptureListener() {
//                        @Override
//                        public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
//                            Logger.d(TAG,"sampling rate : "+samplingRate);
//                            StringBuilder sb = new StringBuilder();
//                            for (int i = 0; i < waveform.length; i++) {
//                                sb.append(waveform[i]);
//                                sb.append(" ");
//                            }
//                            Logger.d(TAG,"waveForm : "+sb.toString());
//                        }
//
//                        @Override
//                        public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
//                            Logger.d(TAG,"sampling rate : "+samplingRate);
//                            StringBuilder sb = new StringBuilder();
//                            for (int i = 0; i < fft.length; i++) {
//                                sb.append(fft[i]);
//                                sb.append(" ");
//                            }
//                            Logger.d(TAG,"fft : "+sb.toString());
//                        }
//                    },Visualizer.getMaxCaptureRate()/2,true,true);
//            mVisualizer.setEnabled(true);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
//        new Thread(){
//            @Override
//            public void run() {
//                super.run();
//                try {
//                    while (true){
//                        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//                        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//                        Logger.d(TAG,"volume = "+volume+"  maxVolume = "+maxVolume);
//                        Thread.sleep(10);
//                    }
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }.start();

        recordThread = new RecordThread();

        waveView = (WaveView) findViewById(R.id.waveView);

        findViewById(R.id.record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRun.get()){
                    recordThread.start();
                    isRun.set(true);
                }else {
//                    recordThread.interrupt();
                    Logger.d(TAG,"isInterrupt : "+recordThread.isInterrupted());
                    isRun.set(false);
                }
            }
        });
    }

    private RecordThread recordThread;

    private void init(){
        bufferSize = AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
        record = new AudioRecord(MediaRecorder.AudioSource.MIC,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,bufferSize);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (player!=null){
//            mVisualizer.release();
//            player.release();
//            player=null;
//        }
    }

    private AtomicBoolean isRun = new AtomicBoolean(false);

    private class RecordThread extends Thread{
        @Override
        public void run() {
            super.run();
            if (record==null){
                init();
            }
            record.startRecording();
            short[] buff = new short[bufferSize];
            while (isRun.get()){
//                Logger.d(TAG,"Thread.interrupted()v1 : "+Thread.interrupted());
                Logger.d(TAG,"isInterrupt v1: "+isInterrupted());
                int r = record.read(buff,0,bufferSize);
                long v = 0;
                for (int i = 0; i < buff.length; i++) {
                    v+= buff[i]*buff[i];
                }
                double mean = v/(double)r;
                double volume = 10*Math.log10(mean);
                Logger.d(TAG,"dB = "+volume);
                waveView.setAmplitude((float) volume,true);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Logger.e(TAG,"I am interrupted!");
                    Logger.d(TAG,"interrupt : "+isInterrupted());
                    e.printStackTrace();
                }
            }
//            Logger.d(TAG,"Thread.interrupted()v2 : "+Thread.interrupted());
//            try {
//                sleep(100);
//            } catch (InterruptedException e) {
//                Logger.e(TAG,e.toString());
//                e.printStackTrace();
//            }
//            Logger.d(TAG,"wake up");
//            Logger.d(TAG,"isInterrupt v2: "+isInterrupted());
//            record.stop();
//            record.release();
//            record = null;
        }
    }
}
