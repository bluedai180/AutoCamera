package com.hipad.autocamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by HIPADUSER on 2017/5/18.
 */

public class CameraActivity extends Activity implements SurfaceHolder.Callback{

    private SurfaceView mSurface;
    private Camera mCamera;
    private static final String TAG = "CameraActivity";
    private String mScene;
    private String mMode;
    private StringBuilder mLog;
    private TextView mPicNum;
    private TextView mFV;
    private TextView mPosition;
    private TextView mModeTV;
    private TextView mSceneTV;
    private boolean isAuto;
    private int num = 0;
    private int mRotation;
    private MyOrientationEventListener mOrientationEventListener;
    int i = 0;
    private static final int PIC_NUM = 1;
    private static final int SEND_AF_INFO = 2;
    private static final int RESPONSE = 3;
    private static final int AUTO_TAKE_PIC = 4;
    private static final int SWITCH_SENCE = 5;
    private static final int TURN_ON_FLASH = 6;
    private static final int TURN_OFF_FLASH = 7;
//    private String[] mSenceList = {"8cm", "10cm"};
    private String[] mSenceList = {"8cm", "10cm", "14cm", "20cm", "30cm", "40cm", "50cm", "60cm", "120cm", "245cm", "far end"};
    private String[] mModeList = {"fullsweep", "single"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        writeLog.start();
        mScene = getIntent().getStringExtra("Scene");
        mMode = getIntent().getStringExtra("mode");
        isAuto = getIntent().getBooleanExtra("auto", false);
        mSurface = (SurfaceView) findViewById(R.id.surface);
        mPicNum = (TextView) findViewById(R.id.tv_pic_num);
        mFV = (TextView) findViewById(R.id.tv_fv);
        mPosition = (TextView) findViewById(R.id.tv_position);
        mModeTV = (TextView) findViewById(R.id.tv_mode);
        mSceneTV = (TextView) findViewById(R.id.tv_sence);
        mModeTV.setText("Mode: " + mMode);
        mSceneTV.setText("Scene: " + mScene);
        SurfaceHolder surfaceHolder = mSurface.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(this);
        mOrientationEventListener = new MyOrientationEventListener(this);
        findViewById(R.id.btn_shutter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Camera.Parameters parameters = mCamera.getParameters();
                Log.d("bluedai", "onClick: " + getJepgRotation(mRotation));
                Camera.CameraInfo info = new Camera.CameraInfo();
                parameters.setRotation(getJepgRotation(mRotation));
                mCamera.setParameters(parameters);
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean b, Camera camera) {
                        if (b) {
                            Log.d("bluedai", "onAutoFocus: " + b);
                            mCamera.takePicture(null, null, mPictureCallback);
                            mHandler.sendEmptyMessage(PIC_NUM);
                            num += 1;
                        }
                    }
                });

            }
        });
        readFromPC();
        if (isAuto) {
            autoTakePic();
        }
    }

    Thread writeLog = new Thread(new Runnable() {
        @Override
        public void run() {
            writeLogFile();
        }
    });

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PIC_NUM:
                    mPicNum.setText(num + " pic");
                    break;
                case SEND_AF_INFO:
                    mFV.setText(msg.getData().getString("af"));
                    mPosition.setText("CurPosition: " + msg.getData().getString("position"));
                    break;
                case RESPONSE:
                    Toast.makeText(CameraActivity.this, msg.getData().getString("response"), Toast.LENGTH_SHORT).show();
                    break;
                case AUTO_TAKE_PIC:
                    if (mCamera != null) {
                        mCamera.autoFocus(new Camera.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean b, Camera camera) {
                                if (b){
                                    mCamera.takePicture(null, null, mPictureCallback);
                                    mHandler.sendEmptyMessage(PIC_NUM);
                                    num += 1;
                                    autoTakePic();
                                } else {
                                    autoTakePic();
                                }
                            }
                        });
                    }
                    break;
                case SWITCH_SENCE:
                    writeToFile();
                    i += 1;
                    num = 0;
                    mHandler.sendEmptyMessage(PIC_NUM);
                    if (i < mSenceList.length) {
                        mScene = mSenceList[i];
                        mSceneTV.setText("Sence: " + mScene);
                    } else {
                        try {
                            sendToPC("package:af action:done \n");
                            if ("single".equals(mMode)){
                                Runtime.getRuntime().exec("input keyevent 4");
                                Runtime.getRuntime().exec("input keyevent 4");
                            } else {
                                Runtime.getRuntime().exec("input keyevent 4");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    sendToPC("package:af action:move \n");
                    break;
                case TURN_ON_FLASH:
                    Camera.Parameters parameters = null;
                    parameters = mCamera.getParameters();
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(parameters);
                    break;
                case TURN_OFF_FLASH:
                    Camera.Parameters parameters_off = null;
                    parameters_off = mCamera.getParameters();
                    parameters_off.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(parameters_off);
                    break;
            }
        }
    };


    private void writeLogFile () {
        try {
            String[] cmd = {"logcat"};
            java.lang.Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            mLog = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains("af_util_cur_pos_after_lens_move:")) {
                    mLog.append(line + "\n");
                    Message message = mHandler.obtainMessage();
                    message.what = SEND_AF_INFO;
                    Bundle bundle = new Bundle();
                    bundle.putString("af", "AF: " + line.split(":")[7].replace("CurPosition", ""));
                    bundle.putString("position", line.split(":")[8]);
                    message.setData(bundle);
                    mHandler.sendMessage(message);
                    Log.d(TAG, "AF: " + line.split(":")[7].replace("CurPosition", ""));
                    Log.d(TAG, "CurPosition: " + line.split(":")[8]);

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            Log.d("bluedai", "handleMessage: takepic" + mMode + mScene);
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "AutoCamera/" + mMode + "/" + mScene);
            if (!mediaStorageDir.exists()){
                if (!mediaStorageDir.mkdirs()){
                    Log.d(TAG, "failed to create directory");
                }
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            File pictureFile = new File(mediaStorageDir.getPath() + File.separator + mScene + "_" + timeStamp + ".jpg");
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(bytes);
                fos.close();
//                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(pictureFile));
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
//                bos.flush();
//                bos.close();
                camera.stopPreview();
                camera.startPreview();
//                mCamera.cancelAutoFocus();
                bitmap.recycle();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (mLog != null) {
                mLog.append("Picture name: " + mScene + "_" + timeStamp + ".jpg");
                mLog.append("======================================================\n");
            }
            sendToPC("package:af class:"+ mMode + " case:"+ mScene +" result:pass"+" \n");
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mOrientationEventListener.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOrientationEventListener.disable();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            Log.d(TAG, "surfaceCreated: ");
            mCamera = Camera.open(0);
            if (mCamera != null) {
                mCamera.setPreviewDisplay(surfaceHolder);
                Camera.Parameters parameters = mCamera.getParameters();
                for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
                    Log.d(TAG, "surfaceCreated: previewsize " + size.width + size.height);
                }
                for (Camera.Size size : parameters.getSupportedPictureSizes()) {
                    Log.d(TAG, "surfaceCreated: picturesize " + size.width + size.height);
                }
                parameters.setPreviewSize(1280, 720);
                parameters.setPictureSize(4160, 3120);

//                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                parameters.setFocusMode(Camera.Parameters.FLASH_MODE_AUTO);
//                parameters.setRotation(90);
                mCamera.setParameters(parameters);
                mCamera.setDisplayOrientation(90);
                mCamera.enableShutterSound(true);
                mCamera.startPreview();
                mCamera.cancelAutoFocus();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.d(TAG, "surfaceChanged: ");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceDestroyed: ");
        if (mCamera != null) {
            mCamera.stopPreview();
            try {
                mCamera.setPreviewDisplay(null);
            } catch (Exception e) {
                Log.d(TAG, "surfaceDestroyed: " + e.toString());
            }
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        writeToFile();
    }

    private class MyOrientationEventListener extends OrientationEventListener{

        public MyOrientationEventListener (Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            mRotation = orientation;
            Log.d("bluedai", "onOrientationChanged: " + orientation);
        }
    }

    private int getJepgRotation(int orientation) {
        int rotation = 0;
        if (orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
//            Camera.CameraInfo info = new Camera.CameraInfo();
            if (orientation < 45 || orientation >= 315) {
                rotation = 90;
            } else if (orientation >= 45 && orientation < 135) {
                rotation = 180;
            } else if (orientation >= 135 && orientation < 225) {
                rotation = 270;
            } else if (orientation >= 225 && orientation < 315) {
                rotation = 0;
            }
//            rotation = (90 + orientation) % 360;
        }
        return rotation;
    }

    private void writeToFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File pictureFile = new File(new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "AutoCamera/" + mMode + "/" + mScene).getPath() + File.separator + mScene + "_" + timeStamp + ".log");
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(mLog.toString().getBytes());
            fos.close();
            Toast.makeText(CameraActivity.this, "Write log file successful!", Toast.LENGTH_SHORT).show();
            mLog.delete(0, mLog.length());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(CameraActivity.this, "Write log file failed!", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendToPC(final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket = SocketHandler.getSocket();
                if (socket != null) {
                    try {
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(text.getBytes("utf-8"));
                        outputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
                            if (response != null) {
                                Message massage = mHandler.obtainMessage();
                                massage.what = RESPONSE;
                                Bundle bundle = new Bundle();
                                bundle.putString("response", response);
                                massage.setData(bundle);
                                mHandler.sendMessage(massage);
                            }
                            if (response.contains("result:move")) {
                                autoTakePic();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void autoTakePic() {
        if (num < 10) {
            mHandler.sendEmptyMessageDelayed(AUTO_TAKE_PIC, 3000);
        } else if (num == 10) {
            mHandler.sendEmptyMessageDelayed(SWITCH_SENCE, 2000);
        }
    }
}
