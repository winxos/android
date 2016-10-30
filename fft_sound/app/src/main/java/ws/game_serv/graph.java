package ws.game_serv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.nio.Buffer;
import java.util.Timer;
import java.util.TimerTask;

import ws.game_serv.R;

public class graph extends Activity implements android.view.GestureDetector.OnGestureListener  {

    private GestureDetector gestureDetector=null;
    SurfaceView sfv=null;
    SurfaceHolder sfh=null;
    private Timer mTimer=null;
    int w,h;
    static final int frequency = 8000;//分辨率
    static final int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    int recBufSize;//录音最小buffer大小
    AudioRecord audioRecord;
    short[] as=null;
    int[] asf=null;
    private boolean flag=false;
    int fft_len=64;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        gestureDetector=new GestureDetector(this);
        sfv = (SurfaceView) this.findViewById(R.id.surfaceView);
        sfh = sfv.getHolder();
        mTimer = new Timer();
        recBufSize = AudioRecord.getMinBufferSize(frequency,channelConfiguration, audioEncoding);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,channelConfiguration, audioEncoding, recBufSize);
    }
    int ct=0;
    public void render()
    {
        Canvas canvas = sfh.lockCanvas(new Rect(0, 0, w,h));// 关键:获取画布
        canvas.drawColor(Color.TRANSPARENT);
        Paint p = new Paint();
        //清屏
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPaint(p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));

        Paint mPaint = new Paint();
        mPaint.setColor(Color.GREEN);// 画笔为绿色
        mPaint.setStrokeWidth(1);// 设置画笔粗细
        int det=1;
        if(as!=null) {
            for (int i = 0; i < as.length - det; i += det) {
                canvas.drawLine(i, as[i] / 30 + h / 4, i + det, as[i + det] / 30 + h / 4, mPaint);
            }
        }

        if(asf!=null) {
            mPaint.setColor(Color.BLUE);// 画笔为绿色
            det=640*2/fft_len;
            for (int i = 0; i < asf.length - 1; i++) {
                int yy=asf[i]/100;
                canvas.drawRect(i*det,-yy + h *4 / 5, i*det + det-2,h *4 / 5, mPaint);
            }
        }
        mPaint.setColor(Color.RED);// 画笔为绿色
        mPaint.setTextSize(30);
        int [] aa=max_freq();
        canvas.drawText(String.format("dB:%03d",aa[1]),10,h-100,mPaint);
        canvas.drawText(String.format("Max Freq:%04d",aa[0]),10,h-50,mPaint);
        sfh.unlockCanvasAndPost(canvas);// 解锁画布，提交画好的图像
    }
    public int[] max_freq()
    {
        int n=0,ans=0;
        for(int i=0;i<asf.length;i++)
        {
            if(asf[i]>n)
            {
                n=asf[i];
                ans=i;
            }
        }
        return  new int[]{ans*frequency/fft_len,(int)(20*Math.log(n)/Math.log(10))};
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.graph, menu);
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
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);         // 注册手势事件
    }
    int first=0;
    public void paint(View v)
    {
        if(first==0) {
            w = sfv.getWidth();
            h = sfv.getHeight();
            as = new short[1024];
            asf=new int[fft_len/2];
            first = 1;

        }
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                render();
            }
        },0,20);
        flag=!flag;
        if(flag=true)
        {
            new RecordThread(audioRecord,recBufSize).start();
        }
    }
    private void add_audio(short buf)
    {
        for(int i=0;i<as.length-1;i++)
        {
            as[i]=as[i+1];
        }
        as[as.length-1]=buf;
    }
    class RecordThread extends Thread {
        private int recBufSize;
        private AudioRecord audioRecord;
        public RecordThread(AudioRecord audioRecord, int recBufSize) {
            this.audioRecord = audioRecord;
            this.recBufSize = recBufSize;
        }
        @Override
        public void run() {
            try {
                short[] buffer = new short[recBufSize];
                while(flag) {
                    audioRecord.startRecording();// 开始录制
                    int bufferReadResult = audioRecord.read(buffer, 0, recBufSize);
                    synchronized (as){
                        for(int i=0;i<bufferReadResult;i++) {
                            add_audio(buffer[i]);
                        }
                    }
                    int length=fft_len;
                    Complex[]complexs = new Complex[length];
                    for(int i=0;i < length; i++){
                        Short short1 = as[i];
                        complexs[i] = new Complex(short1.doubleValue());
                    }
                    fft(complexs,length);
                    for (int i = 0; i < length/2; i++) {
                        asf[i] =complexs[i].getIntValue();
                    }
                }
                audioRecord.stop();
            } catch (Throwable t) {
                Log.println(0,"test",t.getMessage().toString());
            }
        }
    };
    private int up2int(int iint) {
        int ret = 1;
        while (ret<=iint) {
            ret = ret << 1;
        }
        return ret>>1;
    }
    //快速傅里叶变换
    public void fft(Complex[] xin,int N)
    {
        int f,m,N2,nm,i,k,j,L;//L:运算级数
        float p;
        int e2,le,B,ip;
        Complex w = new Complex();
        Complex t = new Complex();
        N2 = N / 2;//每一级中蝶形的个数,同时也代表m位二进制数最高位的十进制权值
        f = N;//f是为了求流程的级数而设立的
        for(m = 1; (f = f / 2) != 1; m++);                             //得到流程图的共几级
        nm = N - 2;
        j = N2;
        /******倒序运算——雷德算法******/
        for(i = 1; i <= nm; i++)
        {
            if(i < j)//防止重复交换
            {
                t = xin[j];
                xin[j] = xin[i];
                xin[i] = t;
            }
            k = N2;
            while(j >= k)
            {
                j = j - k;
                k = k / 2;
            }
            j = j + k;
        }
        /******蝶形图计算部分******/
        for(L=1; L<=m; L++)                                    //从第1级到第m级
        {
            e2 = (int) Math.pow(2, L);
            le=e2+1;
            B=e2/2;
            for(j=0;j<B;j++)                                    //j从0到2^(L-1)-1
            {
                p=2*(float)Math.PI/e2;
                w.real = Math.cos(p * j);
                w.image = Math.sin(p*j) * -1;
                //w.imag = -sin(p*j);
                for(i=j;i<N;i=i+e2)                                //计算具有相同系数的数据
                {
                    ip=i+B;                                           //对应蝶形的数据间隔为2^(L-1)
                    t=xin[ip].cc(w);
                    xin[ip] = xin[i].cut(t);
                    xin[i] = xin[i].sum(t);
                }
            }
        }
    }
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e2.getX() - e1.getX() > 120) {            // 从左向右滑动（左进右出）
        } else if (e2.getX() - e1.getX() < -120) {        // 从右向左滑动（右进左出）
            Intent ii=new Intent();
            ii.setClass(graph.this,MyActivity.class);
            startActivity(ii);
            graph.this.finish();
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
