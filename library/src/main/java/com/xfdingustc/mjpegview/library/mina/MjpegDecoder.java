package com.xfdingustc.mjpegview.library.mina;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.xfdingustc.mjpegview.library.MjpegBuffer2;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import java.io.IOException;
import java.nio.ByteOrder;

/**
 * Created by Xiaofei on 2016/7/12.
 */
public class MjpegDecoder extends CumulativeProtocolDecoder {
    private static final String TAG = MjpegDecoder.class.getSimpleName();
    private static final byte[] HTTP_END = {'\r', '\n', '\r', '\n'};
    // Content-Length:
    private static final byte[] Content_Length = {'C', 'o', 'n', 't', 'e', 'n', 't', '-', 'L', 'e', 'n', 'g', 't',
        'h', ':'};

    private byte[] tempBuffer = new byte[8192 * 4];

    private static MjpegBuffer2 mjpegBuffer2 = new MjpegBuffer2();

    private static int previousFrameLength = -1;

    @Override
    protected boolean doDecode(IoSession ioSession, IoBuffer ioBuffer, ProtocolDecoderOutput protocolDecoderOutput) throws Exception {
        ioBuffer.order(ByteOrder.LITTLE_ENDIAN);
        if (ioBuffer.remaining() < 512) {
            return false;
        }

        mjpegBuffer2.setInput(ioBuffer);


        if (previousFrameLength < 0) {
            mjpegBuffer2.refill();
            Log.d(TAG, "refilled");
            mjpegBuffer2.skipContentLength();
            Log.d(TAG, "skipContentLength");
            int frameLen = mjpegBuffer2.scanInteger();
            if (frameLen <= 0) {
                throw new IOException("cannot get Content-Length");
            }

            previousFrameLength = frameLen;
            mjpegBuffer2.skipHttpEnd();
            Log.d(TAG, "skipHttpEnd: " + frameLen);
            // read frame

            return false;


        } else {
            if (previousFrameLength > ioBuffer.limit()) {
                Log.d(TAG, "framelength: " + previousFrameLength + " limit: " + ioBuffer.remaining());
                return false;
            } else {
                Log.d(TAG, "decode one frame");
                byte[] bitmapDataBuffer = new byte[previousFrameLength];
                mjpegBuffer2.read(bitmapDataBuffer, 0, previousFrameLength);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapDataBuffer, 0, bitmapDataBuffer.length);
                protocolDecoderOutput.write(bitmap);
                previousFrameLength = -1;
                return true;
            }

        }


//


    }
}
