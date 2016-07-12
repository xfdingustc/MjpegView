package com.xfdingustc.mjpegview.library.mina;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * Created by Xiaofei on 2016/7/12.
 */
public class MjpegEncoder implements ProtocolEncoder {
    @Override
    public void encode(IoSession ioSession, Object message, ProtocolEncoderOutput protocolEncoderOutput) throws Exception {
        String msg = (String)message;
        IoBuffer buffer = IoBuffer.allocate(256).setAutoExpand(true);
        buffer.put(msg.getBytes());
        buffer.flip();

        protocolEncoderOutput.write(buffer);
        protocolEncoderOutput.flush();
    }

    @Override
    public void dispose(IoSession ioSession) throws Exception {

    }
}
