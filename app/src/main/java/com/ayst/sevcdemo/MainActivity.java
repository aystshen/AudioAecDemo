package com.ayst.sevcdemo;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import kr.co.namee.permissiongen.PermissionGen;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AudioAecDemo";

    private Button mRawRecordBtn;
    private Button mSpeechRecordBtn;
    private Button mZSRecordBtn;
    private Button mSpeexRecordBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PermissionGen.with(MainActivity.this)
                .addRequestCode(100)
                .permissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .request();

        mRawRecordBtn = (Button) findViewById(R.id.btn_raw_record);
        mRawRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, RawRecordActivity.class));
            }
        });

        mSpeechRecordBtn = (Button) findViewById(R.id.btn_speech_record);
        mSpeechRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SpeechEchoRecordActivity.class));
            }
        });

        mZSRecordBtn = (Button) findViewById(R.id.btn_zs_record);
        mZSRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, ZSEchoRecordActivity.class));
            }
        });

        mSpeexRecordBtn = (Button) findViewById(R.id.btn_speex_record);
        mSpeexRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SpeexEchoRecordActivity.class));
            }
        });
    }
}