import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.util.CharsetUtil;

public class HttpResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final String TAG = "HttpResponseHandler";
    private final boolean enableLogging=false;

    private SimpleLogger logger = new SimpleLogger();

    private void log(String data){
        if (enableLogging){
            logger.log(data);
        }
    }
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
        log("HttpResponseHandler->channelRead0()->");
        if (msg.status().code() != 101) { // 101 Switching Protocols is expected for WebSocket upgrade
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (status=" + msg.status() +
                            ", content=" + msg.content().toString(CharsetUtil.UTF_8) + ')');
        } else {
            // If it's a 101 Switching Protocols response, we just let it pass through
            ctx.fireChannelRead(msg.retain());
        }
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        log("HttpResponseHandler->userEventTriggered()->");
        log("HttpResponseHandler->userEventTriggered()-> " + evt.toString());
        if (evt instanceof WebSocketClientProtocolHandler.ClientHandshakeStateEvent) {
            WebSocketClientProtocolHandler.ClientHandshakeStateEvent handshakeEvent =
                    (WebSocketClientProtocolHandler.ClientHandshakeStateEvent) evt;
            switch (handshakeEvent) {
                case HANDSHAKE_ISSUED:
                    log("WebSocket Handshake Issued.");
                    break;
                case HANDSHAKE_COMPLETE:
                    log("WebSocket Handshake complete");
                    // Fire the event to let other handlers know the handshake is complete
                    ctx.fireUserEventTriggered(evt);
                case HANDSHAKE_TIMEOUT:
                    log("WebSocket Handshake Timeout.");
                    break;
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }






    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log("HttpResponseHandler->exceptionCaught()->");
        log("HttpResponseHandler->exceptionCaught()->Reason=" + cause.getMessage());
        if (cause instanceof WebSocketClientHandshakeException){
            cause.printStackTrace();
            log("HttpResponseHandler->exceptionCaught()->Reason=" + cause.getMessage());
            ctx.close();
        }




    }

}

