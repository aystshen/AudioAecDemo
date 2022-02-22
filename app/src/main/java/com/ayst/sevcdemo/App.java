package com.ayst.sevcdemo;

import android.app.Application;
import android.util.Log;

import com.aispeech.AIEchoConfig;
import com.aispeech.DUILiteConfig;
import com.aispeech.DUILiteSDK;

public class App extends Application {
    private static final String TAG = "AudioAecDemo";

    public static boolean isInit = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // 产品认证需设置 apiKey, productId, productKey, productSecret
        DUILiteConfig config = new DUILiteConfig(
                "ae68a4bdaadfae68a4bdaadf61e673af",      // apiKey
                "279608264",                            // productId
                "d25a499f21eca904a3840492404c3b6c",     // productKey
                "93f292ef7013177faf914ab2d8e143cd");    // productSecret
//        config.setOfflineProfileName("auth.txt");           // 离线授权文件
//        config.setUpdateTrailProfileToOnlineProfile(true);
        config.setExtraParameter("DEVICE_NAME",AppUtils.getDeviceId());
        config.setExtraParameter("DEVICE_ID", AppUtils.getDeviceId());

        config.setAudioRecorderType(DUILiteConfig.TYPE_COMMON_ECHO);
        if (config.getAudioRecorderType() == DUILiteConfig.TYPE_COMMON_ECHO) {
            AIEchoConfig aiEchoConfig = new AIEchoConfig();
            aiEchoConfig.setAecResource(Constants.AEC_RES); // 设置echo的AEC资源文件
            aiEchoConfig.setChannels(2);                    // 音频通道数
            aiEchoConfig.setMicNumber(1);                   // 真实mic数

            // 1：即左通道为录音，右通道为回采
            // 2：即右通道为录音，左通道为回采
            aiEchoConfig.setRecChannel(2);

            // 设置保存的AEC原始输入和AEC之后的音频文件路径
            aiEchoConfig.setSavedDirPath("/sdcard/aispeech/aecPcmFile/");

            config.setEchoConfig(aiEchoConfig);
        }

        // 打开Debug日志
        //DUILiteSDK.openLog(this, "/sdcard/duilite/log");

        // 初始化数据及授权
        DUILiteSDK.init(getApplicationContext(), config, new DUILiteSDK.InitListener() {
            @Override
            public void success() {
                Log.d(TAG, "Authorization success!");
                DUILiteSDK.setDebugMode(2);

                isInit = true;
            }

            @Override
            public void error(String errorCode, String errorInfo) {
                Log.d(TAG, "Authorization failure, errorCode: " + errorCode + ", errorInfo:" + errorInfo);
            }
        });
    }
}
