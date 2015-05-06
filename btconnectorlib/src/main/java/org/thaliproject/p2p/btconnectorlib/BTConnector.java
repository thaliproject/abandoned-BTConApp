// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btconnectorlib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;

import java.util.Collection;
import java.util.List;

/**
 * Created by juksilve on 13.3.2015.
 */
public class BTConnector implements BluetoothBase.BluetoothStatusChanged, WifiBase.WifiStatusCallBack{

    final BTConnector that = this;

    public enum State{
        Idle,
        NotInitialized,
        WaitingStateChange,
        FindingPeers,
        FindingServices,
        Connecting,
        Connected
    }

    public interface  Callback{
        void Connected(BluetoothSocket socket, boolean incoming);
        void StateChanged(State newState);
    }

    public interface  ConnectSelector{
        ServiceItem SelectServiceToConnect(List<ServiceItem> available);
    }

    private State myState = State.NotInitialized;

    WifiBase mWifiBase = null;
    WifiServiceAdvertiser mWifiAccessPoint = null;
    WifiServiceSearcher mWifiServiceSearcher = null;

    BluetoothBase mBluetoothBase = null;
    BTListenerThread mBTListenerThread = null;
    BTConnectToThread mBTConnectToThread = null;
    BTHandShaker mBTHandShaker = null;

    AESCrypt mAESCrypt = null;

    private Callback callback = null;
    private ConnectSelector connectSelector = null;
    private Context context = null;
    private Handler mHandler = null;

    final CountDownTimer ServiceFoundTimeOutTimer = new CountDownTimer(600000, 1000) {
        public void onTick(long millisUntilFinished) {
            // not using
        }
        public void onFinish() {
            startAll();
        }
    };


    BTConnectorSettings ConSettings = new BTConnectorSettings();


    public BTConnector(Context Context, Callback Callback, ConnectSelector selector, BTConnectorSettings settings,
                       String InstancePassword){
        this.context = Context;
        this.callback = Callback;
        this.mHandler = new Handler(this.context.getMainLooper());
        this.ConSettings = settings;
        this.connectSelector = selector;

        try{
            mAESCrypt = new AESCrypt(InstancePassword);
        }catch(Exception e){
            print_line("", "mAESCrypt instance creation failed: " + e.toString());
            mAESCrypt = null;
        }
    }

    public void Start() {
        //initialize the system, and
        // make sure BT & Wifi are enabled before we start running
        if(mBluetoothBase != null){
            mBluetoothBase.Stop();
            mBluetoothBase = null;
        }
        mBluetoothBase = new BluetoothBase(this.context, this);
        Boolean btOk = mBluetoothBase.Start();

        if(mWifiBase != null){
            mWifiBase.Stop();
            mWifiBase = null;
        }
        mWifiBase = new WifiBase(this.context, this);
        Boolean WifiOk = mWifiBase.Start();

        if (!WifiOk || !btOk) {
            print_line("", "BT available: " + btOk + ", wifi available: " + WifiOk);
            setState(State.NotInitialized);
        } else if (mBluetoothBase.isBluetoothEnabled() && mWifiBase.isWifiEnabled()) {
             print_line("", "All stuff available and enabled");
             startAll();
        }else{
            //we wait until both Wifi & BT are turned on
            setState(State.WaitingStateChange);
        }
    }

    public void Stop() {
        stopAll();
        if (mWifiBase != null) {
            mWifiBase.Stop();
            mWifiBase = null;
        }

        if (mBluetoothBase != null) {
            mBluetoothBase.Stop();
            mBluetoothBase = null;
        }
    }

