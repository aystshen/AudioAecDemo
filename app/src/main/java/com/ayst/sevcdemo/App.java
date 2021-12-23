package com.ayst.sevcdemo;

import android.app.Application;
import android.util.Log;

import com.aispeech.AIEchoConfig;
import com.aispeech.DUILiteConfig;
import com.aispeech.DUILiteSDK;

public class App extends Application {
    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();

        // 产品认证需设置 apiKey, productId, productKey, productSecret
        DUILiteConfig config = new DUILiteConfig(
                "0dea9c20e76c0dea9c20e76c61c03506",      // apiKey
                "279607580",                            // productId
                "acadcaf54f6267a93752b698672d5d96",     // productKey
                "2eb281bf3fbd1b807ee27cf630dd1305");    // productSecret
        config.setOfflineProfileName("auth.txt");           // 离线授权文件

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
            }

            @Override
            public void error(String errorCode, String errorInfo) {
                Log.d(TAG, "Authorization failure, errorCode: " + errorCode + ", errorInfo:" + errorInfo);
            }
        });
    }
}
