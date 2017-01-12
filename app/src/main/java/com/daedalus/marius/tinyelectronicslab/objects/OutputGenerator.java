package com.daedalus.marius.tinyelectronicslab.objects;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;

public class OutputGenerator implements Runnable {

    private AudioTrack mTrack;
    private boolean mIsRunning = false;
    private boolean mIsPaused = false;
    private short[] mBuffer;
    private int frequency = 1000;
    private float phi = 0;
    private float angle = 0;
    public short amplitude = 1000;
    //private Handler mHandler;
    private Thread thread;

    public static final int SINUSOID = 0;
    public static final int TRIANGLE = 1;
    public static final int SQUARE = 2;

    public int function;

    public OutputGenerator() {
        int sampleRateInHz = AudioTrack
                .getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);

        int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        mTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeInBytes, AudioTrack.MODE_STREAM);

        phi = (float) (2 * Math.PI * frequency / sampleRateInHz);

        mBuffer = new short[bufferSizeInBytes];

        function = SINUSOID;

        thread = new Thread(this);

    }

    public OutputGenerator(int frequency, Handler handler, short amplitude) {

        int sampleRateInHz = AudioTrack
                .getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);

        int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        this.amplitude = amplitude;

        mTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeInBytes, AudioTrack.MODE_STREAM);

        this.frequency = frequency;

        //mHandler = handler;

        phi = (float) (2 * Math.PI * frequency / sampleRateInHz);

        mBuffer = new short[bufferSizeInBytes];

        function = SINUSOID;

        thread = new Thread(this);

    }

    @Override
    public void run() {
        while (mIsRunning) {

            synchronized (thread) {
                while (mIsPaused) {
                    try {
                        thread.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }

            // float sum = 0;
            for (int i = 0; i < mBuffer.length; i++) {

                switch(function){
                    case SINUSOID: mBuffer[i] = (short) (amplitude * Math.sin(angle));
                        break;
                    case TRIANGLE: mBuffer[i] = (short) (amplitude * Utilities.triangle(angle));
                        break;
                    case SQUARE: mBuffer[i] = (short) (amplitude * Utilities.square(angle));
                }

                // sum += mBuffer[i] * mBuffer[i];
                angle += phi;
                angle %= (2f * Math.PI);
            }

            mTrack.write(mBuffer, 0, mBuffer.length);



			/*if(mHandler!=null) {


                Message message = new Message();
                message.what = 1;
                message.obj = mBuffer;

                mHandler.sendMessage(message);
            }*/

        }

    }

    public void start() {
        if (!mIsRunning) {
            mIsRunning = true;
            thread.start();
        } else
            synchronized (thread) {
                mIsPaused = false;
                thread.notifyAll();
            }

        mTrack.play();
    }

    public void pause() {
        synchronized (thread) {
            mIsPaused = true;
        }
        mTrack.pause();
    }

    public void stop() {
        if (mIsRunning)
            mTrack.stop();
        mIsRunning = false;
        mTrack.release();

    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
        phi = (float) (2 * Math.PI * frequency / mTrack.getSampleRate());
    }

    public int getFrequency() {
        return frequency;
    }

    /*public void setHandler(Handler handler) {
        mHandler = handler;
    }*/

}
