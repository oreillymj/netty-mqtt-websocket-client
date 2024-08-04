import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.*;

import java.util.List;
import java.util.logging.Logger;

public class MqttWebSocketCodec extends MessageToMessageCodec<WebSocketFrame, ByteBuf> {

    private final String TAG = "MqttWebSocketCodec";
    private final boolean enableLogging=false;

    private SimpleLogger logger = new SimpleLogger();

    private void log(String data){
        if (enableLogging){
            logger.log(data);
        }
    }


    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        log("encoding");
        out.add(new BinaryWebSocketFrame(msg.retain()));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame  frame, List<Object> out) throws Exception {
        log("decoding");
        if (frame instanceof BinaryWebSocketFrame || frame instanceof ContinuationWebSocketFrame) {
            out.add(frame.content().retain());
        } else if (frame instanceof CloseWebSocketFrame) {
            ctx.close();
        } else if (frame instanceof PingWebSocketFrame) {
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
        } else if (frame instanceof PongWebSocketFrame) {
            // Pong frames can be ignored
        } else {
            throw new UnsupportedOperationException("Unsupported frame type: " + frame.getClass().getName());
        }
    }

}