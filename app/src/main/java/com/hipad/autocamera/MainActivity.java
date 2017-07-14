package com.hipad.autocamera;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Toolbar mToolbar;
    private final static String TAG = "MainActivity";
    private final static int MY_PERMISSION_CAMERA = 1;
    private String mMode = "fullsweep";
    private String mDistance = "cm";
    private String ip;
    private Handler mMainHandler;
    private Socket socket;
    OutputStream outputStream;
    private boolean isAuto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions(new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_LOGS,
                        Manifest.permission.INTERNET,
                        Manifest.permission.RECEIVE_BOOT_COMPLETED},
                MY_PERMISSION_CAMERA);

        findViewById(R.id.btn_8cm).setOnClickListener(this);
        findViewById(R.id.btn_10cm).setOnClickListener(this);
        findViewById(R.id.btn_14cm).setOnClickListener(this);
        findViewById(R.id.btn_20cm).setOnClickListener(this);
        findViewById(R.id.btn_50cm).setOnClickListener(this);
        findViewById(R.id.btn_30cm).setOnClickListener(this);
        findViewById(R.id.btn_40cm).setOnClickListener(this);
        findViewById(R.id.btn_70cm).setOnClickListener(this);
        findViewById(R.id.btn_120cm).setOnClickListener(this);
        findViewById(R.id.btn_other).setOnClickListener(this);
        findViewById(R.id.btn_auto).setOnClickListener(this);

        isAuto = getIntent().getBooleanExtra("auto", false);
        if (getIntent().getStringExtra("mode") != null) {
            mMode = getIntent().getStringExtra("mode");
        }
        if (getIntent().getStringExtra("ip") != null) {
            ip = getIntent().getStringExtra("ip");
            SharedPreferences sharedPreferences = getSharedPreferences("auto_camera", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("ip", ip);
            editor.apply();
            Toast.makeText(MainActivity.this, ip, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "set ip addr", Toast.LENGTH_SHORT).show();
        }
        RadioGroup group = (RadioGroup) findViewById(R.id.radio_group);
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.rb_fullsweep:
                        mMode = "fullsweep";
                        configCamera();
                        break;
                    case R.id.rb_single_AF_algorithm:
                        mMode = "single";
                        configCamera();
                        break;
                }
            }
        });
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        root();
        configCamera();
        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                        intent.putExtra("mode", mMode);
                        intent.putExtra("Scene", "8cm");
                        intent.putExtra("auto", true);
                        clearLogcat();
                        startActivity(intent);
                        break;
                }
            }
        };

        connectToPC();
        readFromPC();
    }

    @Override
    public void onClick(View v) {
        final Intent intent = new Intent(MainActivity.this, CameraActivity.class);
        intent.putExtra("mode", mMode);
        clearLogcat();
        switch (v.getId()) {
            case R.id.btn_8cm:
                intent.putExtra("Scene", "8cm");
                startActivity(intent);
                break;
            case R.id.btn_10cm:
                intent.putExtra("Scene", "10cm");
                startActivity(intent);
                break;
            case R.id.btn_14cm:
                intent.putExtra("Scene", "14cm");
                startActivity(intent);
                break;
            case R.id.btn_20cm:
                intent.putExtra("Scene", "20cm");
                startActivity(intent);
                break;
            case R.id.btn_30cm:
                intent.putExtra("Scene", "30cm");
                startActivity(intent);
                break;
            case R.id.btn_40cm:
                intent.putExtra("Scene", "40cm");
                startActivity(intent);
                break;
            case R.id.btn_50cm:
                intent.putExtra("Scene", "50cm");
                startActivity(intent);
                break;
            case R.id.btn_70cm:
                intent.putExtra("Scene", "70cm");
                startActivity(intent);
                break;
            case R.id.btn_120cm:
                intent.putExtra("Scene", "120cm");
                startActivity(intent);
                break;
            case R.id.btn_other:
                View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_choose_distance, null);
                final EditText editText = (EditText) view.findViewById(R.id.et_distance);
                Spinner spinner = (Spinner) view.findViewById(R.id.spinner_distance);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        mDistance = getResources().getStringArray(R.array.distances)[i];
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
                new AlertDialog.Builder(this)
                        .setTitle("Please input distance: ")
                        .setView(view)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                intent.putExtra("Scene", editText.getText().toString() + mDistance);
                                startActivity(intent);
                            }
                        }).show();
                break;
            case R.id.btn_auto:
                intent.putExtra("mode", mMode);
                intent.putExtra("Scene", "8cm");
                intent.putExtra("auto", true);
                clearLogcat();
                startActivity(intent);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        disconnect();
    }

    private void clearLogcat() {
        try {
            Runtime.getRuntime().exec("logcat -c");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configCamera() {
        try {
            if ("fullsweep".equals(mMode)) {
                Runtime.getRuntime().exec("setprop debug.camera.af_fullsweep 1");
            } else {
                Runtime.getRuntime().exec("setprop debug.camera.af_fullsweep 0");
            }
            Runtime.getRuntime().exec("setprop persist.camera.mobicat 2");
            Runtime.getRuntime().exec("setprop persist.camera.global.debug 1");
            Runtime.getRuntime().exec("setprop persist.camera.stats.af.debug 255");
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "config camera failed!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void root() {
        try {
            Runtime.getRuntime().exec("input keyevent 224");
            Runtime.getRuntime().exec("pm grant com.hipad.autocamera android.permission.READ_LOGS");
            Runtime.getRuntime().exec("setprop service.adb.tcp.port 5555");
            Runtime.getRuntime().exec("setprop sys.restart.adbd 1");
//            Runtime.getRuntime().exec("setprop sys.usb.config diag,adb");
//            Runtime.getRuntime().exec("setprop sys.usb.config mtp,adb");
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "adb root failed!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void disconnect() {
        try {
            if (outputStream != null && socket != null) {
                outputStream.close();
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectToPC() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (ip == null){
                        SharedPreferences sharedPreferences = getSharedPreferences("auto_camera", Activity.MODE_PRIVATE);
                        String localIp = sharedPreferences.getString("ip", "");
                        socket = new Socket(localIp, 5055);
                    } else {
                        socket = new Socket(ip, 5055);
                    }
                    SocketHandler.setSocket(socket);
                    if (socket.isConnected()) {
                        mMainHandler.sendEmptyMessage(0);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "connect: failed");
                }
            }
        }).start();
    }

    private void readFromPC() {
        final Socket socket = SocketHandler.getSocket();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (socket != null) {
                            InputStream is = socket.getInputStream();
                            InputStreamReader isr = new InputStreamReader(is);
                            BufferedReader br = new BufferedReader(isr);
                            String response = br.readLine();
                            if (response != null && response.contains("action:start")) {
                                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                                intent.putExtra("Scene", "8cm");
                                intent.putExtra("mode", mMode);
                                intent.putExtra("auto", true);
                                startActivity(intent);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
