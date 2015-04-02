// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btconnectorlib;

/**
 * Created by juksilve on 12.3.2015.
 */
public class ServiceItem{

    public ServiceItem(String instance,String type,String address, String name){
        this.instanceName = instance;
        this.serviceType = type;
        this.deviceAddress = address;
        this.deviceName =  name;
    }
    public String instanceName;
    public String serviceType;
    public String deviceAddress;
    public String deviceName;
}
