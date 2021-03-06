package com.hipad.autocamera;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by HIPADUSER on 2017/5/24.
 */

public class SocketUtils {

    private static String IP_ADDR = "192.168.43.71";
    private static int PORT = 5055;
    private static Socket mSocket;

    public static void connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mSocket = new Socket(IP_ADDR, PORT);
                    SocketHandler.setSocket(mSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void disconnect() {
        try {
            if (mSocket != null && mSocket.getOutputStream() != null) {
                mSocket.getOutputStream().close();
                mSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void send(final String mode, final String scene) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket = SocketHandler.getSocket();
                if (socket != null) {
                    try {
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(("package:af class:"+ mode + " case:"+ scene +" result:pass"+" \n").getBytes("utf-8"));
                        outputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public static void receive() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mSocket != null) {
                        InputStream is = mSocket.getInputStream();
                        InputStreamReader isr = new InputStreamReader(is);
                        BufferedReader br = new BufferedReader(isr);
                        String response = br.readLine();
                        Log.d("bluedai", "run: " + response);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
