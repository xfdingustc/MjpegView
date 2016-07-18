package com.xfdingustc.mjpegview.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


import com.orhanobut.logger.Logger;
import com.xfdingustc.mjpegview.library.mina.MjpegCodecFactory;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.IoServiceListener;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.net.InetSocketAddress;



public class MjpegView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = MjpegView.class.getSimpleName();

    private MjpegStream mMjpegStream;

    private IoSession mSession = null;
    private IoConnector mConnector = null;
    private SurfaceHolder mSurfaceHolder;

    private boolean mUseMina = true;


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


    public void startStream(final InetSocketAddress serverAddr) {
        if (mUseMina) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    startPreviewSocketConnection(serverAddr);
                }
            }).start();
        } else {
            mMjpegStream.start(serverAddr);
        }
    }

    private void startPreviewSocketConnection(InetSocketAddress serverAddr) {
        mConnector = new NioSocketConnector();
        mConnector.setConnectTimeoutMillis(5000);


        mConnector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MjpegCodecFactory()));
        mConnector.addListener(new IoServiceListener() {
            @Override
            public void serviceActivated(IoService ioService) throws Exception {

            }

            @Override
            public void serviceIdle(IoService ioService, IdleStatus idleStatus) throws Exception {

            }

            @Override
            public void serviceDeactivated(IoService ioService) throws Exception {

            }

            @Override
            public void sessionCreated(IoSession ioSession) throws Exception {

            }

            @Override
            public void sessionClosed(IoSession ioSession) throws Exception {

            }

            @Override
            public void sessionDestroyed(IoSession ioSession) throws Exception {

            }
        });

        mConnector.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
//                super.messageReceived(session, message);
                Bitmap bitmap = (Bitmap)message;
                drawBitmap(bitmap);
            }
        });

        try {
            ConnectFuture future = mConnector.connect(serverAddr);
            Logger.t(TAG).d("start connection");
            future.awaitUninterruptibly();
            mSession = future.getSession();
            mSession.getConfig().setReadBufferSize(100000);

            Logger.t(TAG).d("connected");

            // write
            String request = "GET / HTTP/1.1\r\n" + "Host: " + serverAddr + "\r\n" + "Connection: keep-alive\r\n"
                + "Cache-Control: no-cache\r\n" + "\r\n";

            mSession.write(request);
        } catch (Exception e) {
            Logger.t(TAG).d("connection error");
        }
    }



    public void stopStream() {
        if (mUseMina) {
            mSession.close();
        } else {
            mMjpegStream.stop();
        }
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
