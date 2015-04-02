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
 * Test class for WifiServiceSearcher class
 * NOTE: This would require other device being discoverable, and also advertising the service, so it can be found.
 */
public class WifiServiceSearcherTest extends InstrumentationTestCase {
    WifiBase wBase = null;
    WifiServiceSearcher sSearcher = null;

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

        if(sSearcher != null) {
            sSearcher.Stop();
            sSearcher = null;
        }
        if(wBase != null) {
            wBase.Stop();
            wBase = null;
        }
    }

    /**
     * Test that we find a Peer devices, and then Services.
     */
    public final void testPeerServiceDiscovery() throws InterruptedException {

        final CountDownLatch gotPeersLatch = new CountDownLatch(1);
        final CountDownLatch gotServicesLatch = new CountDownLatch(1);

        wBase = new WifiBase(getInstrumentation().getContext(), new WifiBase.WifiStatusCallBack(){
            @Override
            public void WifiStateChanged(int state) {}
            @Override
            public void gotPeersList(Collection<WifiP2pDevice> list) {}
            @Override
            public void gotServicesList(List<ServiceItem> list) {}
        });

        wBase.Start();
        WifiP2pManager.Channel channel = wBase.GetWifiChannel();
        WifiP2pManager p2p = wBase.GetWifiP2pManager();

        sSearcher = new WifiServiceSearcher(getInstrumentation().getContext(),p2p,channel, new WifiBase.WifiStatusCallBack(){
            @Override
            public void WifiStateChanged(int state) {}
            @Override
            public void gotPeersList(Collection<WifiP2pDevice> list) {
                gotPeersLatch.countDown();
            }
            @Override
            public void gotServicesList(List<ServiceItem> list) {
                gotServicesLatch.countDown();
            }
        });

        sSearcher.Start();

        boolean await = gotPeersLatch.await(30, TimeUnit.SECONDS);
        assertTrue(await);

        boolean awaitAgain = gotServicesLatch.await(60, TimeUnit.SECONDS);
        assertTrue(awaitAgain);
    }

}
