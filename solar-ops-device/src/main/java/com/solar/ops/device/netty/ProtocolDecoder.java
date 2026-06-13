package com.solar.ops.device.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class ProtocolDecoder extends ByteToMessageDecoder {

    private static final int MIN_FRAME_LENGTH = 5;
    private static final int MAX_FRAME_LENGTH = 256;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        in.markReaderIndex();

        if (in.readableBytes() < MIN_FRAME_LENGTH) {
            in.resetReaderIndex();
            return;
        }

        int byteCountIndex = in.readerIndex() + 2;
        if (in.readableBytes() < byteCountIndex + 1) {
            in.resetReaderIndex();
            return;
        }

        int byteCount = in.getUnsignedByte(byteCountIndex);
        int totalLength = 3 + byteCount + 2;

        totalLength = Math.min(totalLength, MAX_FRAME_LENGTH);

        if (in.readableBytes() < totalLength) {
            in.resetReaderIndex();
            return;
        }

        ByteBuf frame = in.readBytes(totalLength);
        out.add(frame);
    }
}
