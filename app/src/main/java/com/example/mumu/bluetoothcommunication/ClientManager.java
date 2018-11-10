package com.example.mumu.bluetoothcommunication;

import com.inuker.bluetooth.library.BluetoothClient;

/**
 * Created by mumu on 2018/11/6.
 */

public class ClientManager {
    private static BluetoothClient mClient;

    public static BluetoothClient getClient() {
        if (mClient == null) {
            synchronized (ClientManager.class) {
                if (mClient == null) {
                    mClient = new BluetoothClient(MyApplication.getInstance());
                }
            }
        }
        return mClient;
    }
}
