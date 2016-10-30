package com.aist.net_cloud;

import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Created by winxos on 2014-9-27.
 */
public class Agents {
    private ArrayList<Agent> la;
    private static final int NOT_EXIST = -1;

    public Agents() {
        la = new ArrayList<Agent>();
    }

    public ArrayList<Agent> getAll() {
        return la;
    }

    public int getAgentCount() {
        return la.size();
    }

    public boolean isOnline(InetAddress ip) {
        int id = search(ip);
        if (id == NOT_EXIST) return false;
        return la.get(id).isConnected;
    }

    public Agent getAgent(InetAddress ip) {
        int id = search(ip);
        if (id == NOT_EXIST) return null;
        return la.get(id);
    }
    public Agent getAgent(int id) {
        return la.get(id);
    }
    private int search(InetAddress ip) {
        for (int i = 0; i < la.size(); i++) {
            if (ip.equals(la.get(i).getIp())) {
                return i;
            }
        }
        return NOT_EXIST;
    }

    public boolean add(InetAddress ip) {
        return add(new Agent(ip));
    }

    public boolean add(Agent a) {
        if (search(a.getIp()) != NOT_EXIST)
            return false;
        la.add(a);
        return true;
    }

    public void save() {

    }

    public void load() {

    }
}