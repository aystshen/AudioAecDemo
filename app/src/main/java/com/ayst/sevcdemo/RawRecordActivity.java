package com.ayst.sevcdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.ayst.audio.NativeAudioRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 原始录音
 */
public class RawRecordActivity extends AppCompatActivity {
    private static final String TAG = "AudioAecDemo";

    private ToggleButton mRecordBtn;

    private NativeAudioRecord mAudioRecord;
    private AudioTrack mAudioTrack;

    private String mLocalFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        // 创建文件路径
        mLocalFilePath = ContextCompat.getExternalFilesDirs(this,
                Environment.DIRECTORY_MUSIC)[0].getAbsolutePath() + File.separator + "nar.pcm";

        // 创建录音器
        mAudioRecord = new NativeAudioRecord(0, 1, 4, 16000,
                NativeAudioRecord.PCM_FORMAT.PCM_FORMAT_S16_LE.value, 1024, 3);

        // 创建AudioTrack
        int bufferSize = AudioTrack.getMinBufferSize(16000,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize, AudioTrack.MODE_STREAM);

        mRecordBtn = (ToggleButton) findViewById(R.id.btn_record);
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

    private void record() {
        Log.i(TAG, "record");

        // 开始录音
        mAudioRecord.start();
        // 开始播放
        mAudioTrack.play();
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(mLocalFilePath);
                if (file.exists()) {
                    file.delete();
                }

                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(mLocalFilePath);
                    byte[] buffer;

                    // 循环读取语音流（PCM数据包）
                    while (mAudioRecord.isCapturing() && ((buffer = mAudioRecord.read()) != null)) {
                        Log.d(TAG, "AudioRecord read: " + buffer.length);

                        // PCM数据保存到本地文件
                        fos.write(buffer);

                        // PCM数据写入AudioTrack中播放
                        byte[] tmpBuf = trim(buffer);
                        mAudioTrack.write(tmpBuf, 0, tmpBuf.length);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    private void stop() {
        mAudioTrack.stop();
        mAudioRecord.stop();
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

        for (int i = 0; i < dest.length; i = i + 2) {
            dest[i] = src[i * 2 + 2];
            dest[i + 1] = src[i * 2 + 3];
        }

        return dest;
    }
}