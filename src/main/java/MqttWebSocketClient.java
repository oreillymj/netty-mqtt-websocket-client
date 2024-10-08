import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;

import javax.net.ssl.SSLEngine;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class MqttWebSocketClient {


    private String URL = "ws://broker.hivemq.com:8000/mqtt";
    private SslContext sslContext=null;
    final String subprotocol ="mqtt";
    final int keepAlive=60;

    private String username=null;
    private String password="";

    private ProxyConfig proxyConfig =null;

    private final String TAG = "MqttWebSocketClient";
    private final boolean enableLogging=true;

    private NioEventLoopGroup group = null;
    private ChannelFuture channelFuture=null;

    private SimpleLogger logger = new SimpleLogger();

    private void log(String data){
        if (enableLogging){
            logger.log(data);
        }
    }


    public MqttWebSocketClient(){

    }

    public MqttWebSocketClient(String url){
        this.URL=url;
    }

    public MqttWebSocketClient(String url,String username, String password){
        this.URL=url;
        this.username=username;
        this.password=password;
    }

    public MqttWebSocketClient(String url, SslContext sslContext){
        this.URL=url;
        this.sslContext=sslContext;
    }

    public MqttWebSocketClient(String url, SslContext sslContext, String username, String password){
        this.URL=url;
        this.sslContext=sslContext;
        this.username=username;
        this.password=password;
    }

    public void setUsername(String username){
        this.username=username;
    }
    public void setPassword(String password){
        this.password=password;
    }

    public void setProxyConfig(ProxyConfig proxyConfig){
        this.proxyConfig=proxyConfig;
    }

    public  void start()  {
        //final SslContext sslCtx;
        final int port;
        final String host;
        URI uri;





        String parseURL =  parseURL(URL);

        log("ParsedURL=" + parseURL );

        try {
            uri = new URI(parseURL);
            String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
            host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
            String path = uri.getPath();
            String query = uri.getQuery();
            log("Scheme=" + scheme );
            log("Host=" + host );
            log("Path=" + path );
            log("Query=" + query );
            int parsedPort =0;
            if (uri.getPort() == -1) {
                if (scheme.startsWith("http")) {
                    parsedPort = scheme.equals("https") ? 443 : 80;
                }else
                if (scheme.startsWith("mqtt")) {
                    parsedPort = scheme.equals("mqtts") ? 8883 : 1883;
                }else
                if (scheme.startsWith("ws")) {
                    parsedPort = scheme.equals("wss") ? 8084 : 8083; // https://www.emqx.com/en/blog/connect-to-mqtt-broker-with-websocket?utm_source=pocket_saves
                }
            } else {
                parsedPort = uri.getPort();
            }
            port = parsedPort;
            log("URL=" + parseURL );


            if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
                System.err.println("Only WS(S) is supported.");
                return;
            }
            group = new NioEventLoopGroup();
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        public void initChannel(Channel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslContext!=null){
                                Consumer<SSLEngine> sslEngineConsumer=(engine)->{};
                                SSLEngine engine = sslContext.newEngine(ch.alloc(), host, port);
                                sslEngineConsumer.accept(engine);
                                SslHandler sslHandler = new SslHandler(engine);
                                sslHandler.setHandshakeTimeoutMillis(60000);
                                ch.pipeline().addFirst("ssl", sslHandler);
                            }
                            if (proxyConfig!=null){
                                ch.pipeline().addFirst("proxy", proxyConfig.getProxyHandler());
                            }
                            p.addLast(new HttpClientCodec(512, 512, 512));
                            p.addLast(new HttpObjectAggregator(65536));
                            p.addLast(WebSocketClientCompressionHandler.INSTANCE);
                            p.addLast(new WebSocketClientProtocolHandler(WebSocketClientHandshakerFactory.newHandshaker(
                                    uri, WebSocketVersion.V13, subprotocol, true, new DefaultHttpHeaders(),65536)));
                            p.addLast(new MqttWebSocketCodec());
                            p.addLast(new MqttDecoder());
                            p.addLast(MqttEncoder.INSTANCE);
                            p.addLast("idleStateHandler", new IdleStateHandler(keepAlive, keepAlive, 0, TimeUnit.SECONDS));
                            p.addLast("mqttPingHandler", new MqttPingHandler(keepAlive));
                            p.addLast(new HttpResponseHandler());
                            if( (username!=null) && (password!=null) ){
                                p.addLast(new MqttMessageHandler(keepAlive,username,password));
                            }else {
                                p.addLast(new MqttMessageHandler(keepAlive));
                            }
                        }
                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                            log("Exception caught initializing channel");
                            cause.printStackTrace();
                        }
                    });
            log("Connecting to " + host + ":" + port);
            synchronized (bootstrap) {
                channelFuture = bootstrap.connect(host, port);
                Channel ch = channelFuture.channel();
                channelFuture.await();
                ch.closeFuture().sync();
            }
        }catch(InterruptedException IE){
            return;
        }catch (URISyntaxException USE){
            return;
        } finally {
            if (group!=null) {
                log("Shutting down EventLoop");
                group.shutdownGracefully();
                group=null;
            }
        }
    }//start

    public void disConnect(){
        log(TAG + "->disconnecting");
        if( (channelFuture.channel() != null)  && (channelFuture.channel().isActive()) ){
            MqttMessage message = new MqttMessage(new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0));
            channelFuture.channel().writeAndFlush(message).addListener(future -> channelFuture.channel().close());
            if (group!=null) {
                log("Shutting down EventLoop");
                group.shutdownGracefully();
            }
        }
    }


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

