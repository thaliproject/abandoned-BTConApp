// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btconnectorlib;

import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by juksilve on 28.2.2015.
 */
public class WifiServiceAdvertiser {

    private WifiP2pManager p2p;
    private WifiP2pManager.Channel channel;

    int lastError = -1;
    public WifiServiceAdvertiser(WifiP2pManager Manager, WifiP2pManager.Channel Channel) {
        this.p2p = Manager;
        this.channel = Channel;
    }

    public int GetLastError(){
        return lastError;
    }
    public void Start(String instance) {

        Map<String, String> record = new HashMap<String, String>();
        record.put("available", "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(instance, WifiBase.SERVICE_TYPE, record);

        debug_print("Add local service :" + instance);
        p2p.addLocalService(channel, service, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                lastError = -1;
                debug_print("Added local service");
            }

            public void onFailure(int reason) {
                lastError = reason;
                debug_print("Adding local service failed, error code " + reason);
            }
        });
    }

    public void Stop() {
        p2p.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                lastError = -1;
                debug_print("Cleared local services");
            }

            public void onFailure(int reason) {
                lastError = reason;
                debug_print("Clearing local services failed, error code " + reason);
            }
        });
    }

    private void debug_print(String buffer) {
        Log.i("ACCESS point",buffer);
    }
}
