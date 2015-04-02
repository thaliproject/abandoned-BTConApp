package org.thaliproject.p2p.btconnectorlib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.test.InstrumentationTestCase;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by juksilve on 1.4.2015.
 * Test class for testing main functionality of the BluetoothBase class
 */
public class BluetoothBaseTest extends InstrumentationTestCase {

    BluetoothBase wBase = null;
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        assertNotNull(bluetooth);
        assertTrue("Bluetooth is off. Turn Bluetooth on before testing", bluetooth.isEnabled());
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
     * Test Bluetooth setting on / off, and that we get events for it
     */
    public final void testBluetoothOnOff() throws InterruptedException {

        final CountDownLatch btOnLatch = new CountDownLatch(1);
        final CountDownLatch btOffLatch = new CountDownLatch(1);

        wBase = new BluetoothBase(getInstrumentation().getContext(), new BluetoothBase.BluetoothStatusChanged() {

            @Override
            public void Connected(BluetoothSocket socket) {}
            @Override
            public void GotConnection(BluetoothSocket socket) {}
            @Override
            public void ConnectionFailed(String reason) {}
            @Override
            public void ListeningFailed(String reason) {}
            @Override
            public void BluetoothStateChanged(int state) {
                if (state == BluetoothAdapter.SCAN_MODE_NONE) {
                    btOffLatch.countDown();
                } else if (state == BluetoothAdapter.SCAN_MODE_CONNECTABLE
                        || state == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    btOnLatch.countDown();
                }
            }
        });

        assertNotNull(wBase);

        boolean startOk = wBase.Start();
        assertTrue("This device does not support Bluetooth", startOk);

        wBase.SetBluetoothEnabled(false);
        boolean await = btOffLatch.await(30, TimeUnit.SECONDS);
        assertTrue(await);

        wBase.SetBluetoothEnabled(true);
        boolean awaitAgain = btOnLatch.await(30, TimeUnit.SECONDS);
        assertTrue(awaitAgain);
    }

}