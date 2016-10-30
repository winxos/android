package com.example.winxos.net_server;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class main extends Activity {
    private EditText ep,em,eip;
    private ListView lv;
    private DatagramSocket ds;
    private MulticastSocket ms;
    public static final String BROAD="WS.BROAD";
    private InetAddress lip=null;
    private String msg="";
    private List<String> ips;
    ArrayAdapter<String> aa;
    final int port= 10001;
    CheckBox cb;
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //判断wifi是否开启
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        try {
            lip=InetAddress.getByName((ipAddress & 0xFF ) + "." +
                    ((ipAddress >> 8 ) & 0xFF) + "." +
                    ((ipAddress >> 16 ) & 0xFF) + "." +
                    ( ipAddress >> 24 & 0xFF) );
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String ip = "local ip:"+lip.toString() ;
        Toast.makeText(this,ip,Toast.LENGTH_LONG).show();

        ep = (EditText) super.findViewById(R.id.editText3);
        em = (EditText) super.findViewById(R.id.editText);
        lv=(ListView)super.findViewById(R.id.listView);
        ips=new ArrayList<String>();
        aa=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice, ips);
        lv.setAdapter(aa);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private Handler  update_view=new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            Bundle data=msg.getData();
            String s=data.getString("value");
            for(int i=0;i<aa.getCount();i++)
            {
                if(aa.getItem(i).equals(s))
                    return;
            }
            aa.add(s);
            aa.notifyDataSetChanged();
        }
    };

    public void listen(View v)
    {

        if(ms==null)
            try {
                ms=new MulticastSocket(port);
            } catch (IOException e) {
                e.printStackTrace();
            }

        new Thread(new Runnable(){ //receive
            @Override
            public void run() {
                try {
                    while(true) {
                        byte [] buf = new byte[1024];
                        DatagramPacket dp = new DatagramPacket(buf,buf.length);
                        ms.receive(dp);
                        if(ip_equal(dp.getAddress().getAddress(),lip.getAddress())){ //local data
                            continue;
                        }
                        Message m=new Message();
                        Bundle data=new Bundle();
                        String ts=dp.getAddress().toString();
                        data.putString("value",ts.substring(1,ts.length()));  //start , end
                        m.setData(data);
                        update_view.sendMessage(m);

                        Intent ii=new Intent(BROAD);
                        msg=dp.getAddress().toString()+":"+new String(buf);
                        ii.putExtra("msg",msg);
                        sendBroadcast(ii);//update other activity
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public boolean ip_equal(byte[]a,byte[]b)
    {
        return a[0]==b[0] && a[1]==b[1] && a[2]==b[2] && a[3]==b[3];
    }
    public void sendto(View v)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg = em.getText().toString();

                for(int i=0;i<aa.getCount();i++) {
                    View vb=lv.getChildAt(i);
                    CheckedTextView tv = (CheckedTextView)vb.findViewById(android.R.id.text1);
                    if(tv.isChecked()) {
                        try {
                            ms.send(new DatagramPacket(msg.getBytes(), msg.length(),
                                    InetAddress.getByName(aa.getItem(i)), port));

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    public void broadcast(View v)
    {
        try {
            ms=new MulticastSocket(port); //broadcast
            ms.setTimeToLive(3); //retry times
            Timer mTimer=new Timer();
            mTimer.schedule(new TimerTask() {//broad cast
                @Override
                public void run() {
                    String msg="server";
                    try {
                        ms.send(new DatagramPacket( msg.getBytes(), msg.length(),
                                InetAddress.getByName("255.255.255.255"), port));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 1000);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void viewlogs(View v)
    {
        Intent ii=new Intent();
        ii.setClass(this,logs.class);
        startActivity(ii);
    }

}
