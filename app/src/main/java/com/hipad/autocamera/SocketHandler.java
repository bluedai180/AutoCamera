package com.hipad.autocamera;

import java.net.Socket;

/**
 * Created by HIPADUSER on 2017/5/24.
 */

public class SocketHandler {
    private static Socket socket;

    public static synchronized Socket getSocket() {
        return socket;
    }

    public static synchronized void setSocket(Socket socket) {
        SocketHandler.socket = socket;
    }
}
