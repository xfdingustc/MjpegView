package com.xfdingustc.mjpegview.library;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

abstract public class MjpegDecoder extends Thread {

    protected static final boolean DEBUG = false;
    protected static final String TAG = "MjpegDecoder";

    protected final SimpleQueue<ByteArrayBuffer> mInputQ;
    protected final SimpleQueue<BitmapBuffer> mOutputQ;
    protected final BitmapBuffer.Manager mBitmapManager;
    protected final BitmapFactory.Options mOptions;

    // callback - bb may be null
    abstract public void onBitmapDecodedAsync(MjpegDecoder decoder, boolean isEvent);

    public MjpegDecoder(SimpleQueue<ByteArrayBuffer> inputQ, SimpleQueue<BitmapBuffer> outputQ) {
        super("JpegDecoder");

        mInputQ = inputQ;
        mOutputQ = outputQ;
        mBitmapManager = new BitmapBuffer.Manager(2);

        mOptions = new BitmapFactory.Options();
        mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
    }

    // API
    public void shutdown() {
        interrupt();
    }

    // API
    public final BitmapBuffer getBitmapBuffer() {
        return mOutputQ.peekObject();
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                ByteArrayBuffer buffer = mInputQ.waitForObjectAndEvent();
                if (buffer == null) {
                    // this is an event
                    onBitmapDecodedAsync(this, true);
                } else {
                    decodeOneFrame(buffer);
                }
            }
            if (DEBUG) {
                Log.d(TAG, "finish");
            }
        } catch (InterruptedException e) {
            if (DEBUG) {
                Log.d(TAG, "interrupted");
            }
        }
        mOutputQ.clear();
        mBitmapManager.clear();
    }

    private void decodeOneFrame(ByteArrayBuffer buffer) throws InterruptedException {

        byte[] data = buffer.getBuffer();
        BitmapBuffer bb = mBitmapManager.allocateBitmap();

        mOptions.inMutable = true;
        mOptions.inSampleSize = 1;

        if (bb.mBitmap != null) {
            // using the old bitmap; need to check if sizes match
            mOptions.inJustDecodeBounds = true;
            mOptions.inBitmap = null;
            BitmapFactory.decodeByteArray(data, 0, data.length, mOptions);
            mOptions.inJustDecodeBounds = false;
            if (mOptions.outWidth != bb.mBitmap.getWidth() || mOptions.outHeight != bb.mBitmap.getHeight()) {
                mOptions.inBitmap = null;
            } else {
                mOptions.inBitmap = bb.mBitmap;
            }
        } else {
            mOptions.inBitmap = null;
        }

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, mOptions);
        buffer.recycle();
        bb.setBitmap(bitmap);

        // save the result in Q
        mOutputQ.putObject(bb);

        // notify
        onBitmapDecodedAsync(this, false);
    }
}
