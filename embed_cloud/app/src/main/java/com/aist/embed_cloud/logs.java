package com.aist.embed_cloud;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class logs extends Activity {
    public static final String BROAD="WS.BROAD";
    private TextView tv=null;
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        tv=(TextView) super.findViewById(R.id.textView);
        registerReceiver(new BroadcastReceiver() { //
            @Override
            public void onReceive(Context context, Intent intent) {
                tv.setText(tv.getText()+"\n"+intent.getStringExtra("msg"));
            }

        },new IntentFilter(BROAD) );
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.logs, menu);
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
    public void ret(View v)
    {
        Intent ii=new Intent();
        ii.setClass(this,main.class);
        startActivity(ii);
        overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.slide_out_right);
    }
}
