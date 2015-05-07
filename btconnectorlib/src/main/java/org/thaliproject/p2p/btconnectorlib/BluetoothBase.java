// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btconnectorlib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Created by juksilve on 6.3.2015.
 */
public class BluetoothBase {

    public interface  BluetoothStatusChanged{
        void Connected(BluetoothSocket socket);
        void GotConnection(BluetoothSocket socket);
        void ConnectionFailed(String reason);
        void ListeningFailed(String reason);
        void BluetoothStateChanged(int state);
        void HandShakeOk(BluetoothSocket socket, boolean incoming);
        void HandShakeFailed(String reason, boolean incoming);
    }

    private BluetoothStatusChanged callBack = null;
    private BluetoothAdapter bluetooth = null;

    private BtBrowdCastReceiver receiver = null;
    private final Context context;

    public BluetoothBase(Context Context, BluetoothStatusChanged handler) {
        this.context = Context;
        this.callBack = handler;

        //bluetooth = new BluetoothAdapter(this);
        bluetooth = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean Start() {

        boolean ret = false;
        if(bluetooth != null) {
            ret = true;
            Log.d("", "My BT: " + bluetooth.getAddress() + " : " + bluetooth.getName() + " , state: " + bluetooth.getState());

            receiver = new BtBrowdCastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            this.context.registerReceiver(receiver, filter);
        }
        return ret;
    }

    public void Stop() {
        this.context.unregisterReceiver(receiver);
    }

    public void SetBluetoothEnabled(boolean seton) {
        if (bluetooth != null) {
            if (seton) {
                bluetooth.enable();
            } else {
                bluetooth.disable();
            }
        }
    }

    public boolean isBluetoothEnabled() {
        return bluetooth != null && bluetooth.isEnabled();
    }

    public BluetoothAdapter getAdapter(){
        return bluetooth;
    }

    public String getAddress() {
        String ret = "";
        if (bluetooth != null){
            ret = bluetooth.getAddress();
        }
        return ret;
    }

    public String getName() {
        String ret = "";
        if (bluetooth != null){
            ret = bluetooth.getName();
        }
        return ret;
    }

    public BluetoothDevice getRemoteDevice(String address) {
        BluetoothDevice device = null;
        if (bluetooth != null){
            device = bluetooth.getRemoteDevice(address);
        }
        return device;
    }

    private class BtBrowdCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                if (callBack != null) {
                    callBack.BluetoothStateChanged(mode);
                }
            }
        }
    }
}
