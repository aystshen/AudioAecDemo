package com.ayst.sevcdemo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.audio.signal.core.AudioSignal;
import com.ayst.audio.NativeAudioRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 志晟消回算法
 */
public class ZSEchoRecordActivity extends AppCompatActivity {
    private static final String TAG = "AudioAecDemo";

    private static final int PERIOD_SIZE = 240;
    private static final int PERIOD_COUNT = 10;

    private ToggleButton mRecordBtn;

    private AudioSignal mAudioSignal;
    private NativeAudioRecord mAudioRecord;
    private AudioTrack mAudioTrack;

    private String mOriginFilePath;
    private String mEchoFilePath;
    private FileOutputStream mOriginFos = null;
    private FileOutputStream mEchoFos = null;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        mHandler = new Handler(getMainLooper());

        // 创建文件路径
        mOriginFilePath = ContextCompat.getExternalFilesDirs(this,
                Environment.DIRECTORY_MUSIC)[0].getAbsolutePath() + File.separator + "origin.pcm";
        mEchoFilePath = ContextCompat.getExternalFilesDirs(this,
                Environment.DIRECTORY_MUSIC)[0].getAbsolutePath() + File.separator + "echo.pcm";

        // 创建录音器
        mAudioRecord = new NativeAudioRecord(0, 1, 4, 16000,
                NativeAudioRecord.PCM_FORMAT.PCM_FORMAT_S16_LE.value, 240, 10);

        // 创建AudioTrack
        int bufferSize = AudioTrack.getMinBufferSize(16000,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize, AudioTrack.MODE_STREAM);

        mRecordBtn = (ToggleButton) findViewById(R.id.btn_record);
        mRecordBtn.setEnabled(false);
        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    record();
                } else {
                    stop();
                }
            }
        });

        // 初始化引擎
        mAudioSignal = new AudioSignal();
        if (mAudioSignal.init() == 0) {
            mRecordBtn.setEnabled(true);
        }
    }

    /**
     * 开始录音
     */
    private void record() {
        Log.i(TAG, "record");

        // 创建本地文件
        File file = new File(mOriginFilePath);
        if (file.exists()) {
            file.delete();
        }
        try {
            mOriginFos = new FileOutputStream(mOriginFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        file = new File(mEchoFilePath);
        if (file.exists()) {
            file.delete();
        }
        try {
            mEchoFos = new FileOutputStream(mEchoFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 开始录音
        mAudioRecord.start();
        // 开始播放
        mAudioTrack.play();
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer;

                // 循环读取语音流（PCM数据包）
                while (mAudioRecord.isCapturing() && ((buffer = mAudioRecord.read()) != null)) {
                    Log.d(TAG, "AudioRecord read: " + buffer.length);

                    // PCM数据预处理
                    byte[] tmpBuf = trim(buffer);

                    // PCM数据写入文件
                    try {
                        mOriginFos.write(tmpBuf);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    for (int i = 0; i < PERIOD_COUNT; i++) {

                        // PCM数据传入引擎处理（限定每帧240个采样数据）
                        byte[] srcBuf = new byte[PERIOD_SIZE * 2 * 2];
                        byte[] destBuf = new byte[PERIOD_SIZE * 2];
                        System.arraycopy(tmpBuf, i * PERIOD_SIZE * 2 * 2, srcBuf, 0, PERIOD_SIZE * 2 * 2);
                        mAudioSignal.process(srcBuf, srcBuf.length, destBuf, destBuf.length);

                        // 引擎处理后数据保存到本地文件
                        try {
                            mEchoFos.write(destBuf);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // 引擎处理后数据写入AudioTrack中播放
                        byte[] trackBuf = new byte[destBuf.length];
                        System.arraycopy(destBuf, 0, trackBuf, 0, destBuf.length);
                        mAudioTrack.write(trackBuf, 0, trackBuf.length);
                    }
                }
            }
        }).start();
    }

    /**
     * 停止录音
     */
    private void stop() {
        mAudioTrack.stop();
        mAudioRecord.stop();
        try {
            mOriginFos.close();
            mEchoFos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        stop();
        mAudioTrack.release();
        super.onDestroy();
    }

    /**
     * PCM数据预处理，4通道PCM数据转2通道数据
     *
     * @param src 4通道PCM数据
     * @return 2通道PCM数据
     */
    private byte[] trim(byte[] src) {
        byte[] dest = new byte[src.length / 2];

        // 4通道取2通道
        for (int i = 0; i < dest.length; i = i + 2) {
            dest[i] = src[i * 2 + 2];
            dest[i + 1] = src[i * 2 + 3];
        }

        // 去掉第一帧数据，将MIC与REF反转
        byte[] reverse = new byte[src.length / 2];
        System.arraycopy(dest, 2, reverse, 0, dest.length - 2);

        return reverse;
    }
}