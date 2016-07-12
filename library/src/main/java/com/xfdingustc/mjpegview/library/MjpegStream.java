package com.xfdingustc.mjpegview.library;

import com.orhanobut.logger.Logger;

import java.net.InetSocketAddress;

abstract public class MjpegStream {

    abstract protected void onBitmapReadyAsync(MjpegDecoder decoder, MjpegStream stream);

    abstract protected void onEventAsync(MjpegDecoder decoder, MjpegStream stream);

    abstract protected void onIoErrorAsync(MjpegStream stream, int error);

    private static final String TAG = "MjpegStream";

    private boolean mbRunning = false;
    private MjpegReceiver mReceiver;
    private MjpegDecoder mDecoder;

    private final SimpleQueue<ByteArrayBuffer> mFrameQ;
    private final SimpleQueue<BitmapBuffer> mBitmapQ;

    public MjpegStream() {
        mFrameQ = new SimpleQueue<ByteArrayBuffer>();
        mBitmapQ = new SimpleQueue<BitmapBuffer>();
    }

    public void start(InetSocketAddress serverAddr) {
        if (mbRunning) {
            return;
        }

        mReceiver = new MjpegReceiver(serverAddr, mFrameQ) {
            @Override
            public void onIOError(int error) {
                MjpegStream.this.onIoErrorAsync(MjpegStream.this, error);
            }
        };

        mDecoder = new MjpegDecoder(mFrameQ, mBitmapQ) {
            @Override
            public void onBitmapDecodedAsync(MjpegDecoder decoder, boolean isEvent) {
                if (isEvent) {
                    onEventAsync(decoder, MjpegStream.this);
                } else {
                    onBitmapReadyAsync(decoder, MjpegStream.this);
                }
            }
        };

        mDecoder.start();
        mReceiver.start();

        mbRunning = true;
    }


    public BitmapBuffer getOutputBitmapBuffer(MjpegDecoder decoder) {
        if (decoder == null && (decoder = mDecoder) == null) {
            // called from UI thread, and no decoder
            return null;
        }
        return decoder.getBitmapBuffer();
    }


    public final void stop() {
        if (!mbRunning) {
            return;
        }

        mReceiver.shutdown();
        mReceiver = null;

        mDecoder.shutdown();
        mDecoder = null;

        mbRunning = false;


        Logger.t(TAG).d("buffer: total=" + mFrameQ.getTotalObjects() + ", dropped=" + mFrameQ.getDroppedObjects());
    }

    // API
    public final void postEvent(int event, int delay) {
        mFrameQ.postEvent(event, delay);
    }
}
