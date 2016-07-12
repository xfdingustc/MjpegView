package com.xfdingustc.mjpegview.library;

import android.util.Log;

import org.apache.mina.core.buffer.IoBuffer;

import java.io.IOException;

public class MjpegBuffer2 {

    static final boolean DEBUG = true;
    static final String TAG = "MjpegBuffer";

    public static final int MAX_LENGTH = 512;

    private final byte[] mInternalData = new byte[MAX_LENGTH];
    private IoBuffer input;

    private int mOffset;
    private int mSize;

    public int state;

    public static final int STATE_SEARCH_FOR_HEAD = 0;
    public static final int STATE_WAIT_FOR_FRAME = 1;

    private static final byte[] HTTP_END = {'\r', '\n', '\r', '\n'};
    // Content-Length:
    private static final byte[] Content_Length = {'C', 'o', 'n', 't', 'e', 'n', 't', '-', 'L', 'e', 'n', 'g', 't',
        'h', ':'};

    public MjpegBuffer2() {

    }


    public void setInput(IoBuffer input) {
        this.input = input;
    }





    private void readFully(byte[] buf, int offset, int size) throws IOException {
        byte[] tempBuf = new byte[size];
        input.get(tempBuf, 0, size);
        System.arraycopy(tempBuf, 0, buf, offset, size);

//        mOffset += size;
//        mSize -= size;
    }

    public void refill() throws IOException {
        if (DEBUG) {
            Log.d(TAG, "refill");
        }
        if (mSize > 0) {
            System.arraycopy(mInternalData, mOffset, mInternalData, 0, mSize);
        }
        mOffset = 0;
        int toRead = mInternalData.length - mSize;
        if (toRead > 0) {
            readFully(mInternalData, mSize, toRead);
            mSize += toRead;
        }
    }

    public void read(byte[] buf, int offset, int size) throws IOException {
        if (DEBUG) {
            Log.d(TAG, "read, offset=" + offset + ", mSize=" + size);
        }
        if (this.mSize > 0) {
            int tocopy = this.mSize;
            if (tocopy > size) {
                tocopy = size;
            }
            System.arraycopy(this.mInternalData, this.mOffset, buf, 0, tocopy);
            this.mOffset += tocopy;
            this.mSize -= tocopy;
            offset += tocopy;
            size -= tocopy;
        }
        if (size > 0) {
            readFully(buf, offset, size);
        }
    }

    private int findSequence(final byte[] seq) {
        int seqlen = seq.length;
        int len = mSize - seqlen;
        for (int index = mOffset, i = len; i > 0; i--, index++) {
            if (mInternalData[index] == seq[0]) {
                int src_index = index + 1;
                int dst_index = 1;
                int cmp = seqlen - 1;
                for (; cmp > 0; cmp--) {
                    if (mInternalData[src_index] != seq[dst_index]) {
                        break;
                    }
                    src_index++;
                    dst_index++;
                }
                if (cmp == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private void skip(int offset, final byte[] seq) {
        int delta = offset - this.mOffset + seq.length;
        this.mOffset += delta;
        this.mSize -= delta;
    }

    public void skipHttpEnd() throws IOException {
        int offset = findSequence(HTTP_END);
        if (offset < 0) {
            throw new IOException("http response end not found");
        }
        if (DEBUG) {
            Log.d(TAG, "skipHttpEnd, offset at " + offset);
        }
        skip(offset, HTTP_END);
    }

    public void skipContentLength() throws IOException {
        int offset = findSequence(Content_Length);
        if (offset < 0) {
            Log.d(TAG, "Content-Length not found");
            throw new IOException("Content-Length not found");
        }
        if (DEBUG) {
            Log.d(TAG, "skipContentLength, offset at " + offset);
        }
        skip(offset, Content_Length);
    }

    public int scanInteger() throws IOException {
        if (mSize == 0) {
            throw new IOException("scanInteger: no more bytes");
        }

        while (mInternalData[mOffset] == ' ') {
            mOffset++;
            mSize--;
            if (mSize == 0) {
                throw new IOException("scanInteger: no more bytes");
            }
        }

        int result = 0;
        int index = mOffset;
        for (int n = mSize; n > 0; n--, index++) {
            byte v = mInternalData[index];
            if (v >= '0' && v <= '9') {
                result = result * 10 + (v - '0');
            } else {
                break;
            }
        }
        mSize -= (index - mOffset);
        mOffset = index;

        if (DEBUG) {
            Log.d(TAG, "scanInteger returns " + result);
        }

        return result;
    }
}
