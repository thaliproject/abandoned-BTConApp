package org.thaliproject.p2p.btconapp;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;

/**
 * Created by juksilve on 2.4.2015.
 */
public class DebugSummaryActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debugsummary);

        DebugDataApp dda = (DebugDataApp)getApplicationContext();
        DebugDataApp.DebugSummary mDebugSummary = dda.getSummary();

        TextView txtView1 =((TextView) findViewById(R.id.PeersFoundCount));
        if(txtView1 != null) {
            txtView1.setText("" + mDebugSummary.State1Count);
            if (mDebugSummary.State1Count > 0) {
                txtView1.setBackgroundColor(0xff00ff00);
            } else {
                txtView1.setBackgroundColor(0xffff0000);
            }
        }

        TextView txtView2 =((TextView) findViewById(R.id.AttemptedCount));
        if(txtView2 != null) {
            txtView2.setText("" + mDebugSummary.State2Count);
            if (mDebugSummary.State2Count > 0
            ||  mDebugSummary.State3Count > 0) { // its also ok, if we got connections from others !
                txtView2.setBackgroundColor(0xff00ff00);
            } else {
                txtView2.setBackgroundColor(0xffff0000);
            }
        }

        TextView txtView3 =((TextView) findViewById(R.id.ConnectedCount));
        if(txtView3 != null) {
            txtView3.setText("" + mDebugSummary.State3Count);
            if (mDebugSummary.State3Count > 0) {
                txtView3.setBackgroundColor(0xff00ff00);
            } else {
                txtView3.setBackgroundColor(0xffff0000);
            }
        }

        TextView txtView4 =((TextView) findViewById(R.id.GotDataCount));
        if(txtView4 != null) {
            txtView4.setText("" + mDebugSummary.State4Count);
            if (mDebugSummary.State4Count > 0) {
                txtView4.setBackgroundColor(0xff00ff00);
            } else {
                txtView4.setBackgroundColor(0xffff0000);
            }
        }

        TextView txtView5 =((TextView) findViewById(R.id.GotBigDataCount));
        if(txtView5 != null) {
            txtView5.setText("" + mDebugSummary.State5Count);
            if (mDebugSummary.State5Count > 0) {
                txtView5.setBackgroundColor(0xff00ff00);
            } else {
                txtView5.setBackgroundColor(0xffff0000);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DebugDataApp dda = (DebugDataApp)getApplicationContext();
        dda.resetSummary();
    }
}
