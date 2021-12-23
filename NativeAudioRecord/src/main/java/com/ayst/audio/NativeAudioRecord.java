package com.ayst.audio;

import android.util.Log;

public class NativeAudioRecord {
    private static final String TAG = "NativeAudioRecord";

    static {
        System.loadLibrary("audiorecord");
    }

    private boolean isCapturing = false;
    private int mCard;
    private int mDevice;
    private int mChannels;
    private int mRate;
    private int mFormat;
    private int mPeriodSize;
    private int mPeriodCount;

    public NativeAudioRecord(int card, int device,
                             int channels, int rate,
                             int format, int periodSize,
                             int periodCount) {
        mCard = card;
        mDevice = device;
        mChannels = channels;
        mRate = rate;
        mFormat = format;
        mPeriodSize = periodSize;
        mPeriodCount = periodCount;
    }

    public boolean start() {
        if (!isCapturing) {
            isCapturing = nativeStart(mCard, mDevice, mChannels, mRate,
                    mFormat, mPeriodSize, mPeriodCount) == 0;
        } else {
            Log.w(TAG, "start, already started.");
        }
        return isCapturing;
    }

    public byte[] read() {
        if (isCapturing) {
            return nativeRead();
        } else {
            Log.e(TAG, "read, please call start().");
            return null;
        }
    }

    public void stop() {
        if (isCapturing) {
            isCapturing = false;
            nativeStop();
        }
    }

    public boolean isCapturing() {
        return isCapturing;
    }

    private native int nativeStart(int card, int device,
                                   int channels, int rate,
                                   int format, int period_size,
                                   int period_count);

    private native byte[] nativeRead();

    private native void nativeStop();

    public enum PCM_FORMAT {
        PCM_FORMAT_INVALID(-1),
        PCM_FORMAT_S16_LE(0),       /* 16-bit signed */
        PCM_FORMAT_S32_LE(1),       /* 32-bit signed */
        PCM_FORMAT_S8(2),           /* 8-bit signed */
        PCM_FORMAT_S24_LE(3),       /* 24-bits in 4-bytes */
        PCM_FORMAT_S24_3LE(4);      /* 24-bits in 3-bytes */
        public final int value;

        PCM_FORMAT(int v) {
            value = v;
        }
    }
}
