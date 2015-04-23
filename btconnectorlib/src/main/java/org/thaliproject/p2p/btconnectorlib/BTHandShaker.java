// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btconnectorlib;

import android.bluetooth.BluetoothSocket;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by juksilve on 11.3.2015.
 */

public class BTHandShaker {

    private BluetoothSocket mmSocket;
    private BluetoothBase.BluetoothStatusChanged callback;
    private boolean isIncoming;

    String handShakeBuf = "handshake";
    String shakeBackBuf = "shakehand";

    BTHandShakeSocketTread mBTHandShakeSocketTread = null;

    CountDownTimer HandShakeTimeOutTimer = new CountDownTimer(4000, 1000) {
        public void onTick(long millisUntilFinished) {
            // not using
        }
        public void onFinish() {
            callback.HandShakeFailed("TimeOut",isIncoming);
        }
    };

    public BTHandShaker(BluetoothSocket socket, BluetoothBase.BluetoothStatusChanged Callback, boolean incoming) {
        printe_line("Creating BTHandShaker");
        callback = Callback;
        mmSocket = socket;
        isIncoming = incoming;
    }

    public void Start() {
        printe_line("Start");
        HandShakeTimeOutTimer.start();

        mBTHandShakeSocketTread = new BTHandShakeSocketTread(mmSocket,mHandler);
        mBTHandShakeSocketTread.start();

        if(!isIncoming) {
            mBTHandShakeSocketTread.write(handShakeBuf.getBytes());
        }
    }

    public void tryCloseSocket() {
        if(mBTHandShakeSocketTread != null){
            mBTHandShakeSocketTread.CloseSocket();
        }
    }

    public void Stop() {
        printe_line("Stop");
        HandShakeTimeOutTimer.cancel();
        if(mBTHandShakeSocketTread != null){
            mBTHandShakeSocketTread = null;
        }
    }

    private void printe_line(String message){
           Log.d("BTHandShaker",  "BTHandShaker: " + message);
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mBTHandShakeSocketTread != null) {
                switch (msg.what) {
                    case BTHandShakeSocketTread.MESSAGE_WRITE: {
                        printe_line("MESSAGE_WRITE " + msg.arg1 + " bytes.");
                        if (isIncoming) {
                            callback.HandShakeOk(mmSocket, isIncoming);
                        }
                    }
                    break;
                    case BTHandShakeSocketTread.MESSAGE_READ: {
                        printe_line("got MESSAGE_READ " + msg.arg1 + " bytes.");
                        if (isIncoming) {
                            mBTHandShakeSocketTread.write(shakeBackBuf.getBytes());
                        } else {
                            callback.HandShakeOk(mmSocket, isIncoming);
                        }
                    }
                    break;
                    case BTHandShakeSocketTread.SOCKET_DISCONNEDTED: {

                        callback.HandShakeFailed("SOCKET_DISCONNEDTED", isIncoming);
                    }
                    break;
                }
            } else {
                printe_line("handleMessage called for NULL thread handler");
            }
        }
    };
}
