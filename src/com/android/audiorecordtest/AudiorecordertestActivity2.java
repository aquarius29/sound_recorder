package com.android.audiorecordtest;


import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import com.android.audiorecordtest.TunerEngine;

	
	

public class AudiorecordertestActivity2 extends Activity
{
	
    private static final double[] FREQUENCIES = { 77.78, 82.41, 87.31, 92.50, 98.00, 103.83, 110.00, 116.54, 123.47, 130.81, 138.59, 146.83, 155.56, 164.81 ,174.61};
    private static final String[] NAME        = {  "D#",  "E",   "F",   "F#"  , "G" ,  "G#",   "A",    "A#",   "B",   "C",     "C#",   "D",   "D#"   ,"E"  ,   "F" };

//    static {
//        System.loadLibrary("FFT");
//}
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tuner);
        boolean start = true;
        toggleTunerState(start);
        start = !start;
    }
    
    TunerEngine t;
    final Handler mHandler = new Handler();
    final Runnable callback = new Runnable() {
        public void run() {
            updateUI(t.currentFrequency);
//            System.out.println("tuner.currentFrequency = " + tuner.currentFrequency);
        }
    };
    public void toggleTunerState(boolean start){
        if(start){
            try {
                t = new TunerEngine(mHandler,callback);
                t.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            t.close();
        }

    }
    
    public void updateUI(double frequency){
    	
        frequency = normaliseFreq(frequency);
        
        int note = closestNote(frequency);
        double matchFreq = FREQUENCIES[note];
        int offset = 0;

        if ( frequency < matchFreq ) {
            double prevFreq = FREQUENCIES[note-1];
            offset = (int)(-(frequency-matchFreq)/(prevFreq-matchFreq)/0.2);
        }
        else {
            double nextFreq = FREQUENCIES[note+1];
            offset = (int)((frequency-matchFreq)/(nextFreq-matchFreq)/0.2);
        }
        TextView tex = (TextView) findViewById(R.id.frequency);
        tex.setText("note: " + NAME[note]);
    }
    
    private static double normaliseFreq(double hz) {
        // get hz into a standard range to make things easier to deal with
        while ( hz < 82.41 ) {
            hz = 2*hz;
        }
        while ( hz > 164.81 ) {
            hz = 0.5*hz;
        }
        return hz;
    }
    
    private static int closestNote(double hz) {
        double minDist = Double.MAX_VALUE;
        int minFreq = -1;
        for ( int i = 0; i < FREQUENCIES.length; i++ ) {
            double dist = Math.abs(FREQUENCIES[i]-hz);
            if ( dist < minDist ) {
                minDist=dist;
                minFreq=i;
            }
        }
//        minFreq = minFreq == 13 ? 1 : minFreq;
        return minFreq;
    }
    
    
}
