package se.shumeika.record;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;

public class TunerEngine {
	static {
		System.loadLibrary("FFT");
	}

	public native double processSampleData(byte[] sample, int sampleRate);

	private static final int[] OPT_SAMPLE_RATES = { 11025, 8000, 22050, 44100 };
	private static final int[] BUFFERSIZE_PER_SAMPLE_RATE = { 8 * 1024, 4 * 1024, 16 * 1024, 32 * 1024 };

	public double currentFrequency = 0.0;

	int SAMPLE_RATE = 8000;
	int READ_BUFFERSIZE = 4 * 1024;

	AudioRecord targetDataLine_;

	final Handler mHandler;
	Runnable callback;

	public TunerEngine(Handler mHandler, Runnable callback) {
		this.mHandler = mHandler;
		this.callback = callback;
		initAudioRecord(44100);
	}

	// private void initAudioRecord(){
	// int counter = 0;
	// for(int sampleRate : OPT_SAMPLE_RATES){
	// initAudioRecord(sampleRate);
	// if(targetDataLine_.getState() == AudioRecord.STATE_INITIALIZED ){
	// SAMPLE_RATE = sampleRate;
	// READ_BUFFERSIZE = BUFFERSIZE_PER_SAMPLE_RATE[counter];
	// break;
	// }
	// counter++;
	// }
	// }

	private void initAudioRecord(int sampleRate) {
		targetDataLine_ = new AudioRecord(MediaRecorder.AudioSource.MIC,
				sampleRate,
				android.media.AudioFormat.CHANNEL_CONFIGURATION_MONO,
				android.media.AudioFormat.ENCODING_PCM_16BIT, sampleRate * 6);
		if (targetDataLine_.getState() == AudioRecord.STATE_INITIALIZED) {
			SAMPLE_RATE = sampleRate;
			READ_BUFFERSIZE = 32 * 1024; // BUFFERSIZE_PER_SAMPLE_RATE[counter];
		}
	}

	byte[] bufferRead;

	// long l;
	public void run() { // fft

		targetDataLine_.startRecording();
		bufferRead = new byte[READ_BUFFERSIZE];
		int n = -1;
		while ((n = targetDataLine_.read(bufferRead, 0, READ_BUFFERSIZE)) > 0) {
			// l = System.currentTimeMillis();
			currentFrequency = processSampleData(bufferRead, SAMPLE_RATE);
			// System.out.println("process time  = " +
			// (System.currentTimeMillis() - l));
			if (currentFrequency > 0) {
				mHandler.post(callback);
				try {
					targetDataLine_.stop();
					Thread.sleep(20);
					targetDataLine_.startRecording();
				} catch (InterruptedException e) {
					// e.printStackTrace();
				}
			}
		}
	}

	public void close() {
		// targetDataLine_.stop();
		targetDataLine_.release();
	}
}
