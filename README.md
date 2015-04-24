# BTConApp
Android p2p Library, using Wifi Direct for discovery, and Bluetooth for Communications. Includes example application for usage

# Usage

Include the BtConnectorLib in your application, create an instance of the  org.thaliproject.p2p.btconnectorlib.BTConnector class.

The constructor takes five arguments, Context,  Callback, Selector, settings and InstancePassword. In where the Context is normal application context, and the Callback is interface that implements two functions. 

The Selector is other callback interface, which can be set to null as well. it is set if the application wants to get knowledge on the services discovered and wants to implement selection of the device which the connection is to be made to. If null vale is set, then the library will use its own default scheduler for device selection for connections.

Settings argument will be instance of BTConnectorSettings, which with the application will tell the library the SERVICE_TYPE used with Wifi Direct services, the UUID used with Blue tooth connections as well as String to be used as a name for Blue tooth.

The InstancePassword is then used for encrypting the instance name variable used for delivering the Blue tooth address information, so it would be not directly human readable.

To start the discovery process, you need to call Start() for the instance. And if you need to stop the library, then call Stop().

Each time the library makes successful connection, it will stop is logic and go to Idle state. And thus if additional connections are needed, you need to call Start() to restart the logic.


# Callback functions

Connected(BluetoothSocket socket, boolean incoming) function will be called by the library when connection is established to remote device, the socket argument has already connected socket, thus you can start reading /writing to it strait away. The value for the incoming argument specifies whether the connection was started by the remote device.

void StateChanged(State newState) function is called by the library when internal state of the library changes. possible values are:
* Idle: The Library is ready, but not actively doing anything.
* NotInitialized: Library has not been initialized
* WaitingStateChange: Bluetooth or Wife is disabled, thus the library is waiting for them to be enabled before doing anything.
* FindingPeers: Library is currently discovering peers.
* FindingServices: Library has detected peer devices, and is now checking whether they are advertising the service we use.
* Connecting: Library has discovered a device having our service, and is attempting to establish connection to it.
* Connected: Library has established connection.

# things to do 

Each application using the library, should use their own unique SERVICE_TYPE as well as set own unique InstancePassword, simply to avoid conflicts with other applications using the library.


# Note

If the Selector interface is set to null, then the library will be making connections automatically, and it is trying to make connections to devices which have not had connections before. 

This feature is implemented by storing information of each attempted connection into a list inside org.thaliproject.p2p.btconnectorlib.WifiBase class.

Then after we have discovered services around, the list is gone through and first device which is not stored in the list will be used for connection attempt.

In case all devices are found from the list, then the first device on the list (has been longest time since last connection attempt) will be selected and moved to be last item on the list.

By default the list contains maximum of 100 items, if more is needed, you would need to change the value used inside the SelectServiceToConnect() function defined in org.thaliproject.p2p.btconnectorlib.WifiBase class


