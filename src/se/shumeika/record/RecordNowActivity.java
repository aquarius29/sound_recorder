 package se.shumeika.record;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

public class RecordNowActivity extends Activity {

	private static final String LOG_TAG = "AudioRecordTest";
	private static String mFileName = null;

	private MediaRecorder mRecorder = null;
	private MediaPlayer mPlayer = null;

	private static final int RECORDER_BPP = 16;
	private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
	private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
	private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
	private static final int[] OPT_SAMPLE_RATES = {44100};
	private static final int[] BUFFERSIZE_PER_SAMPLE_RATE = {32 * 1024};

	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	private AudioRecord recorder = null;
	private int bufferSize = 32 * 1024;
	private int RECORDER_SAMPLERATE = 44100;
	private String point = ""; 
	private Thread recordingThread = null;
	private boolean isRecording = false;
	private boolean debug = false;
    
	private RadioGroup mRadioGroup;
	private String machine = "";
	private int checkedRadioButton = 0;
	private String fileName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
//		initAudioRecord();
		
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final ImageButton button = (ImageButton) findViewById(R.id.button);
		Spinner spinner = (Spinner) findViewById(R.id.spinner);
		mRadioGroup = (RadioGroup) findViewById(R.id.machineState);

		 
		
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (button.isSelected()) { // Stop Recording
					button.setSelected(false);
					// ...Handle toggle off
			        
					if(debug) Toast.makeText(getApplicationContext(),"Saved Recording", Toast.LENGTH_SHORT).show();
					
					onRecord(false);
					//TODO Display Save/Discard file dialog
				} else { // Start Recording
					button.setSelected(true);
					// ...Handled toggle on
					if(debug) Toast.makeText(getApplicationContext(),"Started Recording", Toast.LENGTH_SHORT).show();
					onRecord(true);
				}
			}
		});
		
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			        point = parent.getItemAtPosition(pos).toString();
					if (debug) Toast.makeText(getApplicationContext(),"Spinner pos : "  + point, Toast.LENGTH_SHORT).show();

			    }
			    public void onNothingSelected(AdapterView<?> parent) {
			    }
			});
		
		
	}

	
	//INITIALIZE RECORDING ============================================
	  private void initAudioRecord(){
		  int counter = 0;
		  for(int sampleRate : OPT_SAMPLE_RATES){ 
	    	  if(debug) Toast.makeText(getApplicationContext(),"sample R " + sampleRate , Toast.LENGTH_SHORT).show();

		      initAudioRecord(sampleRate);
		      if(recorder.getState() == AudioRecord.STATE_INITIALIZED ){
		    	  RECORDER_SAMPLERATE = sampleRate;
		      	  bufferSize = BUFFERSIZE_PER_SAMPLE_RATE[counter];
		      	  if(debug) Toast.makeText(getApplicationContext(),"SampleRate possible: " + sampleRate + "\nbuffersize: " + bufferSize, Toast.LENGTH_SHORT).show();
		      }else{
		    	  if(debug) Toast.makeText(getApplicationContext(),"NOT INIT", Toast.LENGTH_SHORT).show();
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
		    }else{
		    	  if(debug) Toast.makeText(getApplicationContext(),"SOMTHIGN wrong "+ recorder.getState(), Toast.LENGTH_SHORT).show();	    			    	
		    }		      
		  }

	//RECORDING =======================================================
	private void startRecording() {
		 initAudioRecord(RECORDER_SAMPLERATE);

			recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
					RECORDER_SAMPLERATE, RECORDER_CHANNELS,
					RECORDER_AUDIO_ENCODING, bufferSize);

		if (debug) Toast.makeText(getApplicationContext(),"recording at SR:" + RECORDER_SAMPLERATE + " \nbuff size:" + bufferSize, Toast.LENGTH_LONG).show();

		recorder.startRecording();
		isRecording = true;
		recordingThread = new Thread(new Runnable() {

			public void run() {
				writeAudioDataToFile();
			}
		}, "AudioRecorder Thread");

		recordingThread.start();
	}
	
	private void stopRecording() {
		if (null != recorder) {
			isRecording = false;

			recorder.stop();
			recorder.release();

			recorder = null;
			recordingThread = null;
			
			checkedRadioButton = mRadioGroup.getCheckedRadioButtonId();
			switch (checkedRadioButton) {
			  case R.id.radio0 : machine = "working ";
			                   	              break;
			  case R.id.radio1 : machine = "faulty ";
					                      break;
			}
		}

		copyWaveFile(getTempFilename(), getFilename());
		deleteTempFile();
	}
	
	//PLAY RECORDING ===================================================
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

	// FILE HANDLING ===================================================
	private String getFilename() {
		String filepath = Environment.getExternalStorageDirectory().getPath();
		File file = new File(filepath, AUDIO_RECORDER_FOLDER);

		if (!file.exists()) {
			file.mkdirs();
		}

		// name the file after the current date and time:
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss",
				Locale.US);
		
		fileName = machine + point + " " + sdf.format(new Date()) ; // (DateFormat.getTimeInstance(DateFormat.LONG).format(new
																					// Date())).replace(':',
																					// '_');
		if (debug)
			Toast.makeText(getApplicationContext(),
					fileName + AUDIO_RECORDER_FILE_EXT_WAV, Toast.LENGTH_SHORT)
					.show();

		return (file.getAbsolutePath() + "/" + fileName + AUDIO_RECORDER_FILE_EXT_WAV);

	}
	
	private String getTempFilename() {
		String filepath = Environment.getExternalStorageDirectory().getPath();
		File file = new File(filepath, AUDIO_RECORDER_FOLDER);

		if (!file.exists()) {
			file.mkdirs();
		}

		File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);

		if (tempFile.exists())
			tempFile.delete();

		return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
	}
	
	private void deleteTempFile() {
		File file = new File(getTempFilename());

		file.delete();
	}
	
	private void writeAudioDataToFile() {
		byte data[] = new byte[bufferSize];
		String filename = getTempFilename();
		FileOutputStream os = null;

		try {
			os = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		int read = 0;

		if (null != os) {
			while (isRecording) {
				read = recorder.read(data, 0, bufferSize);

				if (AudioRecord.ERROR_INVALID_OPERATION != read) {
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

	private void copyWaveFile(String inFilename, String outFilename) {
		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = RECORDER_SAMPLERATE;
		int channels = 2;
		long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;

		byte[] data = new byte[bufferSize];

		try {
			in = new FileInputStream(inFilename);
			out = new FileOutputStream(outFilename);
			totalAudioLen = in.getChannel().size();
			totalDataLen = totalAudioLen + 36;

			WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
					longSampleRate, channels, byteRate);

			while (in.read(data) != -1) {
				out.write(data);
			}

			in.close();
			out.close();
			
		Toast.makeText(getApplicationContext(),"File is saved \nfor " + machine + "machine \n" + point, Toast.LENGTH_LONG).show();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
			long totalDataLen, long longSampleRate, int channels, long byteRate)
			throws IOException {

		byte[] header = new byte[44];

		header[0] = 'R'; // RIFF/WAVE header
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
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
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
		header[32] = (byte) (2 * 16 / 8); // block align
		header[33] = 0;
		header[34] = RECORDER_BPP; // bits per sample
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

	//ON ACTION METHODS ==============================================
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


}