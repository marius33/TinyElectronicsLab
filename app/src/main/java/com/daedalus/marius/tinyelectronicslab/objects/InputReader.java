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
    private final Handler mHandler;

    private final int sampleRate;

    private Thread mThread;

    public InputReader(Handler handler, int fs) {

        mHandler = handler;
        sampleRate = fs;
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mBuffer = new short[bufferSize];
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
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
//            double sum = 0;
            int readSize = mRecorder.read(mBuffer, 0, mBuffer.length);

            if (readSize > 0) {
                short[] samples = new short[readSize];
                for (int i = 0; i < readSize; i++) {
//                    sum += mBuffer[i] * mBuffer[i];
                    samples[i] = mBuffer[i];
                }
//
//                final int amplitude = (int) Math.sqrt((sum / readSize));

                Message message = new Message();
                message.obj = samples;

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

    public int getSampleRate() {
        return sampleRate;
    }

    public static int getPreferredSampleRate() {

        int[] sampleRates = new int[]{44100, 32000, 22050, 16000, 11025, 8000};
        for (int rate : sampleRates) {
            int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize != AudioRecord.ERROR_BAD_VALUE || bufferSize != AudioRecord.ERROR) {
                return rate;
            }
        }
        return -1;
    }
}

