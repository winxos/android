package com.aist.net_cloud;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class NetCloud {
    public static final String router_gap = "~",command_gap="\\^",parameter_gap=",";
    public boolean flag = false;
    public static final int UDP_PORT = 10400;
    public static final int MAX_BUF = 128;
    public InetAddress ip = null,last_comm_ip=null;
    private MulticastSocket ms;
    private DatagramPacket dp;

    public Agents agents;
    public static final int MAX_CLIENT_NUMBER = 10;

    public static final int SERVER = 0, CLIENT = 1;
    private static int role = SERVER;

    private Handler hand;

    public NetCloud() {
    }

    public NetCloud(Handler h, InetAddress ip) {
        hand = h;
        this.ip = ip;
        init();
    }

    private int handshake_state = 0;

    public boolean handshake(Agent a, String cmd) {
        String[] tmp = cmd.split(command_gap);
        String op = tmp[0];

        switch (handshake_state) {
            case 0:
                if (op.equals("login")) {
                    if (agents.getAgentCount() < MAX_CLIENT_NUMBER) { //max client
                        send(a.getIp(), "agree^");
                        handshake_state = 1;
                    }
                }
                break;
            case 1:
                if (op.equals("nice")) {
                    send(a.getIp(), "success^");
                    handshake_state = 0;
                    a.isConnected = true;
                    return true;
                }
                break;
        }
        return false;
    }

    //handshake_cache for handshake with the same server
    private ArrayList<InetAddress> handshake_cache = new ArrayList<InetAddress>();

    private void login_success(Agent a) {
        handshake_state = 0;
        a.isConnected = true;
        role = SERVER;
    }

    private boolean request_handshake(Agent a, String cmd) {
        String[] tmp = cmd.split("\\^");
        String op = tmp[0];
        switch (handshake_state) {
            case 0:
                break;
            case 1:
                if (op.equals("agree")) {
                    send(a.getIp(), "nice^");
                    handshake_cache.add(a.getIp());
                    handshake_state = 2;
                }
                if (op.equals("again")) {
                    login_success(a);
                    return true;
                }
                break;
            case 2:
                if (handshake_cache.contains(a.getIp()) && op.equals("success")) {
                    login_success(a);
                    return true;
                }
                break;
        }
        return false;
    }
    public void request_login() {
        if (agents.getAgentCount() > 0) //already connected
        {
            send_msg("data", "ALREADY CONNECTION");
        } else {
            handshake_state = 1;
            broadcast("login^");
            role = CLIENT;
        }
    }
    public void send_msg(String tag, String data) {
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString(tag, data);
        msg.setData(b);
        hand.sendMessage(msg);
    }

    public void init() {
        agents = new Agents();
        flag = true;
        if (ms == null) {
            try {
                ms = new MulticastSocket(UDP_PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {//
                    if (!flag) continue;
                    byte[] buf;
                    buf = new byte[MAX_BUF];
                    dp = new DatagramPacket(buf, buf.length);
                    try {
                        ms.receive(dp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (dp.getAddress().equals(ip)) { //local data
                        continue;
                    }
                    last_comm_ip=dp.getAddress();
                    String cmd = new String(buf).trim();
                    Agent a = agents.getAgent(last_comm_ip);
                    switch (role) {
                        case CLIENT:
                            if (a == null) {//new join
                                a = new Agent(last_comm_ip);
                            }
                            if (!a.isConnected) {
                                if (request_handshake(a, cmd)) {
                                    agents.add(a);
                                    send_msg("login", last_comm_ip.toString());
                                }
                                continue;
                            }
                            break;
                        case SERVER:
                            if (a == null) {//new join
                                a = new Agent(last_comm_ip);
                            }
                            if (!a.isConnected) {
                                if (handshake(a, cmd)) {
                                    agents.add(a);
                                    send_msg("login", last_comm_ip.toString());
                                }
                                continue;
                            } else {
                                if (cmd.equals("login^")) //once connected
                                {
                                    send(last_comm_ip, "again^");
                                    continue;
                                }
                            }
                            break;
                    }
                    String[] tmp = cmd.split(router_gap);
                    if (tmp.length==3) { //normal
                        try {
                            InetAddress ii = InetAddress.getByName(tmp[0]);
                            if (ii.equals(ip)) { //self run
                                send_msg("cmd",cmd.substring(tmp[0].length()+1,cmd.length()));
                            } else { //other send to others
                                send_to_all_agent_but(cmd,last_comm_ip);
                            }
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    } else if(tmp.length==2) { //broad cast
                        send_msg("cmd",cmd);
                    }
                    else//invalid
                    {

                    }
                }
            }
        }).start();
    }

    public void send(InetAddress ip, String msg) {
        class sender implements Runnable {
            private InetAddress ip;
            private String msg;
            public sender(InetAddress ip, String msg) {
                this.ip = ip;
                this.msg = msg;
            }
            @Override
            public void run() {
                try {
                    ms.send(new DatagramPacket(msg.getBytes(), msg.length(), ip, UDP_PORT));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        new Thread(new sender(ip, msg)).start();
    }

    public void send_to_all_agent_but(String msg, InetAddress not_send) {
        Agent a;
        for (int i = 0; i < agents.getAgentCount(); i++) {
            a = agents.getAgent(i);
            if (a.getIp().equals(not_send))
                continue;
            send(a.getIp(), msg);
        }
    }
    public void send_to_all(String msg) {
        for (int i = 0; i < agents.getAgentCount(); i++) {
            send(agents.getAgent(i).getIp(), msg);
        }
    }
    public void broadcast(String msg) {
        try {
            send(InetAddress.getByName("255.255.255.255"), msg);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Agent> get_all() {
        return agents.getAll();
    }


}
