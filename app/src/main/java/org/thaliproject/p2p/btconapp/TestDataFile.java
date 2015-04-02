// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btconapp;

import android.os.Build;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Created by juksilve on 6.3.2015.
 */
public class TestDataFile {

    private final String fileNameStart = "BTConApp";
    private final String firstLine= "Os ,Type ,service discovery, Connected ,first data ,GotBigData \n";

    long State1 = 0; // FoundPeers, 0
    long State2 = 0; // Connecting, 0
    long State3 = 0; // Connected , 1
    long State4 = 0; // GotData   , 2
    long State5 = 0; // GoBigtData, 3

    enum TimeForState{
        FoundPeers,
        Connecting,
        Connected,
        GotData,
        GoBigtData
    }


    private File dbgFile;
    private OutputStream dbgFileOs;
    private MainActivity context;

    public TestDataFile(MainActivity Context){
        this.context = Context;
    }

    public void StartNewFile(){
        Time t= new Time();
        t.setToNow();

        File path = this.context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        String sFileName =  "/" + fileNameStart + t.yearDay + t.hour+ t.minute + t.second + ".txt";

        try {
            dbgFile = new File(path, sFileName);
            dbgFileOs = new FileOutputStream(dbgFile);
            dbgFileOs.write(firstLine.getBytes());
            dbgFileOs.flush();

            Log.d(fileNameStart, "File created:" + path + " ,filename : " + sFileName);
        }catch(Exception e){
            Log.d("FILE","FileWriter, create file error, :"  + e.toString() );
        }
    }

    public void CloseFile(){
        try {
            if (dbgFile != null) {
                dbgFileOs.close();
                dbgFile.delete();
            }
        }catch (Exception e){
            Log.d(fileNameStart,"dbgFile close error :"  + e.toString() );
        }
    }

    void SetTimeNow(TimeForState state){
        switch(state){
            case FoundPeers:
                ((TextView) context.findViewById(R.id.statusBox)).setBackgroundColor(0xffffff00);
                State1 = System.currentTimeMillis(); // Connecting
                break;
            case Connecting:
                ((TextView) context.findViewById(R.id.statusBox)).setBackgroundColor(0xffff0000);
                State2 = System.currentTimeMillis(); // Connecting
                break;
            case Connected:
                ((TextView) context.findViewById(R.id.statusBox)).setBackgroundColor(0xff00ff00);
                State3 = System.currentTimeMillis(); // Connected
                break;
            case GotData:
                ((TextView) context.findViewById(R.id.statusBox)).setBackgroundColor(0xff0000ff);
                State4 = System.currentTimeMillis(); // GotData
                break;
            case GoBigtData:
                ((TextView) context.findViewById(R.id.statusBox)).setBackgroundColor(0xffffffff);
                State5 = System.currentTimeMillis(); // GoBigtData
                break;
        }
    }

    public long timeBetween(TimeForState from, TimeForState to){

        long fromTime = 0;
        switch(from){
            case FoundPeers:
                fromTime = State1;
                break;
            case Connecting:
                fromTime = State2;
                break;
            case Connected:
                fromTime = State3;
                break;
            case GotData:
                fromTime = State4;
                break;
            case GoBigtData:
                fromTime = State5;
                break;
        }

        long toTime = 0;
        switch(to){
            case FoundPeers:
                toTime = State1;
                break;
            case Connecting:
                toTime = State2;
                break;
            case Connected:
                toTime = State3;
                break;
            case GotData:
                toTime = State4;
                break;
            case GoBigtData:
                toTime = State5;
                break;
        }

        return (fromTime - toTime);
    }


    public void WriteDebugline(String type) {

        try {
            String dbgData = Build.VERSION.SDK_INT + " ," ;
            dbgData = dbgData  + type + " ,";

            dbgData = dbgData + (State2 - State1) + " ,";
            dbgData = dbgData + (State3 - State2) + " ,";
            dbgData = dbgData + (State4 - State3) + " ,";
            dbgData = dbgData + (State5 - State4) + " ,";

            Log.d("FILE", "write: " + dbgData);
            dbgFileOs.write(dbgData.getBytes());
            dbgFileOs.flush();

            State1 = 0;
            State2 = 0;
            State3 = 0;
            State4 = 0;
            State5 = 0;

        }catch(Exception e){
            Log.d("FILE", "dbgFile write error :" + e.toString());
        }
    }
}
