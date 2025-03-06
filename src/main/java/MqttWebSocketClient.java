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
import io.netty.util.concurrent.ScheduledFuture;

import javax.net.ssl.SSLEngine;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class MqttWebSocketClient implements MqttMessageHandlerCallback {
    private boolean mAsynchMode =true;
    private int maxConnectionAttempts=10;
    private String URL = "ws://broker.hivemq.com:8000/mqtt";
    private SslContext sslContext=null;
    final String subprotocol ="mqtt";
    private int mKeepAlive =60;
    private final long mTimeOut =10000;
    private boolean mAutoReconnect =true;
    private boolean mUserDisconnected =false;
    private final ScheduledFuture<?> rescheduleFuture=null;
    private int mRescheduleInterval=5;
    private boolean connected=false;
    private int failedConnections=0;

    private String mUsername = null;
    private String mPassword = "";
    private final String mWillTopic=null;
    private String mWillMessage=null;
    private int mWillQOS=0;
    private boolean mWillRetain=true;
    private boolean mCleanSession=false;
    private ProxyConfig proxyConfig =null;

    private final MqttMessageHandlerCallback mCallback = this;

    private final String TAG = "MqttWebSocketClient";
    private final boolean enableLogging=true;

    private MqttMessageHandler mqttMessageHandler;
    private NioEventLoopGroup group = null;
    private ChannelFuture channelFuture=null;
    private Thread disConnectThread;

    private final SimpleLogger logger = new SimpleLogger();

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
        this.mUsername =username;
        this.mPassword =password;
    }

    public MqttWebSocketClient(String url, SslContext sslContext){
        this.URL=url;
        this.sslContext=sslContext;
    }

    public MqttWebSocketClient(String url, SslContext sslContext, String username, String password){
        this.URL=url;
        this.sslContext=sslContext;
        this.mUsername =username;
        this.mPassword =password;
    }

    public void setUsername(String mUsername){
        this.mUsername = mUsername;
    }
    public void setPassword(String password){
        this.mPassword =password;
    }

    public void setWillTopic(String willtopic){
        if ((willtopic!=null) && (!willtopic.isEmpty())) {
            this.mWillMessage = willtopic;
        }
    }

    public void setWillMessage(String willmessage){
        if ((willmessage!=null) && (!willmessage.isEmpty())) {
            this.mWillMessage = willmessage;
        }
    }

    public void setWillQOS(int qos){
        if ((qos>=0) && (qos<3)){
            mWillQOS=qos;
        }
    }

    public void setKeepAlive(int keepAlive){
        if ((keepAlive>-1) && (keepAlive<=65535)){
            mKeepAlive=keepAlive;
        }
    }

    public void setWillRetain(boolean willretain){
        mWillRetain=willretain;
    }

    public void setCleanSession(boolean cleansession){
        mCleanSession =cleansession;
    }

    public void setProxyConfig(ProxyConfig proxyConfig){
        this.proxyConfig=proxyConfig;
    }

    public void setAutoReconnect(boolean autoReconnect){
        mAutoReconnect=autoReconnect;
    }

    public void setMaxConnectionAttempts(int attempts){
        if (attempts>0) {
            maxConnectionAttempts = attempts;
        }
    }

    public void setReconnectIntervalSeconds(int interval){
        if ((interval>0) && (interval<61)){
            mRescheduleInterval=interval;
        }
    }

    public void setAsynchMode(boolean mode){
        mAsynchMode = mode;
    }

    public void publish(String topic, String payload, int qos, boolean retain){
        if ((connected) && (mqttMessageHandler!=null)){
            mqttMessageHandler.publish(topic, payload, qos, retain);
        }
    }



    public void connect()  {
        log("MqttWebSocketClient()->connect()->start");
        if (connected){return;}
        String parseURL =  parseURL(URL);

        log("ParsedURL=" + parseURL );

        failedConnections=0;
        mUserDisconnected =false;

        do {
            failedConnections++;
            try {
                final URI uri = new URI(parseURL);
                String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
                final String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
                String path = uri.getPath();
                String query = uri.getQuery();
                log("Scheme=" + scheme );
                log("Host=" + host );
                log("Path=" + path );
                log("Query=" + query );
                int parsedPort = uri.getPort();
                if (parsedPort == -1) {
                    if (scheme.startsWith("http")) {
                        parsedPort = scheme.equals("https") ? 443 : 80;
                    }else
                    if (scheme.startsWith("mqtt")) {
                        parsedPort = scheme.equals("mqtts") ? 8883 : 1883;
                    }else
                    if (scheme.startsWith("ws")) {
                        parsedPort = scheme.equals("wss") ? 8084 : 8083; // https://www.emqx.com/en/blog/connect-to-mqtt-broker-with-websocket
                    }
                }
                final int port = parsedPort;
                log("URL=" + parseURL );


                if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
                    System.err.println("Only WS(S) is supported.");
                    return;
                }

                if (group!=null) {
                    log(TAG + "->connect()->Shutting down old EventLoop");
                    group.shutdownGracefully();
                    group=null;
                }

                group = new NioEventLoopGroup();
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 40000);
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
                                    sslHandler.setHandshakeTimeoutMillis(40000);
                                    ch.pipeline().addFirst("ssl", sslHandler);
                                    log("Adding ssl support to pipeline ");
                                }
                                if (proxyConfig!=null){
                                    ch.pipeline().addFirst("proxy", proxyConfig.getProxyHandler());
                                    log("Adding proxy support to pipeline " + proxyConfig.getProxy());
                                }
                                p.addLast(new HttpClientCodec(512, 512, 512));
                                p.addLast(new HttpObjectAggregator(65536));
                                p.addLast(WebSocketClientCompressionHandler.INSTANCE);
                                p.addLast(new WebSocketClientProtocolHandler(WebSocketClientHandshakerFactory.newHandshaker(
                                        uri, WebSocketVersion.V13, subprotocol, true, new DefaultHttpHeaders(),65536)));
                                p.addLast(new MqttWebSocketCodec());
                                p.addLast(new MqttDecoder());
                                p.addLast(MqttEncoder.INSTANCE);
                                p.addLast("idleStateHandler", new IdleStateHandler(mKeepAlive, mKeepAlive, 0, TimeUnit.SECONDS));
                                p.addLast("mqttPingHandler", new MqttPingHandler(mKeepAlive));
                                p.addLast(new HttpResponseHandler());
                                if( (mUsername !=null) && (mPassword !=null) ){
                                    mqttMessageHandler = new MqttMessageHandler(mKeepAlive, mUsername, mPassword);
                                }else {
                                    mqttMessageHandler = new MqttMessageHandler(mKeepAlive);
                                }
                                mqttMessageHandler.setKeepAlive(mKeepAlive);
                                mqttMessageHandler.setWillMessage(mWillMessage);
                                mqttMessageHandler.setWillTopic(mWillTopic);
                                mqttMessageHandler.setWillQOS(mWillQOS);
                                mqttMessageHandler.setWillRetain(mWillRetain);
                                mqttMessageHandler.setCleanSession(mCleanSession);
                                mqttMessageHandler.setCallback(mCallback);
                                p.addLast(mqttMessageHandler);
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
                    //channelFuture.awaitUninterruptibly();
                    channelFuture.await();
                    long waitTime=0;
                    if (channelFuture.isSuccess()){
                        // Nettys connection attempt was initiated, waiting for a server response.
                        do{
                            Thread.sleep(5);
                            waitTime+=5;
                        }while ((!connected) && (waitTime< mTimeOut));
                    }

                    if (!connected){
                        //An error code would be useful here.
                        log("Connection failed to complete within timeout period.");
                        return;
                    }
                    if (!mAsynchMode) {
                        ch.closeFuture().sync(); //We are connected. Execution sits here in Synch Mode
                    }

                    if (!channelFuture.isSuccess()) {
                        Thread.sleep(mRescheduleInterval * 1000L);
                    }
                }
            }catch(InterruptedException IE){
                return;
            }catch (URISyntaxException USE){
                return;
            } finally {
                log("Got to finally");
                log("Connected status=" + connected);
                if (!mAsynchMode) {
                    disConnectThread= new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(30000); // Sleep for 30 seconds
                            } catch (InterruptedException e) {
                                log("Disconnect Thread interrupted.");
                                return;
                            }
                            // If we got here, the sleep finished without interruption, so display an error message and exit
                            log("Timed out waiting for disconnect.");
                        }
                    });
                    if (connected) {
                        log("Starting disconnect Thread.");
                        disConnectThread.start(); // Start the thread
                    }
                    if (group != null) {
                        log("Shutting down EventLoop");
                        group.shutdownGracefully();
                        group = null;
                    }
                    disConnectThread=null;
                }
            }
        } while ((!connected) && (mAutoReconnect) && (!mUserDisconnected) && (failedConnections<maxConnectionAttempts));
        log("MqttWebSocketClient()->connect()->end");
    }//connect



    public void disconnect(){
        log(TAG + "->disconnect()->start");
        if (!connected){return;}
        if( (channelFuture.channel() != null)  && (channelFuture.channel().isActive()) ){
            log(TAG + "->disconnect()->channel active");
            mUserDisconnected=true;
            MqttMessage message = new MqttMessage(new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0));
            channelFuture.channel().writeAndFlush(message).addListener(future -> channelFuture.channel().close());
            log(TAG + "->disconnect()->sent MQTT disconnect message");
            /*
            if (group!=null) {
                log(TAG + "->disconnect()->Shutting down EventLoop");
                group.shutdownGracefully();
                group=null;
            }
             */
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

    @Override
    public void onConnect(String info) {
        log("onConnect fired");
        connected=true;
        failedConnections=0;
        publish("test/topic4", "hello world", 0, false);
    }

    @Override
    public void onDisconnect(String reason) {
        log("onDisconnect()->start");
        log("onDisconnect()->fired, mDisconnected=" + mUserDisconnected);

        connected=false;
        if ((disConnectThread!=null) && (disConnectThread.isAlive())){
            log("onDisconnect()->Interrupting disconnect thread");
            disConnectThread.interrupt();
        }
        disConnectThread=null;

        //if ((mAsynchMode) && (!mUserDisconnected)) {
            if ((mAsynchMode) ) {
            log("onDisconnect()->AsyncMode, user did not disconnect");
            //shutdowngroup();
                if (group != null) {
                    log("onDisconnect()->Shutting down EventLoop");
                    //group.shutdownGracefully();
                    group.shutdownGracefully(0, 5, TimeUnit.SECONDS);
                    //group=null;
                }
            if ((mAutoReconnect) && (!mUserDisconnected) && (failedConnections<maxConnectionAttempts)) {
                log("onDisconnect()->User did not disconnect, calling reconnect");
                connect();
            }
        }
        log("onDisconnect()->end");
    }


    private boolean shutdowngroup(){
        boolean retval=false;
        if (group != null) {
            log("shutdowngroup()->Shutting down EventLoop");
            group.shutdownGracefully(0, 100, TimeUnit.MILLISECONDS);
            try {
                retval = group.awaitTermination(1000, TimeUnit.MILLISECONDS);
            }catch (IllegalStateException ise){
                log("shutdowngroup()->IllegalStateException");
            } catch (InterruptedException e) {
                log("shutdowngroup()->InterruptedException");
            }finally {
                group = null;
            }
        }
        return retval;
    }
}

