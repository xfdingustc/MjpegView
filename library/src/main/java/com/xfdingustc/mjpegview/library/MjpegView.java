package com.xfdingustc.mjpegview.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.orhanobut.logger.Logger;

import java.net.InetSocketAddress;


public class MjpegView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = MjpegView.class.getSimpleName();

    private MjpegStream mMjpegStream;


    private SurfaceHolder mSurfaceHolder;

    private boolean mUseMina = false;


    public MjpegView(Context context) {
        super(context);
        initView();
    }

    public MjpegView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public MjpegView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder = null;
    }

    private void initView() {
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mMjpegStream = new MyMjpegStream();
    }

    public void setMinaEnabled(boolean enableMina) {
        mUseMina = enableMina;
    }


    public void startStream(final InetSocketAddress serverAddr) {

        mMjpegStream.start(serverAddr);

    }


    public void stopStream() {

        mMjpegStream.stop();

    }


    class MyMjpegStream extends MjpegStream {

        @Override
        protected void onBitmapReadyAsync(MjpegDecoder decoder, MjpegStream stream) {
            BitmapBuffer bb = stream.getOutputBitmapBuffer(decoder);
            if (bb != null) {
                drawBitmap(bb.getBitmap());
            }
        }

        @Override
        protected void onEventAsync(MjpegDecoder decoder, MjpegStream stream) {
        }

        @Override
        protected void onIoErrorAsync(MjpegStream stream, final int error) {

        }

    }


    private void drawBitmap(Bitmap bitmap) {
        if (bitmap == null || mSurfaceHolder == null) {
            Logger.t(TAG).d("mSurfaceHolder " + mSurfaceHolder);
            return;
        }

        Canvas canvas = mSurfaceHolder.lockCanvas();
        if (canvas == null) {
            return;
        }
        Rect rect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(bitmap, null, rect, null);
        mSurfaceHolder.unlockCanvasAndPost(canvas);
    }

}
