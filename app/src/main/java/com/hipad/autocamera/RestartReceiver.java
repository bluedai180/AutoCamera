package com.hipad.autocamera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by HIPADUSER on 2017/5/31.
 */

public class RestartReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("bluedai", "onReceive: " + intent.getAction());
        Intent restart = new Intent(context, MainActivity.class);
        restart.putExtra("mode", "single");
        restart.putExtra("auto", true);
        context.startActivity(restart);
    }
}
