import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

public final class MqttWebSocketClient {


    private String URL = "ws://broker.hivemq.com:8000/mqtt";
    final String subprotocol ="mqtt";
    final int keepAlive=60;


    public MqttWebSocketClient(){

    }

    public MqttWebSocketClient(String url){
        this.URL=url;
    }

    public  void start()  {
        final SslContext sslCtx;
        int port=0;
        final String host;
        URI uri;
        EventLoopGroup group = null;

        String parseURL =  parseURL(URL);

        System.out.println("ParsedURL=" + parseURL );

        try {
            uri = new URI(parseURL);
            String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
            host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();

            if (uri.getPort() == -1) {
                if (scheme.startsWith("http")) {
                    port = scheme.equals("https") ? 443 : 80;
                }else
                if (scheme.startsWith("mqtt")) {
                    port = scheme.equals("mqtts") ? 8883 : 1883;
                }else
                if (scheme.startsWith("ws")) {
                    port = scheme.equals("wss") ? 8084 : 8083; // https://www.emqx.com/en/blog/connect-to-mqtt-broker-with-websocket?utm_source=pocket_saves
                }
                System.out.println("URL=" + parseURL + ":" + port);
            } else {
                port = uri.getPort();
                System.out.println("URL=" + parseURL );
            }




            if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
                System.err.println("Only WS(S) is supported.");
                return;
            }


            group = new NioEventLoopGroup();

            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        public void initChannel(Channel ch) {
                            ChannelPipeline p = ch.pipeline();


                            p.addLast(new HttpClientCodec(512, 512, 512));
                            p.addLast(new HttpObjectAggregator(65536));
                            p.addLast(WebSocketClientCompressionHandler.INSTANCE);
                            p.addLast(new WebSocketClientProtocolHandler(WebSocketClientHandshakerFactory.newHandshaker(
                                    uri, WebSocketVersion.V13, subprotocol, true, new DefaultHttpHeaders(false))));
                            p.addLast(new MqttWebSocketCodec());
                            p.addLast(new MqttDecoder());
                            p.addLast(MqttEncoder.INSTANCE);
                            p.addLast("idleStateHandler", new IdleStateHandler(keepAlive, keepAlive, 0, TimeUnit.SECONDS));
                            p.addLast("mqttPingHandler", new MqttPingHandler(keepAlive));
                            p.addLast(new HttpResponseHandler());
                            p.addLast(new MqttMessageHandler(keepAlive));
                        }
                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                            System.out.println("Exception caught initializing channel");
                            cause.printStackTrace();
                        }
                    });

            Channel ch = b.connect(host, port).sync().channel();
            ch.closeFuture().sync();
        }catch(InterruptedException IE){
            return;
        }catch (URISyntaxException USE){
            return;
        } finally {
            if (group!=null) {
                group.shutdownGracefully();
            }
        }
    }//start


    private String parseURL(String url){
        String retval = url.toLowerCase();
        boolean isCorrect = url.endsWith("/mqtt");
        boolean trailingmqttslash = url.endsWith("/mqtt/");
        //isCorrect = isCorrect || trailingmqttslash;
        if (!isCorrect)  {
            if (trailingmqttslash){
                retval = retval.replace("/mqtt/", "/mqtt");
            }else
            if( url.endsWith("/")){
                retval = retval + "mqtt";
            }else{
                retval = retval + "/mqtt";
            }


        }
        return  retval;
    }
}

