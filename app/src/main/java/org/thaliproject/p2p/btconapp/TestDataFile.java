// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btconapp;

import android.os.Build;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Created by juksilve on 6.3.2015.
 */
public class TestDataFile {

    private final String fileNameStart = "BTCAppTTest";
    private final String firstLine= "Os ,Time ,Type ,service discovery, Connected ,first data ,GotBigData \n";

    long State1 = 0; // FoundPeers, 0
    long State2 = 0; // Connecting, 0
    long State3 = 0; // Connected , 1
    long State4 = 0; // GotData   , 2
    long State5 = 0; // GoBigtData, 3

    long State2tmp = 0; // Connecting, 0
    long State3tmp = 0; // Connected , 1
    long State4tmp = 0; // GotData   , 2
    long State5tmp = 0; // GoBigtData, 3



    enum TimeForState{
        FoundPeers,
        Connecting,
        Connected,
        GotData,
        GoBigtData
    }

    private File dbgFile;
    private OutputStream dbgFileOs;
    private final MainActivity context;

    public TestDataFile(MainActivity Context){
        this.context = Context;
    }


    public void StartNewFile(){
        Time t= new Time();
        t.setToNow();

        // We are using DIRECTORY_DOWNLOADS rather than DIRECTORY_DOCUMENTS because
        // directory document is only supported in API 19 and we want to test on an 18
        // device.
        File path = this.context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);

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

        DebugDataApp dda = (DebugDataApp)this.context.getApplicationContext();
        DebugDataApp.DebugSummary mDebugSummary = dda.getSummary();

        switch(state){
            case FoundPeers:
                State1 = System.currentTimeMillis(); // Connecting
                mDebugSummary.State1Count = mDebugSummary.State1Count + 1;
                break;
            case Connecting:
                State2 = System.currentTimeMillis(); // Connecting
                long dbgtime2 = (State2 - State1);
                if(mDebugSummary.State2Max < dbgtime2){
                    mDebugSummary.State2Max = dbgtime2;
                }
                if(mDebugSummary.State2Min > dbgtime2){
                    mDebugSummary.State2Min = dbgtime2;
                }
                mDebugSummary.State2Count = mDebugSummary.State2Count + 1;

                State2tmp = State2tmp  + dbgtime2;
                mDebugSummary.State2Avg = (State2tmp / mDebugSummary.State2Count);

                break;
            case Connected:
                State3 = System.currentTimeMillis(); // Connected
                long dbgtime3 = (State3 - State2);
                if(mDebugSummary.State3Max < dbgtime3){
                    mDebugSummary.State3Max = dbgtime3;
                }
                if(mDebugSummary.State3Min > dbgtime3){
                    mDebugSummary.State3Min = dbgtime3;
                }
                mDebugSummary.State3Count = mDebugSummary.State3Count + 1;

                State3tmp = State3tmp  + dbgtime3;
                mDebugSummary.State3Avg = (State3tmp / mDebugSummary.State3Count);
                break;
            case GotData:
                State4 = System.currentTimeMillis(); // GotData

                long dbgtime4 = (State4 - State3);
                if(mDebugSummary.State4Max < dbgtime4){
                    mDebugSummary.State4Max = dbgtime4;
                }
                if(mDebugSummary.State4Min > dbgtime4){
                    mDebugSummary.State4Min = dbgtime4;
                }
                mDebugSummary.State4Count = mDebugSummary.State4Count + 1;

                State4tmp = State4tmp  + dbgtime4;
                mDebugSummary.State4Avg = (State4tmp / mDebugSummary.State4Count);
                break;
            case GoBigtData:
                State5 = System.currentTimeMillis(); // GoBigtData

                long dbgtime5 = (State5 - State4);
                if(mDebugSummary.State5Max < dbgtime5){
                    mDebugSummary.State5Max = dbgtime5;
                }
                if(mDebugSummary.State5Min > dbgtime5){
                    mDebugSummary.State5Min = dbgtime5;
                }
                mDebugSummary.State5Count = mDebugSummary.State5Count + 1;

                State5tmp = State5tmp  + dbgtime5;
                mDebugSummary.State5Avg = (State5tmp / mDebugSummary.State5Count);
                break;
        }

        dda.setSummary(mDebugSummary);
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
            dbgData = dbgData  + System.currentTimeMillis()+ " ,";
            dbgData = dbgData  + type + " ,";

            dbgData = dbgData + (State2 - State1) + " ,";
            dbgData = dbgData + (State3 - State2) + " ,";
            dbgData = dbgData + (State4 - State3) + " ,";
            dbgData = dbgData + (State5 - State4) + " \n";

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
