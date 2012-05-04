package com.android.audiorecordtest;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
	
	

public class AudiorecordertestActivity extends Activity
{

    
    private static final String LOG_TAG = "AudioRecordTest";
    private static String mFileName = null;

    private RecordButton mRecordButton = null;
    private MediaRecorder mRecorder = null;

    private PlayButton   mPlayButton = null;
    private MediaPlayer   mPlayer = null;
    
    private TunerButton   tunerButton = null;
    
    private Spinner spinnerF;

    
    // audioRecord variables:::
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int[] OPT_SAMPLE_RATES = {8000,11025,22050,44100, 48000};
    private static final int[] BUFFERSIZE_PER_SAMPLE_RATE = {4*1024,8*1024,16*1024,32*1024,32*1024};

    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    
    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private int RECORDER_SAMPLERATE = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private boolean debug = true;
    
    List<Integer> PossibleRates = new ArrayList<Integer>();

    //end of the vars<<<    
    
   //audio recorder code>>>
    private String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);
        
        if(!file.exists()){
                file.mkdirs();
        }
        
        //name the file after the current date and time: 
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss", Locale.US);
        
        String fileName = sdf.format(new Date()) + "_sr_" + RECORDER_SAMPLERATE; //(DateFormat.getTimeInstance(DateFormat.LONG).format(new Date())).replace(':', '_');
        
        if(debug) Toast.makeText(getApplicationContext(),fileName + AUDIO_RECORDER_FILE_EXT_WAV, Toast.LENGTH_SHORT).show();
        
        return (file.getAbsolutePath() + "/" + fileName  + AUDIO_RECORDER_FILE_EXT_WAV);
        
}

  private void initAudioRecord(){
  int counter = 0;
  for(int sampleRate : OPT_SAMPLE_RATES){ 
      initAudioRecord(sampleRate);
      if(recorder.getState() == AudioRecord.STATE_INITIALIZED ){
    	  RECORDER_SAMPLERATE = sampleRate;
      	  bufferSize = BUFFERSIZE_PER_SAMPLE_RATE[counter];
      	  if(debug) Toast.makeText(getApplicationContext(),"SampleRate possible: " + sampleRate + "\nbuffersize: " + bufferSize, Toast.LENGTH_SHORT).show();
      }
      counter++;
  }
}
  
private void initAudioRecord(int sampleRate){
	recorder =  new AudioRecord(
              MediaRecorder.AudioSource.MIC,
              sampleRate,
              android.media.AudioFormat.CHANNEL_CONFIGURATION_MONO,
              android.media.AudioFormat.ENCODING_PCM_16BIT ,
              sampleRate*6
      );
      if(recorder.getState() == AudioRecord.STATE_INITIALIZED ){
    	RECORDER_SAMPLERATE = sampleRate;
    	
    	if(!PossibleRates.contains(sampleRate)) PossibleRates.add(sampleRate);
    	
    	int counter = 0;
    	  for(int sr: OPT_SAMPLE_RATES){    		  
    		  if(sr==sampleRate) bufferSize = BUFFERSIZE_PER_SAMPLE_RATE[counter];
    		  counter++;
    	  }
    }
  }
    
    
private String getTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);
        
        if(!file.exists()){
                file.mkdirs();
        }
        
        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);
        
        if(tempFile.exists())
                tempFile.delete();
        
        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
}


private void startRecording(){
	initAudioRecord(Integer.parseInt(spinnerF.getSelectedItem().toString()));

	if(debug) Toast.makeText(getApplicationContext(),"initializing Rec_sample: " + Integer.parseInt(spinnerF.getSelectedItem().toString()), Toast.LENGTH_LONG).show();
	
    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                    RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);
    
    if(debug) Toast.makeText(getApplicationContext(),"recording at SR:"+RECORDER_SAMPLERATE + " \nbuff size:" + bufferSize , Toast.LENGTH_LONG).show();
    
    recorder.startRecording();
    
    isRecording = true;
    
    recordingThread = new Thread(new Runnable() {
            
            public void run() {
                    writeAudioDataToFile();
            }
    },"AudioRecorder Thread");
    
    recordingThread.start();
}

private void writeAudioDataToFile(){
    byte data[] = new byte[bufferSize];
    String filename = getTempFilename();
    FileOutputStream os = null;
    
    try {
            os = new FileOutputStream(filename);
    } catch (FileNotFoundException e) {
            e.printStackTrace();
    }
    
    int read = 0;
    
    if(null != os){
            while(isRecording){
                    read = recorder.read(data, 0, bufferSize);
                    
                    if(AudioRecord.ERROR_INVALID_OPERATION != read){
                            try {
                                    os.write(data);
                            } catch (IOException e) {
                                    e.printStackTrace();
                            }
                    }
            }
            
            try {
                    os.close();
            } catch (IOException e) {
                    e.printStackTrace();
            }
    }
}

private void stopRecording(){
    if(null != recorder){
            isRecording = false;
            
            recorder.stop();
            recorder.release();
            
            recorder = null;
            recordingThread = null;
    }
    
    copyWaveFile(getTempFilename(),getFilename());
    deleteTempFile();
}

private void deleteTempFile() {
    File file = new File(getTempFilename());
    
    file.delete();
}

