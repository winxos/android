package ws.game_serv;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ViewFlipper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MyActivity extends Activity implements android.view.GestureDetector.OnGestureListener {
    EditText et;
    DatagramSocket ds;
    Handler hand=new Handler();
    List<DatagramPacket> dps=new ArrayList<DatagramPacket>();
    private GestureDetector gestureDetector=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        et = (EditText) super.findViewById(R.id.editText);
        try {
            ds = new DatagramSocket(8888);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //判断wifi是否开启
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        String ip = "your ip:"+(ipAddress & 0xFF ) + "." +
                ((ipAddress >> 8 ) & 0xFF) + "." +
                ((ipAddress >> 16 ) & 0xFF) + "." +
                ( ipAddress >> 24 & 0xFF) ;
        et.setText(ip);
        gestureDetector=new GestureDetector(this);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
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
    int x,y;
    int dx=1,dy=2;
    public void test(View v)
    {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(15);
                    x+=dx;
                    y+=dy;
                    if(x<0 || x>750) dx=-dx;
                    if(y<0 || y>750)dy=-dy;
                    for(int i=0;i<20;i++) {
                        send_to_all((100+i)+",loc," + (int)(Math.sin((i*20+x)/30.0)*(x/2)+400)+ "," + (int)(Math.cos((i*2+x)/30.0)*(x/2)+400));
                    }
                    new Thread(this).start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }
    public void start(View v)
    {
        new Thread( new Runnable(){
            @Override
            public void run() {
                try {
                    while(true) {
                        byte [] buf = new byte[16];
                        DatagramPacket dp = new DatagramPacket(buf,buf.length);
                        ds.receive(dp);
                        int a=get_id(dp);
                        if(-1==a)
                        {
                            dps.add(dp);
                            send_str(dp,"id,"+dps.size());
                            send_to_all(dp.getAddress().toString()+" login.");
                        }
                        else {
                            send_to_all(a+","+ new String(dp.getData()));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        et.setText("server start. port 8888.");
    }
    Handler handler1 = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String val = data.getString("value");
            et.setText(et.getText()+"\n"+val);
        }
    };
    public void log(String s)
    {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putString("value", s);
        msg.setData(data);
        handler1.sendMessage(msg);
    }
    public boolean is_byte_array_equal(byte[] a,byte[] b)
    {
        for(int i=0;i<a.length;i++)
        {
            if(a[i]!=b[i])return false;
        }
        return true;
    }
    public int get_id(DatagramPacket dp)
    {
        for(int i=0;i<dps.size();i++)
        {
            if(is_byte_array_equal(dp.getAddress().getAddress(), dps.get(i).getAddress().getAddress()))
            {
                if(dp.getPort()==dps.get(i).getPort())
                {
                    return i;
                }
            }
        }
        return -1;
    }
    public void send_str(DatagramPacket dp,String str)
    {
        dp.setData(str.getBytes());
        try {
            ds.send(dp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void send_to_all(String s)
    {
        for(DatagramPacket d:dps)
        {
            send_str(d,s);
        }
    }

    public void send_buf(DatagramPacket dp,byte[] bs)
    {
        dp.setData(bs);
        try {
            ds.send(dp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);         // 注册手势事件
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e2.getX() - e1.getX() > 120) {            // 从左向右滑动（左进右出）
            return true;
        } else if (e2.getX() - e1.getX() < -120) {        // 从右向左滑动（右进左出）
            Intent ii=new Intent();
            ii.setClass(MyActivity.this,graph.class);
            startActivity(ii);
            MyActivity.this.finish();
            return true;
        }
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }
}