    private void startServices() {

        stopServices();
        String advertLine = "";

        if (mBluetoothBase != null) {
            if(mAESCrypt != null){
                try {
                    advertLine = mAESCrypt.encrypt(mBluetoothBase.getAddress());
                }catch (Exception e){
                    print_line("", "mAESCrypt.encrypt failed: " + e.toString());
                }
            }
        }

        WifiP2pManager.Channel channel = null;
        WifiP2pManager p2p = null;
        if (mWifiBase != null) {
            channel = mWifiBase.GetWifiChannel();
            p2p = mWifiBase.GetWifiP2pManager();
        }

        if (channel != null && p2p != null) {
            print_line("", "Starting services address: " + advertLine + " " + ConSettings);

            mWifiAccessPoint = new WifiServiceAdvertiser(p2p, channel);
            mWifiAccessPoint.Start(advertLine,ConSettings.SERVICE_TYPE);

            mWifiServiceSearcher = new WifiServiceSearcher(this.context, p2p, channel, this,ConSettings.SERVICE_TYPE);
            mWifiServiceSearcher.Start();
            setState(State.FindingPeers);
        }
    }

    private  void stopServices() {
        print_line("", "Stoppingservices");
        setState(State.Idle);
        if (mWifiAccessPoint != null) {
            mWifiAccessPoint.Stop();
            mWifiAccessPoint = null;
        }

        if (mWifiServiceSearcher != null) {
            mWifiServiceSearcher.Stop();
            mWifiServiceSearcher = null;
        }
    }

    private  void startBluetooth() {

        BluetoothAdapter tmp = null;
        if (mBluetoothBase != null) {
            tmp = mBluetoothBase.getAdapter();
        }

        if (mBTListenerThread == null && tmp != null) {
            print_line("", "StartBluetooth listener");
            mBTListenerThread = new BTListenerThread(that, tmp,ConSettings);
            mBTListenerThread.start();
        }
    }

    private  void stopBluetooth() {
        print_line("", "Stop Bluetooth");

        if(mBTHandShaker != null){
            mBTHandShaker.Stop();
            mBTHandShaker = null;
        }

        if (mBTListenerThread != null) {
            mBTListenerThread.Stop();
            mBTListenerThread = null;
        }

        if (mBTConnectToThread != null) {
            mBTConnectToThread.Stop();
            mBTConnectToThread = null;
        }
    }

    private void stopAll() {
        print_line("", "Stoping All");
        ServiceFoundTimeOutTimer.cancel();
        stopServices();
        stopBluetooth();
    }

    private void startAll() {
        stopAll();
        print_line("", "Starting All");
        startServices();
        startBluetooth();
    }

