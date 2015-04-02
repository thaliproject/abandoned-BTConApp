package org.thaliproject.p2p.btconnectorlib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.test.InstrumentationTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by juksilve on 1.4.2015.
 * Test class for testing main functionality of the BTConnector class
 */
public class BTConnectorTest extends InstrumentationTestCase {

    BTConnector.State currState = BTConnector.State.NotInitialized;
    BTConnector btConnector = null;
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        WifiManager wifiManager = (WifiManager) getInstrumentation().getContext().getSystemService(Context.WIFI_SERVICE);
        assertNotNull(wifiManager);
        assertTrue("Wifi is off. Turn Wifi on before testing", wifiManager.isWifiEnabled());

        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        assertNotNull(bluetooth);
        assertTrue("Bluetooth is off. Turn Bluetooth on before testing", bluetooth.isEnabled());
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        if(btConnector != null) {
            btConnector.Stop();
            btConnector = null;
        }
    }

    /**
     * Test that we get right state changes for turning Wifi on / off
     */
    public final void testWifiOnOff() throws InterruptedException {

        final CountDownLatch waitZeroLatch = new CountDownLatch(1);
        final CountDownLatch waitOneLatch = new CountDownLatch(1);
        final CountDownLatch waitTwoLatch = new CountDownLatch(1);

        btConnector = new BTConnector(getInstrumentation().getContext(), new BTConnector.Callback() {

            @Override
            public void Connected(BluetoothSocket socket, boolean incoming) {

            }

            @Override
            public void StateChanged(BTConnector.State newState) {
                currState = newState;

                switch(currState){
                    case Idle:
                        break;
                    case NotInitialized:
                        break;
                    case WaitingStateChange:
                        if(waitZeroLatch.getCount() <= 0) {
                            waitOneLatch.countDown();
                        }
                        break;
                    case FindingPeers:
                        if(waitOneLatch.getCount() <= 0){
                            waitTwoLatch.countDown();
                        }else{
                            waitZeroLatch.countDown();
                        }
                        break;
                    case FindingServices:
                        break;
                    case Connecting:
                        break;
                    case Connected:
                        break;
                }
            }
        });

        assertNotNull(btConnector);

        btConnector.Start();
        boolean await0 = waitZeroLatch.await(30, TimeUnit.SECONDS);
        assertTrue(await0);

        WifiManager wifiManager = (WifiManager) getInstrumentation().getContext().getSystemService(Context.WIFI_SERVICE);

        wifiManager.setWifiEnabled(false);
        boolean await1 = waitOneLatch.await(30, TimeUnit.SECONDS);
        assertTrue(await1);

        wifiManager.setWifiEnabled(true);
        boolean await2 = waitTwoLatch.await(30, TimeUnit.SECONDS);
        assertTrue(await2);

        btConnector.Stop();
        assertEquals(currState,BTConnector.State.Idle);

        btConnector = null;
    }
    /**
     * Test that we get right state changes for turning Bluetooth on / off
     */
    public final void testBluetoothOnOff() throws InterruptedException {

        final CountDownLatch waitZeroLatch = new CountDownLatch(1);
        final CountDownLatch waitOneLatch = new CountDownLatch(1);
        final CountDownLatch waitTwoLatch = new CountDownLatch(1);

        btConnector = new BTConnector(getInstrumentation().getContext(), new BTConnector.Callback() {

            @Override
            public void Connected(BluetoothSocket socket, boolean incoming) {

            }

            @Override
            public void StateChanged(BTConnector.State newState) {
                currState = newState;

                switch(currState){
                    case Idle:
                        break;
                    case NotInitialized:
                        break;
                    case WaitingStateChange:
                        if(waitZeroLatch.getCount() <= 0) {
                            waitOneLatch.countDown();
                        }
                        break;
                    case FindingPeers:
                        if(waitOneLatch.getCount() <= 0){
                            waitTwoLatch.countDown();
                        }else{
                            waitZeroLatch.countDown();
                        }
                        break;
                    case FindingServices:
                        break;
                    case Connecting:
                        break;
                    case Connected:
                        break;
                }
            }
        });

        assertNotNull(btConnector);

        btConnector.Start();
        boolean await0 = waitZeroLatch.await(30, TimeUnit.SECONDS);
        assertTrue(await0);

        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();

        bluetooth.disable();
        boolean await1 = waitOneLatch.await(30, TimeUnit.SECONDS);
        assertTrue(await1);

        bluetooth.enable();
        boolean await2 = waitTwoLatch.await(30, TimeUnit.SECONDS);
        assertTrue(await2);

        btConnector.Stop();
        assertEquals(currState,BTConnector.State.Idle);

        btConnector = null;
    }
}