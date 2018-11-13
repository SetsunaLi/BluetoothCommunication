package com.example.mumu.bluetoothcommunication;

import android.app.Application;

import com.inuker.bluetooth.library.BluetoothContext;

/**
 * Created by mumu on 2018/11/6.
 */

public class MyApplication extends Application {
    private static MyApplication instance;

    public static Application getInstance() {

        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
//        BluetoothContext.set(this);

    }
}
