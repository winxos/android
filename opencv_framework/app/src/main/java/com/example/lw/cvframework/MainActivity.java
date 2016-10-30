package com.example.lw.cvframework;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final String TAG = "msg";
    private SurfaceView im1, im2;
    public Mat imm1, srcgray, imm2, targray;
    private Camera mCamera;
    private SurfaceHolder sh1 = null, sh2 = null;

    private boolean isCatch = false;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.e("msg", "OpenCV loaded successfully");
                    imm1 = new Mat();
                    imm2 = new Mat();
                    srcgray = new Mat();
                    targray = new Mat();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    private void showImg(SurfaceView sv, Mat t) {
        Bitmap raw = Bitmap.createBitmap(t.width(), t.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(t, raw);
        sv.setBackground(new BitmapDrawable(raw));
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();
        try {
            Log.i(TAG, "SurfaceHolder.Callback：surface Created");
            mCamera.setPreviewDisplay(sh1);//set the surface to be used for live preview
            mCamera.setPreviewCallback(MainActivity.this);
        } catch (Exception ex) {
            if (null != mCamera) {
                mCamera.release();
                mCamera = null;
            }
            Log.i(TAG + "initCamera", ex.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        initCamera();
    }

    public void initCamera() {
        if (null != mCamera) {
            Camera.Parameters myParam = mCamera.getParameters();
            myParam.setPictureFormat(PixelFormat.JPEG);//设置拍照后存储的图片格式
            setNeedCameraSize(myParam, 800);
            mCamera.setDisplayOrientation(90);
            myParam.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            mCamera.setParameters(myParam);
            mCamera.startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if(isCatch)
        {
            Camera.Size size = camera.getParameters().getPreviewSize();
            Mat previewFrameMat = new Mat(size.height + size.height / 2, size.width, CvType.CV_8UC1);
            previewFrameMat.put(0, 0, data);
            Mat rawm = new Mat(size.height, size.width, CvType.CV_8UC4);
            Imgproc.cvtColor(previewFrameMat, rawm, Imgproc.COLOR_YUV2RGB_NV21);
            Core.flip(rawm.t(), imm1, 1);
            try {
                Imgproc.cvtColor(imm1, targray, Imgproc.COLOR_RGB2GRAY);
                Canvas c = sh2.lockCanvas();
                c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                Paint p = new Paint();
                String familyName = "Arial";
                Typeface font = Typeface.create(familyName, Typeface.NORMAL);
                p.setColor(Color.RED);
                p.setTypeface(font);
                p.setTextSize(50);
                p.setAntiAlias(true);
                p.setStyle(Paint.Style.FILL);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                String strDate = dateFormat.format(new Date());
                c.drawText(strDate, 10, 50, p);
                sh2.unlockCanvasAndPost(c);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setNeedCameraSize(Camera.Parameters ps, int minWidth) {
        List<Camera.Size> sz = ps.getSupportedPreviewSizes();
        boolean found = false;
        int w = Integer.MAX_VALUE, h = 0;
        for (Camera.Size s : sz) {
            if (s.width >= minWidth && w > s.width) {
                w = s.width;
                h = s.height;
                found = true;
            }
        }
        if (!found) {
            Camera.Size f = sz.get(sz.size() - 1);
            ps.setPreviewSize(f.width, f.height);
        } else {
            ps.setPreviewSize(w, h);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        im1 = (SurfaceView) findViewById(R.id.surfaceView);
        im2 = (SurfaceView) findViewById(R.id.surfaceView2);
        im2.setZOrderOnTop(true);
        sh1 = im1.getHolder();
        sh2 = im2.getHolder();
        sh2.setFormat(PixelFormat.TRANSLUCENT);//translucent半透明 transparent透明
        sh1.addCallback(this);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCatch=!isCatch;
                if(!isCatch)
                {
                    im2.setVisibility(View.INVISIBLE);
                }
                else
                {
                    im2.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("msg", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
        } else {
            Log.d("msg", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
}
