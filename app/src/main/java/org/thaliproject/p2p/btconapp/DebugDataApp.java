package org.thaliproject.p2p.btconapp;

import android.app.Application;

/**
 * Created by juksilve on 2.4.2015.
 */
public class DebugDataApp extends Application {

    public class DebugSummary {
        public long State1Count = 0; // FoundPeers, 0
        public long State2Count = 0; // Connecting, 0
        public long State3Count = 0; // Connected , 1
        public long State4Count = 0; // GotData   , 2
        public long State5Count = 0; // GoBigtData, 3


        public long State2Max = -1; // Connecting, 0
        public long State3Max = -1; // Connected , 1
        public long State4Max = -1; // GotData   , 2
        public long State5Max = -1; // GoBigtData, 3

        public long State2Min = 0xFFFFFFFF; // Connecting, 0
        public long State3Min = 0xFFFFFFFF; // Connected , 1
        public long State4Min = 0xFFFFFFFF; // GotData   , 2
        public long State5Min = 0xFFFFFFFF; // GoBigtData, 3

        public long State2Avg = -1; // Connecting, 0
        public long State3Avg = -1; // Connected , 1
        public long State4Avg = -1; // GotData   , 2
        public long State5Avg = -1; // GoBigtData, 3
    }

    DebugSummary mDebugSummary = new DebugSummary();

    public DebugSummary getSummary(){
        return mDebugSummary;
    }

    public void setSummary(DebugSummary summary){
        mDebugSummary = summary;
    }

    public void resetSummary(){
        mDebugSummary = new DebugSummary();
    }
}
