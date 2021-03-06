package com.aist.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class main extends Activity {
    public static final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket bs;
    SurfaceView sv;
    SurfaceHolder sh;
    TextView tv;
    EditText et1,et2;
    private int sx,sy,cx,cy,dx,dy,ox,oy,l,r;
    Bluetooth_Sender sender;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv=(TextView)findViewById(R.id.textView);
        et1=(EditText)findViewById(R.id.editText);
        et2=(EditText)findViewById(R.id.editText2);
        sv=(SurfaceView)findViewById(R.id.surfaceView);
        sh=sv.getHolder();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                update_state();
                update_view();
            }
        },0,50);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(device.getName().equals(et1.getText().toString())) { //found
                        mBluetoothAdapter.cancelDiscovery();
                        try {
                            bs = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                            bs.connect();
                            Toast.makeText(main.this, "连接成功！", Toast.LENGTH_LONG).show();
                            sender=new Bluetooth_Sender(bs);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBluetoothAdapter == null){
                    return;
                }
                if(!mBluetoothAdapter.isEnabled()){ //蓝牙未开启，则开启蓝牙
                   // Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                  //  startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                    startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                }
                if(!mBluetoothAdapter.isDiscovering())
                {
                    Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
                    startActivity(i);
                    mBluetoothAdapter.startDiscovery();
                }
            }
        });
        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Bluetooth_Sender(bs).start();
            }
        });
        sv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getAction();
                switch(action){
                    case MotionEvent.ACTION_DOWN:
                        sx=(int)event.getX();
                        sy=(int)event.getY();
                        ox=sx;
                        oy=sy;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int tx=(int)event.getX()-ox;
                        int ty=(int)event.getY()-oy;
                        dx+=tx;
                        dy+=ty;
                        if(dx*dx+dy*dy>140*140)
                        {
                            dx-=tx;
                            dy-=ty;
                        }
                        ox=(int)event.getX();
                        oy=(int)event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        dx=0;
                        dy=0;
                        l=0;
                        r=0;
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        break;
                }
                tv.setText("dx:"+dx+" dy:"+dy+" L:"+l+" R:"+r);
                return true;
            }
        });
    }

    private void update_state() {
        if(sender==null) return;
        byte []buf=new byte[5];
        buf[0]='c';
        buf[4]='e';
        int v=-(int)(dy*0.6*255/190);
        l=Math.abs(v);
        r=l;
        buf[1]=v>0?(byte)'f':(byte)'b';
        if(Math.abs(v)<20)
            buf[1]='d'; //small speed. disable
        if(Math.abs(dx)<20)
            dx=0;
        else if (dx > 0) {
            r -= dx/3 * 255 / 190;
        } else {
            l += dx/3 * 255 / 190;
        }
        if(l<0)l=0;
        if(r<0)r=0;
        buf[2]=(byte)(l*1.3);
        buf[3]=(byte)r;
        sender.send(buf);
    }
    private void update_view()
    {
        if(sender==null) return;
        Canvas c=sh.lockCanvas();
        Paint p=new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.BLACK);
        c.drawRect(0, 0, c.getWidth(), c.getHeight(), p);
        cy=c.getHeight()/2;
        cx=c.getWidth()/2;
        p.setColor(Color.BLUE);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5);
        c.drawCircle(cx,cy,200,p);
        c.drawCircle(cx,cy,1,p);
        p.setColor(Color.RED);
        p.setStrokeWidth(2);
        c.drawLine(cx,cy,cx+dx,cy+dy,p);
        p.setColor(Color.CYAN);
        p.setStyle(Paint.Style.FILL);
        c.drawCircle(cx+dx,cy+dy,30,p);
        sh.unlockCanvasAndPost(c);
        sv.postInvalidate();
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
    //sender or receiver
    private class Bluetooth_Sender extends Thread {
        private final OutputStream mmOutStream;
        public Bluetooth_Sender(BluetoothSocket socket) {
            OutputStream tmp=null;
            try {
                tmp = socket.getOutputStream();
            } catch (IOException e) {}
            mmOutStream=tmp;
        }
        public void run() {
            send(et2.getText().toString().getBytes());
        }
        public void send(byte[] buf)
        {
            try {
                mmOutStream.write(buf);
            } catch (IOException e) {}
        }
    }
}
