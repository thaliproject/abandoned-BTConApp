// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btconnectorlib;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;

/**
 * Created by juksilve on 12.3.2015.
 */
public class BTConnectToThread extends Thread {

    private BluetoothBase.BluetoothStatusChanged callback;
    private final BluetoothSocket mSocket;

    public BTConnectToThread(BluetoothBase.BluetoothStatusChanged Callback, BluetoothDevice device) {
        callback = Callback;
        BluetoothSocket tmp = null;
        try {
            tmp = device.createInsecureRfcommSocketToServiceRecord(BluetoothBase.MY_UUID);
        } catch (IOException e) {
            printe_line("createInsecure.. failed: " + e.toString());
        }
        mSocket = tmp;
    }
    public void run() {
        printe_line("Starting to connect");
        if(mSocket != null && callback != null) {
            try {
                mSocket.connect();
                //return success
                callback.Connected(mSocket);
            } catch (IOException e) {
                printe_line("socket connect failed: " + e.toString());
                try {
                    mSocket.close();
                } catch (IOException ee) {
                    printe_line("closing socket 2 failed: " + ee.toString());
                }
                callback.ConnectionFailed(e.toString());
            }
        }
    }

    private void printe_line(String message){
     //   Log.d("BTConnectToThread",  "BTConnectToThread: " + message);
    }

    public void Stop() {
        try {
            if(mSocket != null) {
                mSocket.close();
            }
        } catch (IOException e) {
            printe_line("closing socket failed: " + e.toString());
        }
    }
}
