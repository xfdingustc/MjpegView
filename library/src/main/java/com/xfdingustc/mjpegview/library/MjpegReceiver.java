package com.xfdingustc.mjpegview.library;

import android.util.Log;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

abstract public class MjpegReceiver extends Thread {
    private static final String TAG = MjpegReceiver.class.getSimpleName();
    public static final int ERROR_CANNOT_CONNECT = 1;
    public static final int ERROR_CONNECTION = 2;

    abstract public void onIOError(int error);

    private boolean mbRunning;
    private final InetSocketAddress mServerAddress;
    private final ByteArrayBuffer.Manager mBufferManager;
    private final SimpleQueue<ByteArrayBuffer> mOutputQ;
    private Socket mSocket;
    private MjpegBuffer mBuffer;

    public MjpegReceiver(InetSocketAddress serverAddress, SimpleQueue<ByteArrayBuffer> outputQ) {
        super("MjpegReceiver");
        mServerAddress = serverAddress;
        // 3 buffers: receiving; in queue; decoding
        mBufferManager = new ByteArrayBuffer.Manager(3);
        mOutputQ = outputQ;
    }

    // API
    public void shutdown() {
        mbRunning = false;
        interrupt();
        closeSocket();

        Logger.t(TAG).d("shutdown");
        try {
            join();
        } catch (Exception e) {

        }
        Logger.t(TAG).d("join");
    }

    synchronized private void openSocket() throws SocketException {
        mSocket = new Socket();
        mSocket.setReceiveBufferSize(64 * 1024);
    }

    synchronized private void closeSocket() {
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (Exception ex) {

            }
            mSocket = null;
        }
    }

    private void runOnce() {
        int error = ERROR_CANNOT_CONNECT;
        try {
            connect();
            error = ERROR_CONNECTION;
            while (checkRunning()) {
                readOneFrame();
            }
        } catch (IOException e) {
            Logger.t(TAG).d("IOException: " + e.getMessage());
            onIOError(error);
        }
    }

    @Override
    public void start() {
        mbRunning = true;
        super.start();
    }

    @Override
    public void run() {
        runOnce();
        closeSocket();
        checkRunning();
    }

    private boolean checkRunning() {
        if (mbRunning && !isInterrupted()) {
            return true;
        }

        Logger.t(TAG).d("mbRunning: " + mbRunning + ", isInterrupted: " + isInterrupted());
        return false;
    }

    private void connect() throws IOException {
        Logger.t(TAG).d("connecting to " + mServerAddress);

        while (true) {
            openSocket();
            try {
                mSocket.connect(mServerAddress);
                mSocket.setKeepAlive(true);
                mSocket.setSoTimeout(30000);
                break;
            } catch (IOException e) {
                Log.d(TAG, "IOException: " + e.getMessage());

            }
            if (!checkRunning()) {
                return;
            }
            closeSocket();

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Logger.t(TAG).d("sleep interrupted");
            }
            if (!checkRunning()) {
                return;
            }
        }


        Logger.t(TAG).d("connected to " + mServerAddress);

        PrintWriter out = new PrintWriter(mSocket.getOutputStream());
        String request = "GET / HTTP/1.1\r\n" + "Host: " + mServerAddress + "\r\n" + "Connection: keep-alive\r\n"
            + "Cache-Control: no-cache\r\n" + "\r\n";
        out.print(request);
        out.flush();

        mBuffer = new MjpegBuffer(mSocket.getInputStream());
//        mBuffer.refill();
//        mBuffer.skipHttpEnd();
    }

    private void readOneFrame() throws IOException {
        // find Content-Length
        mBuffer.refill();
        mBuffer.skipContentLength();

        // read frame length
        int frameLen = mBuffer.scanInteger();
        if (frameLen <= 0) {
            throw new IOException("cannot get Content-Length");
        }


        // skip http header
        mBuffer.skipHttpEnd();

        // read frame
        ByteArrayBuffer buffer = mBufferManager.allocateBuffer(frameLen);
        mBuffer.read(buffer.getBuffer(), 0, frameLen);

        // send to decoder
        mOutputQ.putObject(buffer);
    }
}
