// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btconapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.thaliproject.p2p.btconnectorlib.BTConnector;
import org.thaliproject.p2p.btconnectorlib.BTConnectorSettings;
import org.thaliproject.p2p.btconnectorlib.ServiceItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;


public class MainActivity extends ActionBarActivity implements BTConnector.Callback, BTConnector.ConnectSelector {

    final MainActivity that = this;

    /*
        For End-to-End testing we can use timer here to Stop the process
        for example after 1 minute.
        The value is determined by mExitWithDelay
        */

    private int mExitWithDelay = 120; // 60 seconds test before exiting
    private boolean mExitWithDelayIsOn = true; // set false if we are not uisng this app for testing

    final String instanceEncryptionPWD = "CHANGEYOURPASSWORDHERE";
 //   final String serviceTypeIdentifier = "_BTCL_p2p._tcp";
    final String BtUUID                = "fa87c0d0-afac-11de-8a39-0800200c9a66";
    final String Bt_NAME               = "Thaili_Bluetooth";

    //todo remove after tests
    final String serviceTypeIdentifier = "_HUMPPAA._tcp";


    BTConnectorSettings conSettings;
    private final List<ServiceItem> connectedArray = new ArrayList<>();

    MyTextSpeech mySpeech = null;

    int sendMessageCounter = 0;
    int gotMessageCounter = 0;
    int ConAttemptCounter = 0;
    int ConnectionCounter = 0;
    int ConCancelCounter = 0;


    boolean amIBigSender = false;
    boolean gotFirstMessage = false;
    boolean wroteFirstMessage = false;
    long wroteDataAmount = 0;
    long gotDataAmount = 0;

    long startTime = 0;

    TestDataFile mTestDataFile = null;

