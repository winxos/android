package com.aist.embed_cloud;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class main extends Activity {
    private EditText en,em;
    private ListView lv;
    private DatagramSocket ds;
    private MulticastSocket ms;
    public static final String BROAD="WS.BROAD";
    private InetAddress lip=null;
    private String msg="";
    private List<String> ips;
    private boolean lock=false,connected=false;
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

        en = (EditText) super.findViewById(R.id.editText3);
        em = (EditText) super.findViewById(R.id.editText);
        lv=(ListView)super.findViewById(R.id.listView);
        ips=new ArrayList<String>();
        aa=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice, ips);
        lv.setAdapter(aa);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        try {
            ms=new MulticastSocket(port); //broadcast
            ms.setTimeToLive(3); //retry times
        } catch (IOException e) {
            e.printStackTrace();
        }


        new Thread(new Runnable(){ //receive
            @Override
            public void run() { //listen
                try {
                    while(true) {
                        byte [] buf = new byte[128];
                        DatagramPacket dp = new DatagramPacket(buf,buf.length);
                        ms.receive(dp);
                        if(ip_equal(dp.getAddress().getAddress(),lip.getAddress())){ //local data
                            continue;
                        }
                        Message m=new Message();
                        Bundle data=new Bundle();
                        String ts=dp.getAddress().toString();
                        data.putString("ip",ts.substring(1,ts.length()));  //start , end
                        data.putString("data",new String(buf).trim());  //start , end
                        m.setData(data);
                        update_view.sendMessage(m);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
            String s=data.getString("ip");
            String d=data.getString("data");
            if(d.equals("login^"))//server
            {
                new Thread(new sender(s,"agree^")).start();
            }
            else if(d.equals("nice^"))//server
            {
                new Thread(new sender(s,"success^")).start();
                boolean know=false;
                for(int i=0;i<aa.getCount();i++) //added to listview
                {
                    if(aa.getItem(i).equals(s))
                        know=true;
                    break;
                }
                if(!know) {
                    aa.add(s);
                    aa.notifyDataSetChanged();
                }
            }
            else if (d.equals("agree^") && !lock && !connected) //client
            {
                new Thread(new sender(s,"nice^")).start();
                lock=true;
            }
            else if(d.equals("success^")) //client
            {
                boolean know=false;
                for(int i=0;i<aa.getCount();i++) //added to listview
                {
                    if(aa.getItem(i).equals(s))
                        know=true;
                    break;
                }
                if(!know) {
                    aa.add(s);
                    aa.notifyDataSetChanged();
                }
                connected=true;
                lock=false;
            }
            else
            {

            }
            Intent ii=new Intent(BROAD);
            ii.putExtra("msg",data.getString("ip")+":"+data.getString("data"));
            sendBroadcast(ii);//update other activity
        }
    };
    public boolean ip_equal(byte[]a,byte[]b)
    {
        return a[0]==b[0] && a[1]==b[1] && a[2]==b[2] && a[3]==b[3];
    }
    public class sender implements Runnable
    {
        private String _ip,msg;
        public sender(String ip,String data)
        {
            _ip=ip;
            msg=data;
        }
        public void run()
        {
            try {
                ms.send(new DatagramPacket(msg.getBytes(), msg.length(),
                        InetAddress.getByName(_ip), port));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void sendto(View v)
    {
        String msg = em.getText().toString();
        for(int i=0;i<aa.getCount();i++) {
            View vb=lv.getChildAt(i);
            CheckedTextView tv = (CheckedTextView)vb.findViewById(android.R.id.text1);
            if(tv.isChecked()) {
                new Thread(new sender(aa.getItem(i),msg)).start();
            }
        }
    }
    public void broadcast(View v)
    {
        if(!connected)
            new Thread(new sender("255.255.255.255","login^")).start();
    }
    public void viewlogs(View v)
    {
        Intent ii=new Intent();
        ii.setClass(this,logs.class);
        startActivity(ii);
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }
}
