package com.aist.net_cloud;

import java.net.InetAddress;

/**
 * Created by winxos on 2014-9-27.
 */
public class Agent {
    public String name;
    private InetAddress ip;
    public boolean isConnected;
public boolean isAuthorized;
    public Agent(InetAddress ip_) {
        ip = ip_;
        name = "";
        isConnected = false;
        isAuthorized=false;
    }

    public InetAddress getIp() {
        return ip;
    }
};