    private final int mInterval = 1000; // 1 second by default, can be changed later
    private Handler timeHandler;
    private int timeCounter = 0;
    final Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {

            // call function to update timer
            timeCounter = timeCounter + 1;
            String timeShow = "T: " + timeCounter;

       /*     if(mExitWithDelayIsOn) {
                //exit timer for testing
                if (mExitWithDelay > 0) {
                    mExitWithDelay = mExitWithDelay - 1;
                    timeShow = timeShow + ", S: " + mExitWithDelay;
                } else {
                    if(mBTConnector != null) {
                        mBTConnector.Stop();
                        mBTConnector = null;
                    }
                    mExitWithDelayIsOn = false;
                    ShowSummary();
                }
            }*/

            long runTimeSec = ((System.currentTimeMillis() - startTime) / 1000);

            timeShow = timeShow + ", run: ";
            long hours = (runTimeSec / 3600);
            timeShow = timeShow + hours + " h, ";
            runTimeSec = (runTimeSec - (hours * 3600));

            long minutes = (runTimeSec / 60);
            timeShow = timeShow + minutes + " m, ";
            runTimeSec = (runTimeSec - (minutes * 60));

            timeShow = timeShow + runTimeSec + " s.";

            ((TextView) findViewById(R.id.TimeBox)).setText(timeShow);
            timeHandler.postDelayed(mStatusChecker, mInterval);
        }
    };

    BTConnector mBTConnector = null;

    BTConnectedThread mBTConnectedThread = null;
    PowerManager.WakeLock mWakeLock = null;

    long receivingTimeOutBaseTime = 0;
    final CountDownTimer BigBufferReceivingTimeOut = new CountDownTimer(2000, 500) {
        public void onTick(long millisUntilFinished) {
            // not using
        }
        public void onFinish() {

            //if the receiving process has taken more than a minute, lets cancel it
            long receivingNow = (System.currentTimeMillis() - receivingTimeOutBaseTime);
            if(receivingNow > 30000) {
                if (mBTConnectedThread != null) {
                    mBTConnectedThread.Stop();
                    mBTConnectedThread = null;
                }
                print_line("CHAT", "WE got timeout on receiving data, lets Disconnect.");

                ConCancelCounter = ConCancelCounter + 1;
                ((TextView) findViewById(R.id.cancelCount)).setText("" + ConCancelCounter);
            }else{
                BigBufferReceivingTimeOut.start();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startTime = System.currentTimeMillis();

        conSettings = new BTConnectorSettings();
        conSettings.SERVICE_TYPE = serviceTypeIdentifier;
        conSettings.MY_UUID = UUID.fromString(BtUUID);
        conSettings.MY_NAME = Bt_NAME;

        mySpeech = new MyTextSpeech(this);
        mTestDataFile = new TestDataFile(this);
        mTestDataFile.StartNewFile();

        Button btButton = (Button) findViewById(R.id.appToggle);
        btButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExitWithDelayIsOn = false;
                print_line("Debug","Exit with delay is set OFF");
                if(mBTConnector != null){
                    mBTConnector.Stop();
                    mBTConnector = null;
                    ShowSummary();
                }else{
                    mBTConnector = new BTConnector(that,that,that,conSettings,instanceEncryptionPWD);
                    mBTConnector.Start();
                }
            }
        });

        timeHandler = new Handler();
        mStatusChecker.run();

        //for demo & testing to keep lights on
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();

        // would need to make sure here that BT & Wifi both are on !!
        WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        if(!bluetooth.isEnabled()){
            bluetooth.enable();
        }
        //create & start connector
        mBTConnector = new BTConnector(this,this,this,conSettings,instanceEncryptionPWD);
        mBTConnector.Start();
    }

    public void ShowSummary(){

        if(mTestDataFile != null){
            Intent intent = new Intent(getBaseContext(), DebugSummaryActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onDestroy() {
        this.mWakeLock.release();
        super.onDestroy();
        BigBufferReceivingTimeOut.cancel();

        timeHandler.removeCallbacks(mStatusChecker);

        //delete connector
        if(mBTConnector != null) {
            mBTConnector.Start();
            mBTConnector = null;
        }

        mTestDataFile.CloseFile();
        mTestDataFile = null;
    }


    private void SayAck(long gotBytes) {
        if (mBTConnectedThread != null) {
            String message = "Got bytes:" + gotBytes;
            print_line("CHAT", "SayAck: " + message);
            mBTConnectedThread.write(message.getBytes());
        }
    }

    private void sayHi() {
        if (mBTConnectedThread != null) {
            String message = "Hello from ";
            print_line("CHAT", "sayHi");
            mBTConnectedThread.write(message.getBytes());
        }
    }

    private void sayItWithBigBuffer() {
        if (mBTConnectedThread != null) {
            byte[] buffer = new byte[1048576]; //Megabyte buffer
            new Random().nextBytes(buffer);
            print_line("CHAT", "sayItWithBigBuffer");
            mBTConnectedThread.write(buffer);
        }
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BTConnectedThread.MESSAGE_WRITE:
                    if (amIBigSender) {
                        timeCounter = 0;
                        wroteDataAmount = wroteDataAmount + msg.arg1;
                        ((TextView) findViewById(R.id.CountBox)).setText("" + wroteDataAmount);
                        if (wroteDataAmount == 1048576) {
                            if (mTestDataFile != null) {
                                // lets do saving after we got ack received
                                //sendMessageCounter = sendMessageCounter+ 1;
                                //((TextView) findViewById(R.id.msgSendCount)).setText("" + sendMessageCounter);
                                mTestDataFile.SetTimeNow(TestDataFile.TimeForState.GoBigtData);
                                long timeval = mTestDataFile.timeBetween(TestDataFile.TimeForState.GoBigtData, TestDataFile.TimeForState.GotData);

                                final String sayoutloud = "Send megabyte in : " + (timeval / 1000) + " seconds.";

                                // lets do saving after we got ack received
                                //mTestDataFile.WriteDebugline("BigSender");

                                print_line("CHAT", sayoutloud);
                                mySpeech.speak(sayoutloud);
                            }
                        }
                    } else {
                        byte[] writeBuf = (byte[]) msg.obj;// construct a string from the buffer
                        String writeMessage = new String(writeBuf);
                        if (mTestDataFile != null) {
                            mTestDataFile.SetTimeNow(TestDataFile.TimeForState.GotData);
                        }

                        wroteDataAmount = 0;
                        wroteFirstMessage = true;
                        print_line("CHAT", "Wrote: " + writeMessage);
                    }
                    break;
                case BTConnectedThread.MESSAGE_READ:
                    if (!amIBigSender) {
                        gotDataAmount = gotDataAmount + msg.arg1;
                        timeCounter = 0;
                        ((TextView) findViewById(R.id.CountBox)).setText("" + gotDataAmount);
                        BigBufferReceivingTimeOut.cancel();
                        BigBufferReceivingTimeOut.start();
                        if (gotDataAmount == 1048576) {
                            BigBufferReceivingTimeOut.cancel();

                            gotFirstMessage = false;
                            gotMessageCounter = gotMessageCounter+ 1;
                            ((TextView) findViewById(R.id.msgGotCount)).setText("" + gotMessageCounter);

                            if (mTestDataFile != null) {
                                mTestDataFile.SetTimeNow(TestDataFile.TimeForState.GoBigtData);

                                long timeval = mTestDataFile.timeBetween(TestDataFile.TimeForState.GoBigtData, TestDataFile.TimeForState.GotData);
                                final String sayoutloud = "Got megabyte in : " + (timeval / 1000) + " seconds.";

                                mTestDataFile.WriteDebugline("Receiver");

                                print_line("CHAT", sayoutloud);
                                mySpeech.speak(sayoutloud);
                            }

                            //got message
                            ((TextView) findViewById(R.id.dataStatusBox)).setBackgroundColor(0xff00ff00); // green
                            SayAck(gotDataAmount);
                        }
                    } else if(gotFirstMessage) {
                        print_line("CHAT", "we got Ack message back, so lets disconnect.");

                        //got message
                        ((TextView) findViewById(R.id.dataStatusBox)).setBackgroundColor(0xff00ff00); // green

                        sendMessageCounter = sendMessageCounter+ 1;
                        ((TextView) findViewById(R.id.msgSendCount)).setText("" + sendMessageCounter);
                        if (mTestDataFile != null) {
                            mTestDataFile.WriteDebugline("BigSender");
                        }
                        // we got Ack message back, so lets disconnect
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            //There are supposedly a possible race-condition bug with the service discovery
                            // thus to avoid it, we are delaying the service discovery start here
                            public void run() {

                                if(mBTConnectedThread != null){
                                    mBTConnectedThread.Stop();
                                    mBTConnectedThread = null;
                                }
                                //Re-start the loop
                                if(mBTConnector != null) {
                                    mBTConnector.Start();
                                }
                            }
                        }, 1000);
                    }else{
                        byte[] readBuf = (byte[]) msg.obj;// construct a string from the valid bytes in the buffer
                        String readMessage = new String(readBuf, 0, msg.arg1);
                        if (mTestDataFile != null) {
                            mTestDataFile.SetTimeNow(TestDataFile.TimeForState.GotData);
                        }

                        gotFirstMessage = true;
                        print_line("CHAT", "Got message: " + readMessage);
                        if (amIBigSender) {
                            ((TextView) findViewById(R.id.dataStatusBox)).setBackgroundColor(0xff0000ff); //Blue
                            sayItWithBigBuffer();
                        }
                    }
                    break;
                case BTConnectedThread.SOCKET_DISCONNEDTED: {

                    ((TextView) findViewById(R.id.dataStatusBox)).setBackgroundColor(0xffcccccc); //light Gray

                    if (mBTConnectedThread != null) {
                        mBTConnectedThread.Stop();
                        mBTConnectedThread = null;
                    }
                    print_line("CHAT", "WE are Disconnected now.");
                    //Re-start the loop
                    if(mBTConnector != null) {
                        mBTConnector.Start();
                    }
                }
                break;
            }
        }
    };

    public void startChat(BluetoothSocket socket, boolean incoming) {
        // with this sample we only have one connection at any time
        // thus lets delete the previous if we had any
        if (mBTConnectedThread != null) {
            mBTConnectedThread.Stop();
            mBTConnectedThread = null;
        }

        if(socket != null && socket.getRemoteDevice() != null) {
            ((TextView) findViewById(R.id.remoteHost)).setText("Last RH: " + socket.getRemoteDevice().getName());
            mySpeech.speak("Connected to " + socket.getRemoteDevice().getName());
        }
        amIBigSender = incoming;
        gotFirstMessage = false;
        wroteFirstMessage = false;
        wroteDataAmount = 0;
        gotDataAmount = 0;

        mBTConnectedThread = new BTConnectedThread(socket,mHandler);
        mBTConnectedThread.start();

        if(!amIBigSender) {
            // we'll start the cancel timer in here
            receivingTimeOutBaseTime = System.currentTimeMillis();
            // will be waiting for big buffer
            ((TextView) findViewById(R.id.dataStatusBox)).setBackgroundColor(0xff0000ff); //Blue
            sayHi();
        }
    }

    public void print_line(String who, String line) {
        Log.i("BtTestMaa" + who, line);
        timeCounter = 0;
    }

    @Override
    public void Connected(BluetoothSocket socket, boolean incoming) {
        startChat(socket,incoming);
        //At this point the BTConnector is not doing anything additional
        //if we want it to continue, we would need to start it again here
        // with this example we start it after we have done communications
    }

    @Override
    public void StateChanged(BTConnector.State newState) {

        ((TextView) findViewById(R.id.statusBox)).setText("State : " + newState);
        switch(newState){
            case Idle:
                ((TextView) findViewById(R.id.statusBox)).setBackgroundColor(0xff444444); //dark Gray
                break;
            case NotInitialized:
                ((TextView) findViewById(R.id.statusBox)).setBackgroundColor(0xffcccccc); //light Gray
                break;
            case WaitingStateChange:
                ((TextView) findViewById(R.id.statusBox)).setBackgroundColor(0xffEE82EE); // pink
                break;
            case FindingPeers:
                ((TextView) findViewById(R.id.statusBox)).setBackgroundColor(0xff00ffff); // Cyan
                break;
            case FindingServices:
                ((TextView) findViewById(R.id.statusBox)).setBackgroundColor(0xffffff00); // yellow
                if(mTestDataFile != null) {
                    mTestDataFile.SetTimeNow(TestDataFile.TimeForState.FoundPeers);
                }
                break;
            case Connecting: {
                    ((TextView) findViewById(R.id.statusBox)).setBackgroundColor(0xffff0000); // red
                    ConAttemptCounter = ConAttemptCounter + 1;
                    ((TextView) findViewById(R.id.conaCount)).setText("" + ConAttemptCounter);
                    if (mTestDataFile != null) {
                        mTestDataFile.SetTimeNow(TestDataFile.TimeForState.Connecting);
                    }
                }
                break;
            case Connected: {
                    ((TextView) findViewById(R.id.statusBox)).setBackgroundColor(0xff00ff00); // green
                    ConnectionCounter = ConnectionCounter + 1;
                    ((TextView) findViewById(R.id.conCount)).setText("" + ConnectionCounter);
                    if (mTestDataFile != null) {
                        mTestDataFile.SetTimeNow(TestDataFile.TimeForState.Connected);
                    }
                }
                break;
        }

        print_line("STATE", "New state: " + newState);
    }

    @Override
    public ServiceItem SelectServiceToConnect(List<ServiceItem> available) {
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
}


