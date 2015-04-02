// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btconnectorlib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by juksilve on 6.3.2015.
 */
public class WifiBase implements WifiP2pManager.ChannelListener {

    public interface  WifiStatusCallBack{
        public void WifiStateChanged(int state);
        public void gotPeersList(Collection<WifiP2pDevice> list);
        public void gotServicesList(List<ServiceItem> list);
    }

    public static final String SERVICE_TYPE = "_BTCL_p2p._tcp";

    private List<ServiceItem> connectedArray = new ArrayList<ServiceItem>();
    private WifiP2pManager p2p = null;
    private WifiP2pManager.Channel channel = null;
    private Context context;

    WifiStatusCallBack callback;
    MainBCReceiver mBRReceiver;
    private IntentFilter filter;

    public WifiBase(Context Context, WifiStatusCallBack handler){
        this.context = Context;
        this.callback = handler;
    }

    public boolean Start(){

        boolean ret =false;

        mBRReceiver = new MainBCReceiver();
        filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        this.context.registerReceiver((mBRReceiver), filter);

        p2p = (WifiP2pManager) this.context.getSystemService(Context.WIFI_P2P_SERVICE);
        if (p2p == null) {
            Log.d("WifiBase", "This device does not support Wi-Fi Direct");
        } else {
            ret = true;
            channel = p2p.initialize(this.context, this.context.getMainLooper(), this);
        }

        return ret;
    }
    public void Stop(){
        this.context.unregisterReceiver(mBRReceiver);
    }

    public WifiP2pManager.Channel GetWifiChannel(){
        return channel;
    }
    public WifiP2pManager  GetWifiP2pManager(){
        return p2p;
    }

    public boolean isWifiEnabled(){
        WifiManager wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
        if(wifiManager != null) {
            return wifiManager.isWifiEnabled();
        }else{
            return false;
        }
    }

    public boolean setWifiEnabled(boolean enabled){
        WifiManager wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
        if(wifiManager != null) {
            return wifiManager.setWifiEnabled(enabled);
        }else{
            return false;
        }
    }

    @Override
    public void onChannelDisconnected() {
        // we might need to do something in here !
    }

    public ServiceItem SelectServiceToConnect(List<ServiceItem> available){

        ServiceItem  ret = null;

        if(connectedArray.size() > 0 && available.size() > 0) {

            int firstNewMatch = -1;
            int firstOldMatch = -1;

            for (int i = 0; i < available.size(); i++) {
                if(firstNewMatch >= 0) {
                    break;
                }
                for (int ii = 0; ii < connectedArray.size(); ii++) {
                    if (available.get(i).deviceAddress.equals(connectedArray.get(ii).deviceAddress)) {
                        if(firstOldMatch < 0 || firstOldMatch > ii){
                            //find oldest one available that we have connected previously
                            firstOldMatch = ii;
                        }
                        firstNewMatch = -1;
                        break;
                    } else {
                        if (firstNewMatch < 0) {
                            firstNewMatch = i; // select first not connected device
                        }
                    }
                }
            }

            if (firstNewMatch >= 0){
                ret = available.get(firstNewMatch);
            }else if(firstOldMatch >= 0){
                ret = connectedArray.get(firstOldMatch);
                // we move this to last position
                connectedArray.remove(firstOldMatch);
            }

            //print_line("EEE", "firstNewMatch " + firstNewMatch + ", firstOldMatch: " + firstOldMatch);

        }else if(available.size() > 0){
            ret = available.get(0);
        }
        if(ret != null){
            connectedArray.add(ret);

            // just to set upper limit for the amount of remembered contacts
            // when we have 101, we remove the oldest (that's the top one)
            // from the array
            if(connectedArray.size() > 100){
                connectedArray.remove(0);
            }
        }

        return ret;
    }

    private void debug_print(String buffer) {
        Log.i("Service searcher", buffer);
    }

    private class MainBCReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if(callback != null) {
                    callback.WifiStateChanged(state);
                }
            }
        }
    }
}
