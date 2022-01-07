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

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.echo.EchoKernel;
import com.aispeech.echo.EchoKernelListener;
import com.ayst.audio.NativeAudioRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 思必驰消回算法
 */
public class SpeechEchoRecordActivity extends AppCompatActivity {
    private static final String TAG = "AudioAecDemo";

    private ToggleButton mRecordBtn;

    private EchoKernel mEchoKernel;
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
                NativeAudioRecord.PCM_FORMAT.PCM_FORMAT_S16_LE.value, 1024, 3);

        // 创建AudioTrack
        int bufferSize = AudioTrack.getMinBufferSize(16000,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize, AudioTrack.MODE_STREAM);

        // 初始化引擎
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (App.isInit) {
                    Log.i(TAG, "create echo kernel");
                    mEchoKernel = new EchoKernel(new MyEchoKernelListener());
                    mEchoKernel.newKernel();
                    mRecordBtn.setEnabled(true);
                } else {
                    mHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);


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

        // 启动引擎
        mEchoKernel.startKernel();
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

                    // PCM数据传入引擎处理
                    mEchoKernel.feed(tmpBuf);
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
        mEchoKernel.stopKernel();
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
        mEchoKernel.releaseKernel();
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

        for (int i = 0; i < dest.length; i = i + 2) {
            dest[i] = src[i * 2 + 2];
            dest[i + 1] = src[i * 2 + 3];
        }

        return dest;
    }

    /**
     * 引擎回调
     */
    private class MyEchoKernelListener implements EchoKernelListener {

        @Override
        public void onInit(final int status) {
            Log.i(TAG, "onInit, init result: " + status);
            if (status == AIConstant.OPT_SUCCESS) {
                Log.i(TAG, "onInit, init success");

                // 引擎初始化完成后使能录音按钮
                mRecordBtn.setEnabled(true);
            } else {
                Log.e(TAG, "onInit, initialization failed: " + status);
            }
        }

        @Override
        public void onResultBufferReceived(byte[] buffer) {
            Log.i(TAG, "onResultBufferReceived " + buffer.length);

            // 引擎处理后数据保存到本地文件
            try {
                mEchoFos.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 引擎处理后数据写入AudioTrack中播放
            byte[] tmpBuf = new byte[buffer.length];
            System.arraycopy(buffer, 0, tmpBuf, 0, buffer.length);
            mAudioTrack.write(tmpBuf, 0, tmpBuf.length);
        }

        @Override
        public void onAgcDataReceived(byte[] buffer) {
            Log.i(TAG, "onAgcDataReceived, size: " + buffer.length);
        }

        @Override
        public void onError(final AIError error) {
            Log.e(TAG, "onError, " + error.toString());
        }
    }
}