private void copyWaveFile(String inFilename,String outFilename){
    FileInputStream in = null;
    FileOutputStream out = null;
    long totalAudioLen = 0;
    long totalDataLen = totalAudioLen + 36;
    long longSampleRate = RECORDER_SAMPLERATE;
    int channels = 2;
    long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;
    
    byte[] data = new byte[bufferSize];
    
    try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
            
            while(in.read(data) != -1){
                    out.write(data);
            }
            
            in.close();
            out.close();
    } catch (FileNotFoundException e) {
            e.printStackTrace();
    } catch (IOException e) {
            e.printStackTrace();
    }
}

private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {
    
    byte[] header = new byte[44];
    
    header[0] = 'R';  // RIFF/WAVE header
    header[1] = 'I';
    header[2] = 'F';
    header[3] = 'F';
    header[4] = (byte) (totalDataLen & 0xff);
    header[5] = (byte) ((totalDataLen >> 8) & 0xff);
    header[6] = (byte) ((totalDataLen >> 16) & 0xff);
    header[7] = (byte) ((totalDataLen >> 24) & 0xff);
    header[8] = 'W';
    header[9] = 'A';
    header[10] = 'V';
    header[11] = 'E';
    header[12] = 'f';  // 'fmt ' chunk
    header[13] = 'm';
    header[14] = 't';
    header[15] = ' ';
    header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
    header[17] = 0;
    header[18] = 0;
    header[19] = 0;
    header[20] = 1;  // format = 1
    header[21] = 0;
    header[22] = (byte) channels;
    header[23] = 0;
    header[24] = (byte) (longSampleRate & 0xff);
    header[25] = (byte) ((longSampleRate >> 8) & 0xff);
    header[26] = (byte) ((longSampleRate >> 16) & 0xff);
    header[27] = (byte) ((longSampleRate >> 24) & 0xff);
    header[28] = (byte) (byteRate & 0xff);
    header[29] = (byte) ((byteRate >> 8) & 0xff);
    header[30] = (byte) ((byteRate >> 16) & 0xff);
    header[31] = (byte) ((byteRate >> 24) & 0xff);
    header[32] = (byte) (2 * 16 / 8);  // block align
    header[33] = 0;
    header[34] = RECORDER_BPP;  // bits per sample
    header[35] = 0;
    header[36] = 'd';
    header[37] = 'a';
    header[38] = 't';
    header[39] = 'a';
    header[40] = (byte) (totalAudioLen & 0xff);
    header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
    header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
    header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

    out.write(header, 0, 44);
}
   //end audio recorder code<<<

    
    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

//    private void startRecording() {
//        mRecorder = new MediaRecorder();
//        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        mRecorder.setOutputFile(mFileName);
//        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//    	
//    	
//
//        try {
//            mRecorder.prepare();
//        } catch (IOException e) {
//            Log.e(LOG_TAG, "prepare() failed");
//        }
//
//        mRecorder.start();
//    }

//    private void stopRecording() {
//        mRecorder.stop();
//        mRecorder.release();
//        mRecorder = null;
//    }

    class RecordButton extends Button {
        boolean mStartRecording = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                onRecord(mStartRecording);
                if (mStartRecording) {
                    setText("Stop recording");
                } else {
                    setText("Start recording");
                }
                mStartRecording = !mStartRecording;
            }
        };

        public RecordButton(Context ctx) {
            super(ctx);
            setText("Start recording");
            setOnClickListener(clicker);
        }
    }

    class PlayButton extends Button {
        boolean mStartPlaying = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                onPlay(mStartPlaying);
                if (mStartPlaying) {
                    setText("Stop playing");
                } else {
                    setText("Start playing");
                }
                mStartPlaying = !mStartPlaying;
            }
        };

        public PlayButton(Context ctx) {
            super(ctx);
            setText("Start playing");
            setOnClickListener(clicker);
        }
    }
    
    class TunerButton extends Button{
    	
        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
            	Intent myIntent = new Intent(v.getContext(), AudiorecordertestActivity2.class);
                startActivityForResult(myIntent, 0);
            }
        };
        
        public TunerButton(Context ctx) {
            super(ctx);
            setText("Tuner");
            setOnClickListener(clicker);
        }
    }

    public AudiorecordertestActivity() {
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/audiorec/audiorecordtest.3gp";
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);
        
        spinnerF = (Spinner) new Spinner(this); //findViewById(R.id.spinnerF);        

        initAudioRecord();
        
        recorder.release();
        recorder = null;
        
        ArrayAdapter<Integer> dataAdapter = new ArrayAdapter<Integer>(this,
        		android.R.layout.simple_spinner_item, PossibleRates);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerF.setAdapter(dataAdapter);

        
//        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING);

        
        LinearLayout ll = new LinearLayout(this);
        mRecordButton = new RecordButton(this);
        tunerButton = new TunerButton(this);

        ll.addView(spinnerF,
        		new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        
//        ll.addView(tunerButton, 
//                new LinearLayout.LayoutParams(
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        0));

        ll.addView(mRecordButton,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0));
//        mPlayButton = new PlayButton(this);
//        ll.addView(mPlayButton,
//            new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT,
//                0));
        setContentView(ll);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }
}
