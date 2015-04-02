package org.thaliproject.p2p.btconnectorlib;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.test.InstrumentationTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by juksilve on 1.4.2015.
 * Test class for testing main functionality of the WifiBase class
 */
public class WifiBaseTest extends InstrumentationTestCase {

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
     * Test wifi setting on / off, and that we get events for it
     */
    public final void testWifiOnOff() throws InterruptedException {

        final CountDownLatch wifiOnLatch = new CountDownLatch(1);
        final CountDownLatch wifiOffLatch = new CountDownLatch(1);

        wBase = new WifiBase(getInstrumentation().getContext(), new WifiBase.WifiStatusCallBack(){

            @Override
            public void WifiStateChanged(int state) {
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                    wifiOnLatch.countDown();
                }else{
                    wifiOffLatch.countDown();
                }
            }

            @Override
            public void gotPeersList(Collection<WifiP2pDevice> list) {

            }

            @Override
            public void gotServicesList(List<ServiceItem> list) {

            }
        });
        assertNotNull(wBase);

        boolean startOk = wBase.Start();
        assertTrue("This device does not support Wi-Fi Direct", startOk);

        WifiP2pManager.Channel channel = wBase.GetWifiChannel();
        assertNotNull(channel);
        WifiP2pManager p2p = wBase.GetWifiP2pManager();
        assertNotNull(p2p);

        wBase.setWifiEnabled(false);
        boolean await = wifiOffLatch.await(30, TimeUnit.SECONDS);
        assertTrue(await);

        wBase.setWifiEnabled(true);
        boolean awaitAgain = wifiOnLatch.await(30, TimeUnit.SECONDS);
        assertTrue(awaitAgain);
    }

    /**
     * Test that our Scheduler for discovered Services is working right
     * It should select new-Devices (ones we have not connected yet) if any is available,
     * and if there is no new devices available, then is selects the one we had connection
     * before, but which one haven't got connection established the longest time
     */
    public final void testServiceItemSelection() {

        WifiBase wwBase = new WifiBase(getInstrumentation().getContext(), null);
        assertNotNull(wwBase);

        List<ServiceItem> myServiceList = new ArrayList<ServiceItem>();
        ServiceItem First = new ServiceItem("First", "my type", "0000111100001111","First");
        myServiceList.add(First);
        ServiceItem Second = new ServiceItem("Second","my type", "2222111122221111","Second");
        myServiceList.add(Second);
        ServiceItem Third = new ServiceItem("Third", "my type", "2222333322223333","Third");
        myServiceList.add(Third);
        ServiceItem Fourth = new ServiceItem("Fourth","my type", "4444333344443333","Fourth");
        myServiceList.add(Fourth);

        ServiceItem sel1 = wwBase.SelectServiceToConnect(myServiceList);
        assertEquals(sel1.deviceName + " != " + First.deviceName,sel1.deviceAddress,First.deviceAddress);

        ServiceItem sel2 = wwBase.SelectServiceToConnect(myServiceList);
        assertEquals(sel2.deviceName + " != " + Second.deviceName,sel2.deviceAddress,Second.deviceAddress);

        ServiceItem sel3 = wwBase.SelectServiceToConnect(myServiceList);
        assertEquals(sel3.deviceName + " != " + Third.deviceName,sel3.deviceAddress,Third.deviceAddress);

        ServiceItem sel4 = wwBase.SelectServiceToConnect(myServiceList);
        assertEquals(sel4.deviceName + " != " + Fourth.deviceName,sel4.deviceAddress,Fourth.deviceAddress);

        ServiceItem sel5 = wwBase.SelectServiceToConnect(myServiceList);
        assertEquals(sel5.deviceName + " != " + First.deviceName,sel5.deviceAddress,First.deviceAddress);

        ServiceItem newOne = new ServiceItem("newOne", "my type", "2222555522225555","newOne");
        myServiceList.add(newOne);

        ServiceItem sel6 = wwBase.SelectServiceToConnect(myServiceList);
        assertEquals(sel6.deviceName + " != " + newOne.deviceName,sel6.deviceAddress,newOne.deviceAddress);
        //assertEquals(sel6.deviceName + " != " + sel5.deviceName,sel6.deviceAddress,sel5.deviceAddress);
    }

    }