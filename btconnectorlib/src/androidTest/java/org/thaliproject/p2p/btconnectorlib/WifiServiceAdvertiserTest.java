package org.thaliproject.p2p.btconnectorlib;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.test.InstrumentationTestCase;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by juksilve on 1.4.2015.
 * Test class for testing main funcitonality of the WifiServiceAdvertiser
 */
public class WifiServiceAdvertiserTest extends InstrumentationTestCase {

    WifiBase wBase = null;
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        WifiManager wifiManager = (WifiManager) getInstrumentation().getContext().getSystemService(Context.WIFI_SERVICE);
        assertNotNull(wifiManager);
        assertTrue("Wifi is off. Turn Wifi on before testing", wifiManager.isWifiEnabled());
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        if(wBase != null) {
            wBase.Stop();
            wBase = null;
        }
    }

    /**
     * Test that the adding local service & removing it is successfull
     */
    public final void testWifiOnOff() throws InterruptedException {

        wBase = new WifiBase(getInstrumentation().getContext(), new WifiBase.WifiStatusCallBack(){
            @Override
            public void WifiStateChanged(int state) {}
            @Override
            public void gotPeersList(Collection<WifiP2pDevice> list) {}
            @Override
            public void gotServicesList(List<ServiceItem> list) {}
        });
        assertNotNull(wBase);

        wBase.Start();
        WifiP2pManager.Channel channel = wBase.GetWifiChannel();
        WifiP2pManager p2p = wBase.GetWifiP2pManager();

        WifiServiceAdvertiser advert = new WifiServiceAdvertiser(p2p,channel);

        advert.Start("Humppaa");
        assertEquals(advert.GetLastError(),-1);

        advert.Stop();
        assertEquals(advert.GetLastError(),-1);
    }

}
