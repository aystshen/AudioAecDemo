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

import com.ayst.audio.NativeAudioRecord;
import com.speex.EchoCanceller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * speex开源消回算法
 */
public class SpeexEchoRecordActivity extends AppCompatActivity {
    private static final String TAG = "AudioAecDemo";

    private static final int SAMPLE_RATE = 16000;
    private static final int PERIOD_SIZE = 240;
    private static final int PERIOD_COUNT = 10;
    private static final int FRAME_SIZE = PERIOD_SIZE * PERIOD_COUNT;

    private ToggleButton mRecordBtn;

    private EchoCanceller mEchoCanceller;
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
        mAudioRecord = new NativeAudioRecord(0, 1, 4, SAMPLE_RATE,
                NativeAudioRecord.PCM_FORMAT.PCM_FORMAT_S16_LE.value, PERIOD_SIZE, PERIOD_COUNT);

        // 创建AudioTrack
        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
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
        mEchoCanceller = new EchoCanceller();
        mRecordBtn.setEnabled(true);
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

        // 打开算法引擎
        mEchoCanceller.open(SAMPLE_RATE, FRAME_SIZE, 4096);
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

                    // 从4通道数据中提取2通道有效数据
                    short[] tmpBuf = trim(byte2Short(buffer));

                    // PCM数据写入文件（debug）
                    try {
                        mOriginFos.write(short2Byte(tmpBuf));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // 分离麦克风录音与参考（回采）数据
                    short[] recFrame = getRecFrame(tmpBuf);
                    short[] refFrame = getRefFrame(tmpBuf);

                    // 消除回声处理
                    short[] outFrame = mEchoCanceller.process(recFrame, refFrame);

                    // 软件增益
                    short[] amplifyBuf = amplifyPCMData(outFrame, 4.0f);

                    // short转byte
                    byte[] destBuf = short2Byte(amplifyBuf);

                    // 引擎处理后数据保存到本地文件（debug）
                    try {
                        mEchoFos.write(destBuf);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // 引擎处理后数据写入AudioTrack中播放
                    mAudioTrack.write(destBuf, 0, destBuf.length);
                }
                mEchoCanceller.close();
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
     * PCM数据预处理，4通道PCM数据中取第2、4通道数据
     * 第2通道：回采
     * 第4通道：录音
     *
     * @param src 4通道PCM数据
     * @return 2通道PCM数据
     */
    private short[] trim(short[] src) {
        short[] dest = new short[src.length / 2];

        // 4通道取2通道
        for (int i = 0; i < dest.length; i++) {
            dest[i] = src[i * 2 + 1];
        }

        return dest;
    }

    /**
     * 从PCM数据中获取麦克风录音帧
     *
     * @param src 麦克风与参考（回采）帧混合数据
     * @return 麦克风录音帧
     */
    private short[] getRecFrame(short[] src) {
        short[] dest = new short[src.length / 2];

        for (int i = 0; i < dest.length; i++) {
            dest[i] = src[i * 2 + 1];
        }

        return dest;
    }

    /**
     * 从PCM数据中获取参考（回采）帧
     *
     * @param src 麦克风与参考（回采）帧混合数据
     * @return 参考（回采）帧
     */
    private short[] getRefFrame(short[] src) {
        short[] dest = new short[src.length / 2];

        for (int i = 0; i < dest.length; i++) {
            dest[i] = src[i * 2];
        }

        return dest;
    }

    /**
     * byte数组转short数组
     *
     * @param data
     * @return
     */
    private short[] byte2Short(byte[] data) {
        short[] shortValue = new short[data.length / 2];
        for (int i = 0; i < shortValue.length; i++) {
            shortValue[i] = (short) ((data[i * 2] & 0xff) | ((data[i * 2 + 1] & 0xff) << 8));
        }
        return shortValue;
    }

    /**
     * short数据转byte数组
     *
     * @param data
     * @return
     */
    private byte[] short2Byte(short[] data) {
        byte[] byteValue = new byte[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            byteValue[i * 2] = (byte) (data[i] & 0xff);
            byteValue[i * 2 + 1] = (byte) ((data[i] & 0xff00) >> 8);
        }
        return byteValue;
    }

    /**
     * PCM软增益
     *
     * @param src 原始PCM数据
     * @param m   放大倍数
     * @return 增益后PCM数据
     */
    short[] amplifyPCMData(short[] src, float m) {
        short[] dest = new short[src.length];
        for (int i = 0; i < src.length; i++) {
            int tmp = (int) (src[i] * m);
            if (tmp > 32767) { // 溢出处理
                tmp = 32767;
            } else if (tmp < -32768) {
                tmp = -32768;
            }
            dest[i] = (short) tmp;
        }

        return dest;
    }
}