package com.daedalus.marius.tinyelectronicslab.objects;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;

public class InputReader implements Runnable {

    private AudioRecord mRecorder;

    private short[] mBuffer;
    private boolean mIsRunning = false;
    private boolean mIsPaused = false;
    private Handler mHandler;

    private int sampleRate;

    private Thread mThread;

    public InputReader(Handler handler) {

        mHandler = handler;
        mRecorder = makeAudioRecord();
        mThread = new Thread(this);

    }

    @Override
    public void run() {
        while (mIsRunning) {
            synchronized (mThread) {
                while (mIsPaused) {
                    try {
                        mThread.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            double sum = 0;
            int readSize = mRecorder.read(mBuffer, 0, mBuffer.length);

            if (readSize > 0) {
                short[] samples = new short[readSize];
                for (int i = 0; i < readSize; i++) {
                    sum += mBuffer[i] * mBuffer[i];
                    samples[i] = mBuffer[i];
                }

                final int amplitude = (int) Math.sqrt((sum / readSize));

                Message message = new Message();
                message.arg1 = amplitude;
                message.arg2 = readSize;
                message.obj = samples;
                message.what = 0;

                if (mHandler != null)
                    mHandler.sendMessage(message);

            }

        }

        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

    }

    public void start() {
        if (mRecorder == null)
            mRecorder = makeAudioRecord();

        if (!mIsRunning) {
            mIsRunning = true;
            mThread.start();
        } else synchronized (mThread) {
            mIsPaused = false;
            mThread.notifyAll();
        }

        mRecorder.startRecording();

    }

    public void pause() {
        synchronized (mThread) {
            mIsPaused = true;
        }

        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }

    }

    public void stop() {
        if (mIsRunning && mRecorder != null)
            mRecorder.stop();

        mIsRunning = false;
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    private AudioRecord makeAudioRecord() {
        int[] sampleRates = new int[]{44100, 32000, 22050, 16000, 11025, 8000};
        for (short audioFormat : new short[]{AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT}) {
            for (int rate : sampleRates) {
                try {
                    int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, audioFormat);
                    if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO, audioFormat, bufferSize);

                        if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                            mBuffer = new short[bufferSize];
                            sampleRate = rate;
                            return recorder;
                        }//else
                        //   recorder.release();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
        return null;
    }

}
