package com.aist.net_car;

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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class main extends Activity {
    private int mode = 0;
    private Smart_Net smart_net;
    InetAddress ip;
    private String[] msg_buf;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket bs;
    SurfaceView sv;
    SurfaceHolder sh;
    TextView tv;
    EditText et1, et2;
    private int sx, sy, cx, cy, dx, dy, ox, oy, l, r;
    Bluetooth_Sender sender;
    public Handler getMsg = new Handler() {
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            if (b.containsKey("login")) { //new client added
                smart_net.send_msg("data", "login to:" + b.getString("login"));
            }
            if (b.containsKey("data")) { //show in textveiw
                show_msg(new SimpleDateFormat("HH:mm:ss> ").format(new Date()) + b.getString("data"));
            }
            if (b.containsKey("toast")) {
                Toast.makeText(main.this, b.getString("toast"), Toast.LENGTH_LONG).show();
            }
            if (b.containsKey("print")) {
                smart_net.send_msg("data", b.getString("print"));
            }
            if (b.containsKey("car")) {
                sender.send(b.getString("car").getBytes());
            }
        }
    };

    private void update_ip() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        try {
            ip = InetAddress.getByName((ipAddress & 0xFF) + "." +
                    ((ipAddress >> 8) & 0xFF) + "." +
                    ((ipAddress >> 16) & 0xFF) + "." +
                    (ipAddress >> 24 & 0xFF));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.aist.net_car.R.layout.activity_main);
        et1 = (EditText) findViewById(com.aist.net_car.R.id.editText);
        et2 = (EditText) findViewById(com.aist.net_car.R.id.editText2);
        sv = (SurfaceView) findViewById(com.aist.net_car.R.id.surfaceView);
        sh = sv.getHolder();
        msg_buf = new String[10];
        for (int i = 0; i < msg_buf.length; i++) {
            msg_buf[i] = "";
        }
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                update_state();
                update_view();
            }
        }, 0, 50);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getName().equals(et1.getText().toString())) { //found
                        mBluetoothAdapter.cancelDiscovery();
                        try {
                            bs = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                            bs.connect();
                            show_msg("连接成功！");
                            sender = new Bluetooth_Sender(bs);
                            mode = 1;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        findViewById(com.aist.net_car.R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBluetoothAdapter == null) {
                    return;
                }
                if (!mBluetoothAdapter.isEnabled()) { //蓝牙未开启，则开启蓝牙
                    startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                }
                if (!mBluetoothAdapter.isDiscovering()) {
                    Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
                    startActivity(i);
                    mBluetoothAdapter.startDiscovery();
                    show_msg("local ip:" + ip.toString());
                }
            }
        });
        findViewById(com.aist.net_car.R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                smart_net.request_login();
                mode = 2;
            }
        });
        findViewById(com.aist.net_car.R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Bluetooth_Sender(bs).start();
            }
        });
        sv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        sx = (int) event.getX();
                        sy = (int) event.getY();
                        ox = sx;
                        oy = sy;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int tx = (int) event.getX() - ox;
                        int ty = (int) event.getY() - oy;
                        dx += tx;
                        dy += ty;
                        if (dx * dx + dy * dy > 140 * 140) {
                            dx -= tx;
                            dy -= ty;
                        }
                        ox = (int) event.getX();
                        oy = (int) event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        dx = 0;
                        dy = 0;
                        l = 0;
                        r = 0;
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        break;
                }
                return true;
            }
        });
        update_ip();
        smart_net = new Smart_Net(getMsg, ip);

    }

    private void show_msg(String msg) {
        for (int i = msg_buf.length - 1; i > 0; i--) {
            msg_buf[i] = msg_buf[i - 1];
        }
        msg_buf[0] = msg;
    }

    private void update_state() {
        if (mode == 0) return;
        byte[] buf = new byte[5];
        buf[0] = 'c';
        buf[4] = 'e';
        int v = -(int) (dy * 0.6 * 255 / 190);
        l = Math.abs(v);
        r = l;
        buf[1] = v > 0 ? (byte) 'f' : (byte) 'b';
        if (Math.abs(v) < 20)
            buf[1] = 'd'; //small speed. disable
        if (Math.abs(dx) < 20)
            dx = 0;
        else if (dx > 0) {
            r -= dx / 3 * 255 / 190;
        } else {
            l += dx / 3 * 255 / 190;
        }
        if (l < 0) l = 0;
        if (r < 0) r = 0;
        buf[2] = (byte) (l * 1.3);//modify self diff
        buf[3] = (byte) r;
        if (mode == 1 && sender != null)
            sender.send(buf);
        else
            smart_net.send_to_all("car^"+new String(buf));
    }

    private void update_view() {
        if(mode==0)return;
        Canvas c = sh.lockCanvas();
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.BLACK);
        c.drawRect(0, 0, c.getWidth(), c.getHeight(), p);
        cy = c.getHeight() / 2;
        cx = c.getWidth() / 2;
        p.setColor(Color.BLUE);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5);
        c.drawCircle(cx, cy, 200, p);
        c.drawCircle(cx, cy, 1, p);
        p.setColor(Color.RED);
        p.setStrokeWidth(2);
        c.drawLine(cx, cy, cx + dx, cy + dy, p);
        p.setColor(Color.CYAN);
        p.setStyle(Paint.Style.FILL);
        c.drawCircle(cx + dx, cy + dy, 30, p);
        p.setColor(Color.GREEN);
        p.setTextSize(40);
        for (int i = 0; i < msg_buf.length; i++) {
            c.drawText(msg_buf[i], 10, 50 + i * 40, p);
        }
        sh.unlockCanvasAndPost(c);
        sv.postInvalidate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(com.aist.net_car.R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == com.aist.net_car.R.id.action_settings) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //sender or receiver
    private class Bluetooth_Sender extends Thread {
        private final OutputStream mmOutStream;

        public Bluetooth_Sender(BluetoothSocket socket) {
            OutputStream tmp = null;
            try {
                tmp = socket.getOutputStream();
            } catch (IOException e) {
            }
            mmOutStream = tmp;
        }

        public void run() {
            send(et2.getText().toString().getBytes());
        }

        public void send(byte[] buf) {
            try {
                mmOutStream.write(buf);
            } catch (IOException e) {
            }
        }
    }
}
