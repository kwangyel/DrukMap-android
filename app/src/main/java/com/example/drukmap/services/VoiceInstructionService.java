package com.example.drukmap.services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.drukmap.R;

public class VoiceInstructionService extends Service implements MediaPlayer.OnCompletionListener {

    private final IBinder localBinder = new VoiceInstructionBinder();
    MediaPlayer arrival,turnLeft,turnRight,straight;

    public VoiceInstructionService(){}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        straight= MediaPlayer.create(this, R.raw.straight);
        turnLeft = MediaPlayer.create(this,R.raw.left);
        turnRight = MediaPlayer.create(this,R.raw.right);
        arrival = MediaPlayer.create(this,R.raw.arrival);

        straight.setOnCompletionListener(this);
        turnLeft.setOnCompletionListener(this);
        turnRight.setOnCompletionListener(this);
        arrival.setOnCompletionListener(this);
    }

    public void straight(){
        straight.start();
    }
    public void turnLeft(){
        turnLeft.start();
    }
    public void turnRight(){
        turnRight.start();
    }
    public void arrival(){
        arrival.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
       return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        straight.release();
        turnRight.release();
        turnLeft.release();
        arrival.release();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.stop();
    }


    public class VoiceInstructionBinder extends Binder {
        public VoiceInstructionService getService(){
            return VoiceInstructionService.this;
        }
    }
}
