package com.xfdingustc.mjpegview.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import com.xfdingustc.mjpegview.library.MjpegView;

import java.net.InetSocketAddress;

public class MainActivity extends AppCompatActivity {

    private EditText mEtAddress;
    private EditText mEtPort;

    private Button mBtnConnect;
    private Button mBtnDisconnect;

    private MjpegView mMjpegView;
    private Switch mUseMina;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEtAddress = (EditText)findViewById(R.id.address);
        mEtPort = (EditText)findViewById(R.id.port);
        mBtnConnect = (Button)findViewById(R.id.connect);
        mBtnDisconnect = (Button)findViewById(R.id.disconnect);
        mMjpegView = (MjpegView)findViewById(R.id.mjpeg_view);

        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String address = mEtAddress.getText().toString();
                int port = Integer.parseInt(mEtPort.getText().toString());
                mMjpegView.startStream(new InetSocketAddress(address, port));
            }
        });


        mBtnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMjpegView.stopStream();
            }
        });

    }
}
