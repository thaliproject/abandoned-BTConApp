# BTConApp
Android p2p Library, using Wifi Direct for discovery, and Bluetooth for Communications. Includes example application

# Usage

To include the BtConnectorLib in your application, create an instance of the org.thaliproject.p2p.btconnectorlib.BTConnector class.

The constructor takes five arguments: Context, Callback, Selector, settings and InstancePassword. Context is the normal application context. 

Callback and Selector are both call back interfaces, see the next section for details on their use.

The settings argument will be an instance of BTConnectorSettings, which with the application will use to tell the library the SERVICE_TYPE used with Wifi Direct services, the UUID used with Blue tooth connections as well as a String to be used as a name for Blue tooth.

The InstancePassword is then used for encrypting the instance name variable used for delivering the Blue tooth address information, so it would be not directly human readable.

To start the discovery process, you need to call Start() for the instance. And if you need to stop the library, then call Stop().

Each time the library makes a successful connection, it will stop and go to Idle state. To detect further services one has to call Start() to restart the discovery logic.


# Callback functions

Connected(BluetoothSocket socket, boolean incoming) function will be called by the library when a connection is established to a remote device, the socket argument provides the connected socket, thus you can start reading/writing to it strait away. The value for the incoming argument specifies whether the connection was started by the remote device.

void StateChanged(State newState) function is called by the library when internal state of the library changes. possible values are:
* Idle: The Library is ready, but not actively doing anything.
* NotInitialized: Library has not been initialized
* WaitingStateChange: Bluetooth or Wifi is disabled, thus the library is waiting for them to be enabled before doing anything.
* FindingPeers: Library is currently discovering peers.
* FindingServices: Library has detected peer devices, and is now checking whether they are advertising the service we use.
* Connecting: Library has discovered a device with the desired service and is attempting to establish a connection to it.
* Connected: Library has established a connection.

# things to do 

Each application using the library, should use their own unique SERVICE_TYPE as well as set their own unique InstancePassword to avoid conflicts with other applications using the library.


# Note

If the Selector interface is set to null, then the library will make connections automatically to any device it has not connected to before.

This feature is implemented by storing information of each attempted connection into a list inside org.thaliproject.p2p.btconnectorlib.WifiBase class.

Once a service is discovered we check the list and the first device we find that is not in the list will be connected to.

In case all devices are found from the list, then the first device on the list (this will be the one we connected to most distantly in the past) will be selected and moved to be last item on the list.

By default the list contains maximum of 100 items, if more is needed, you would need to change the value used inside the SelectServiceToConnect() function defined in org.thaliproject.p2p.btconnectorlib.WifiBase class