    @Override
    public void Connected(BluetoothSocket socket) {
        //make sure we do not close the socket,
        if(mBTHandShaker == null) {

            final BluetoothSocket tmp = socket;
            mBTConnectToThread = null;
            stopBluetooth();
            stopServices();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBTHandShaker = new BTHandShaker(tmp, that, true);
                    mBTHandShaker.Start();
                }
            });
        }
    }

    @Override
    public void GotConnection(BluetoothSocket socket) {
        if(mBTHandShaker == null) {
            final BluetoothSocket tmp = socket;
            stopBluetooth();
            stopServices();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBTHandShaker = new BTHandShaker(tmp, that, false);
                    mBTHandShaker.Start();
                }
            });
        }
    }

    @Override
    public void HandShakeOk(BluetoothSocket socket, boolean incoming) {
        final BluetoothSocket tmp = socket;
        final boolean incomingTmp = incoming;

        print_line("HS", "HandShakeOk for incoming = " + incoming);


        if(mBTHandShaker != null) {
            mBTHandShaker.Stop();
            mBTHandShaker = null;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (tmp.isConnected()) {
                    setState(State.Connected);
                    that.callback.Connected(tmp, incomingTmp);
                } else {
                    if(incomingTmp) {
                        ListeningFailed("Disconnected");
                    }else{
                        ConnectionFailed("Disconnected");
                    }
                }
            }
        });
    }

    @Override
    public void HandShakeFailed(String reason, boolean incoming) {

        print_line("HS", "HandShakeFailed: " + reason);

        //only care if we have not stoppeed & nulled the instance
        if(mBTHandShaker != null) {
            mBTHandShaker.tryCloseSocket();
            mBTHandShaker.Stop();
            mBTHandShaker = null;

            startServices();
            startBluetooth();
        }
    }

    @Override
    public void ConnectionFailed(String reason) {
        final String tmp = reason;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                print_line("CONNEC", "Error: " + tmp);

                //only care if we have not stoppeed & nulled the instance
                if (mBTConnectToThread != null) {
                    mBTConnectToThread.Stop();
                    mBTConnectToThread = null;

                    startServices();
                }
            }
        });
    }

    @Override
    public void ListeningFailed(String reason) {
        final String tmp = reason;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                print_line("LISTEN", "Error: " + tmp);

                //only care if we have not stoppeed & nulled the instance
                if (mBTListenerThread != null) {
                    mBTListenerThread.Stop();
                    mBTListenerThread = null;

                    startBluetooth();
                }
            }
        });
    }

    @Override
    public void BluetoothStateChanged(int state) {
        if (state == BluetoothAdapter.SCAN_MODE_NONE) {
            print_line("BT", "Bluetooth DISABLED, stopping");
            stopAll();
            // indicate the waiting with state change
            setState(State.WaitingStateChange);
        } else if (state == BluetoothAdapter.SCAN_MODE_CONNECTABLE
                || state == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            if (mWifiBase != null && mWifiBase.isWifiEnabled()) {
                print_line("BT", "Bluetooth enabled, re-starting");
                startAll();
            }
        }
    }



    @Override
    public void WifiStateChanged(int state) {
        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            if (mBluetoothBase != null && mBluetoothBase.isBluetoothEnabled()) {
                // we got wifi back, so we can re-start now
                print_line("WB", "Wifi is now enabled !");
                startAll();
            }
        } else {
            //no wifi availavble, thus we need to stop doing anything;
            print_line("WB", "Wifi is DISABLEd !!");
            stopAll();
            // indicate the waiting with state change
            setState(State.WaitingStateChange);
        }
    }

    @Override
    public void gotPeersList(Collection<WifiP2pDevice> list) {

        ServiceFoundTimeOutTimer.cancel();
        ServiceFoundTimeOutTimer.start();

        print_line("SS", "Found " + list.size() + " peers.");
        int numm = 0;
        for (WifiP2pDevice peer : list) {
            numm++;
            print_line("SS","Peer(" + numm + "): " + peer.deviceName + " " + peer.deviceAddress);
        }

        setState(State.FindingServices);
    }

    @Override
    public void gotServicesList(List<ServiceItem> list) {
        if(mWifiBase != null && list != null && list.size() > 0) {

            ServiceItem selItem = null;

            if(this.connectSelector != null){
                selItem = this.connectSelector.SelectServiceToConnect(list);
            }else
            {
                selItem = mWifiBase.SelectServiceToConnect(list);
            }

            if (selItem != null && mBluetoothBase != null) {

                if (mBTConnectToThread != null) {
                    mBTConnectToThread.Stop();
                    mBTConnectToThread = null;
                }

                if(ServiceFoundTimeOutTimer != null) {
                    ServiceFoundTimeOutTimer.cancel();
                }

                String AddressLine = "";
                if(mAESCrypt != null){
                    try {
                        AddressLine = mAESCrypt.decrypt(selItem.instanceName);
                    }catch (Exception e){
                        print_line("", "mAESCrypt.decrypt failed: " + e.toString());
                    }
                }

                print_line("", "Selected device address: " + AddressLine +  ", from: " + selItem.instanceName);

                BluetoothDevice device = mBluetoothBase.getRemoteDevice(AddressLine);

                mBTConnectToThread = new BTConnectToThread(that, device,ConSettings);
                mBTConnectToThread.start();

                //we have connection, no need to find new ones
                stopServices();
                setState(State.Connecting);
                print_line("", "Connecting to " + device.getName() + ", at " + device.getAddress());
            } else {
                // we'll get discovery stopped event soon enough
                // and it starts the discovery again, so no worries :)
                print_line("", "No devices selected");
            }
        }
    }

    private void setState(final State newState) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                that.callback.StateChanged(newState);
            }
        });
    }

    public void print_line(String who, String line) {
        Log.i("BTConnector" + who, line);
    }
}
