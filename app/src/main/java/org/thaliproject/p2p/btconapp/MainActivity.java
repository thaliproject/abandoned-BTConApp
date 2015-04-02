// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btconapp;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
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

import java.util.Random;


public class MainActivity extends ActionBarActivity implements BTConnector.Callback {

    MainActivity that = this;

    MyTextSpeech mySpeech = null;

    int sendMessageCounter = 0;
    int gotMessageCounter = 0;
    int ConAttemptCounter = 0;
    int ConnectionCounter = 0;
    int ConCancelCounter = 0;

    boolean iWasBigSender = false;
    boolean amIBigSender = false;
    boolean gotFirstMessage = false;
    boolean wroteFirstMessage = false;
    long wroteDataAmount = 0;
    long gotDataAmount = 0;

    TestDataFile mTestDataFile = null;

    private int mInterval = 1000; // 1 second by default, can be changed later
    private Handler timeHandler;
    private int timeCounter = 0;
    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            // call function to update timer
            timeCounter = timeCounter + 1;
            ((TextView) findViewById(R.id.TimeBox)).setText("T: " + timeCounter);
            timeHandler.postDelayed(mStatusChecker, mInterval);
        }
    };

    BTConnector mBTConnector = null;

    BTConnectedThread mBTConnectedThread = null;
    PowerManager.WakeLock mWakeLock = null;

    long receivingTimeOutBaseTime = 0;
    CountDownTimer BigBufferReceivingTimeOut = new CountDownTimer(2000, 500) {
        public void onTick(long millisUntilFinished) {
            // not using
        }
        public void onFinish() {

            //if the receiving process has taken more than a minute, lets cancel it
            long receivingNow = (System.currentTimeMillis() - receivingTimeOutBaseTime);
            if(receivingNow > 60000) {
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

        mySpeech = new MyTextSpeech(this);
        mTestDataFile = new TestDataFile(this);

        Button btButton = (Button) findViewById(R.id.appToggle);
        btButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBTConnector != null){
                    mBTConnector.Stop();
                    mBTConnector = null;
                }else{
                    mBTConnector = new BTConnector(that,that);
                    mBTConnector.Start();
                }
            }
        });

        timeHandler = new Handler();
        mStatusChecker.run();

        //for demo
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();

        //create & start connector
        mBTConnector = new BTConnector(this,this);
        mBTConnector.Start();
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

    private void sayHi() {
        if (mBTConnectedThread != null) {
            String message = "Hello from ";
            print_line("CHAT", "sayHi");
            mBTConnectedThread.write(message.getBytes());
        }
    }

    private void sayItWithBigBuffer() {
        if (mBTConnectedThread != null) {
            iWasBigSender = true;
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
                    if (wroteFirstMessage) {
                        timeCounter = 0;
                        wroteDataAmount = wroteDataAmount + msg.arg1;
                        ((TextView) findViewById(R.id.CountBox)).setText("" + wroteDataAmount);
                        if (wroteDataAmount == 1048576) {
                            if (mTestDataFile != null) {
                                sendMessageCounter = sendMessageCounter+ 1;
                                mTestDataFile.SetTimeNow(TestDataFile.TimeForState.GoBigtData);
                                long timeval = mTestDataFile.timeBetween(TestDataFile.TimeForState.GoBigtData, TestDataFile.TimeForState.GotData);

                                final String sayoutloud = "Send megabyte in : " + (timeval / 1000) + " seconds.";

                                if (iWasBigSender) {
                                    mTestDataFile.WriteDebugline("BigSender");
                                } else {
                                    mTestDataFile.WriteDebugline("Receiver");
                                }

                                print_line("CHAT", sayoutloud);
                                mySpeech.speak(sayoutloud);
                            }
                            wroteFirstMessage = false;
                            gotFirstMessage = false;

                            ((TextView) findViewById(R.id.msgSendCount)).setText("" + sendMessageCounter);
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
                    if (gotFirstMessage) {
                        gotDataAmount = gotDataAmount + msg.arg1;
                        timeCounter = 0;
                        ((TextView) findViewById(R.id.CountBox)).setText("" + gotDataAmount);
                        BigBufferReceivingTimeOut.cancel();
                        BigBufferReceivingTimeOut.start();
                        if (gotDataAmount == 1048576) {
                            BigBufferReceivingTimeOut.cancel();

                            if (mTestDataFile != null) {
                                gotMessageCounter = gotMessageCounter+ 1;
                                mTestDataFile.SetTimeNow(TestDataFile.TimeForState.GoBigtData);

                                long timeval = mTestDataFile.timeBetween(TestDataFile.TimeForState.GoBigtData, TestDataFile.TimeForState.GotData);

                                final String sayoutloud = "Got megabyte in : " + (timeval / 1000) + " seconds.";

                                if (iWasBigSender) {
                                    mTestDataFile.WriteDebugline("BigSender");
                                } else {
                                    mTestDataFile.WriteDebugline("Receiver");
                                }

                                print_line("CHAT", sayoutloud);
                                mySpeech.speak(sayoutloud);
                            }

                            iWasBigSender = false;
                            gotFirstMessage = false;

                            ((TextView) findViewById(R.id.msgGotCount)).setText("" + gotMessageCounter);

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

                        }
                    } else {
                        byte[] readBuf = (byte[]) msg.obj;// construct a string from the valid bytes in the buffer
                        String readMessage = new String(readBuf, 0, msg.arg1);
                        if (mTestDataFile != null) {
                            mTestDataFile.SetTimeNow(TestDataFile.TimeForState.GotData);
                        }
                        receivingTimeOutBaseTime = System.currentTimeMillis();
                        gotFirstMessage = true;
                        gotDataAmount = 0;
                        print_line("CHAT", "Got message: " + readMessage);
                        if (amIBigSender) {
                            amIBigSender = false;
                            sayItWithBigBuffer();
                        } else {
                            sayHi();
                        }
                    }
                    break;
                case BTConnectedThread.SOCKET_DISCONNEDTED: {
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

        sayHi();
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
                if(mTestDataFile != null) {
                    mTestDataFile.SetTimeNow(TestDataFile.TimeForState.FoundPeers);
                }
                break;
            case Connecting: {
                    ConAttemptCounter = ConAttemptCounter + 1;
                    ((TextView) findViewById(R.id.conaCount)).setText("" + ConAttemptCounter);
                    if (mTestDataFile != null) {
                        mTestDataFile.SetTimeNow(TestDataFile.TimeForState.Connecting);
                    }
                }
                break;
            case Connected: {
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
}


