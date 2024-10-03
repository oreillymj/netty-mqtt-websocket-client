import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.CharsetUtil;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MqttMessageHandler extends SimpleChannelInboundHandler<MqttMessage> {

    Map<Integer, ScheduledFuture> syncMap = Collections.synchronizedMap(new HashMap<>());

    private int keepAlive=60;

    private UUID uuid = UUID.randomUUID();

    private final AtomicInteger nextMessageId = new AtomicInteger(1);

    private final String TAG = "MqttMessageHandler";
    private final boolean enableLogging=true;

    private SimpleLogger logger = new SimpleLogger();

    private String username=null;
    private String password="";

    private void log(String data){
        if (enableLogging){
            logger.log(data);
        }
    }

    MqttMessageHandler(int keepAlive) {
        this.keepAlive = keepAlive;
    }
    MqttMessageHandler(int keepAlive, String username, String password) {
        this.keepAlive = keepAlive;
        this.username = username;
        this.password = password;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // Channel is active now
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        log("MqttMessageHandler->userEventTriggered()->");
        log("MqttMessageHandler->userEventTriggered()-> " + evt.toString());
        if (evt instanceof WebSocketClientProtocolHandler.ClientHandshakeStateEvent) {
            WebSocketClientProtocolHandler.ClientHandshakeStateEvent handshakeEvent =
                    (WebSocketClientProtocolHandler.ClientHandshakeStateEvent) evt;
            if (handshakeEvent == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                log("WebSocket handshake complete. Connecting....");
                connect(ctx.channel());

            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log("MqttMessageHandler->exceptionCaught()->");
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) {
        log("MqttMessageHandler->channelRead0()->start");
        if (msg instanceof MqttMessage) {
            MqttMessageType type =  msg.fixedHeader().messageType();
            log("MqttMessageHandler->channelRead0()->MQTT Message Type=" + type.toString());

            // Handle other MQTT messages here if necessary

            switch (type) {
                case CONNACK:
                    MqttConnAckMessage connAck = (MqttConnAckMessage) msg;
                    if (connAck.variableHeader().connectReturnCode() == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                        log("MqttMessageHandler->channelRead0()->MQTT CONNACK connection accepted, publishing....");
                        //subscribe(ctx, "topic/147b6211-8c2b-4e76-a5c1-4030218e97d0", 0);

                        subscribe(ctx, "test/topic0", 0);
                        subscribe(ctx, "test/topic1", 1);
                        subscribe(ctx, "test/topic2", 2);
                        sendPublish(ctx, "test/topic0", "Hello From MQTT Websocket client - QOS0", 0, false);
                        sendPublish(ctx, "test/topic1", "Hello From MQTT Websocket client - QOS1", 1, false);
                        sendPublish(ctx, "test/topic2", "Hello From MQTT Websocket client - QOS1", 2, false);
                        unsubscribe(ctx, "test/topic0", 0);
                    } else {
                        log("MqttMessageHandler->channelRead0()->MQTT connection failed: " + connAck.variableHeader().connectReturnCode());
                        ctx.close();

                    }
                    break;
                case SUBACK:
                    log("MqttMessageHandler->channelRead0()->MQTT SUBACK received");
                    MqttSubAckMessage subAck = (MqttSubAckMessage) msg;
                    handleSubAck(subAck);
                    break;
                case UNSUBACK:
                    log("MqttMessageHandler->channelRead0()->MQTT UNSUBACK received");
                    MqttUnsubAckMessage unsubAck = (MqttUnsubAckMessage) msg;
                    handleUnSubAck(unsubAck);
                    break;
                case PUBACK:
                    log("MqttMessageHandler->channelRead0()->MQTT PUBACK received");
                    MqttPubAckMessage pubAck = (MqttPubAckMessage) msg;
                    handlePubAck(pubAck);
                    break;
                case PUBREC:
                    log("MqttMessageHandler->channelRead0()->MQTT PUBREC received");
                    MqttPubAckMessage pubrec =  new MqttPubAckMessage(msg.fixedHeader(), (MqttMessageIdVariableHeader) msg.variableHeader());
                    handlePubRec(ctx, pubrec);
                    break;
                case PUBREL:
                    log("MqttMessageHandler->channelRead0()->MQTT PUBREL received");
                    MqttPubAckMessage pubrel = (MqttPubAckMessage) msg;
                    //handlePubRel(ctx, pubrel);
                    break;
                case PUBCOMP:
                    log("MqttMessageHandler->channelRead0()->MQTT PUBCOMP received");
                    MqttPubAckMessage pubcomp = new MqttPubAckMessage(msg.fixedHeader(), (MqttMessageIdVariableHeader) msg.variableHeader());
                    handlePubComp(ctx, pubcomp);
                    break;
                case DISCONNECT:
                    log("MqttMessageHandler->channelRead0()->MQTT DISCONNECT received");
                    break;
                case PUBLISH:
                    log("MqttMessageHandler->channelRead0()->MQTT incoming PUBLISH received");
                    handleIncomingPublish(ctx.channel(), (MqttPublishMessage) msg);
                    break;
            }

        }else{
            log("MqttMessageHandler->channelRead0()->Got some other message type");
        }
    }




    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        // The handler has been added to the channel, initialize any state here if needed
    }


    private void connect(Channel channel){
        log("MqttMessageHandler->connect()->");
        String clientid = "netty_websocket_clientId_" + uuid.toString().substring(0,8);
        // Create MQTT CONNECT message
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                MqttMessageType.CONNECT,
                false,
                MqttQoS.AT_MOST_ONCE,
                false,
                0);

        boolean hasUsername=false;
        boolean hasPassword=false;
        if (username!=null){
            hasUsername=true;
        }
        if ((password!=null) && (password!="")){
            hasPassword=true;
        }
        MqttConnectVariableHeader mqttConnectVariableHeader = new MqttConnectVariableHeader(
                "MQTT",
                4,
                hasUsername,
                hasPassword,
                false,
                0,
                false,
                true,
                this.keepAlive);

        MqttConnectPayload mqttConnectPayload = new MqttConnectPayload(
                clientid,
                null,
                null,
                username,
                password.getBytes());

        MqttConnectMessage mqttConnectMessage = new MqttConnectMessage(
                mqttFixedHeader,
                mqttConnectVariableHeader,
                mqttConnectPayload);

        channel.writeAndFlush(mqttConnectMessage);
    }

    private void sendPublish(ChannelHandlerContext ctx, String topic, String payload, int qos, boolean retain){
        sendPublish(ctx, topic, payload, qos, retain, false, 0);
    }
    private void sendPublish(ChannelHandlerContext ctx, String topic, String payload, int qos, boolean retain, boolean dup, int msgid){
        // Publish a message to 'test/topic'
        //log("MqttMessageHandler->publish()->sending a test publish");
        // Create MQTT PUBLISH message

        MqttQoS  mqttqos= MqttQoS.AT_MOST_ONCE;

        switch (qos) {
            case 0:
                mqttqos = MqttQoS.AT_MOST_ONCE;
                break;

            case 1:
                mqttqos = MqttQoS.AT_LEAST_ONCE;
                break;

            case 2:
                mqttqos = MqttQoS.EXACTLY_ONCE;
                break;
        }

        if (qos==0){
            dup=false;
        }
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBLISH,
                dup,
                mqttqos,
                retain,
                0);

        MqttPublishVariableHeader mqttPublishVariableHeader = new MqttPublishVariableHeader(
                topic,
                getMessageId(msgid).messageId());
        msgid = mqttPublishVariableHeader.packetId();
        int finalmsgid=msgid;
        log("MqttMessageHandler->publish()->sending a test publish to topic - " + topic + " with messageid - " + finalmsgid);
        ByteBuf msgPayload = ctx.channel().alloc().buffer();
        msgPayload.writeBytes(payload.getBytes(CharsetUtil.UTF_8));

        MqttPublishMessage mqttPublishMessage = new MqttPublishMessage(
                mqttFixedHeader,
                mqttPublishVariableHeader,
                msgPayload);
        if (ctx.channel().isActive()) {
            ctx.channel().writeAndFlush(mqttPublishMessage);
            if (qos ==1) {
                ScheduledFuture<?> pubAckFuture = ctx.channel().eventLoop().schedule(()->{
                    pubAckTimeoutReached(ctx, topic,payload,qos, retain, finalmsgid);
                }, 10, TimeUnit.SECONDS); //schedule a resend if a subAck is not received.
                syncMap.put(finalmsgid, pubAckFuture); //keep track of multiple futures in a concurrent Hashmap
            }
            if (qos ==2) {
                ScheduledFuture<?> pubRecFuture = ctx.channel().eventLoop().schedule(()->{
                    pubRecTimeoutReached(ctx, topic,payload,qos, retain, finalmsgid);
                }, 10, TimeUnit.SECONDS); //schedule a resend if a subAck is not received.
                syncMap.put(finalmsgid, pubRecFuture); //keep track of multiple futures in a concurrent Hashmap
            }
        }
    }

    private void handlePubAck(MqttPubAckMessage message){
        if (message==null) {return;}
        int msgId = message.variableHeader().messageId();
        log( "MqttMessageHandler()->handlePubAck()->q=" + message.fixedHeader().qosLevel() + ",msgid=" + msgId );
        ScheduledFuture<?> pubackFuture = syncMap.get(msgId);
        //ScheduledFuture subackFuture = syncMap.get(1); //testing
        if (pubackFuture!=null){
            pubackFuture.cancel(true);
            log( "MqttMessageHandler()->handlePubAck()->removed scheduledFuture for publish msgid " + msgId);
            syncMap.remove(msgId);
        }
    }

    private void handlePubRec(ChannelHandlerContext ctx, MqttPubAckMessage message){
        if (message==null) {return;}
        //cancel the pubrec future
        //send the pubrel message
        //schedule a future to wait for a pubcomp
        int msgId = message.variableHeader().messageId();
        log( "MqttMessageHandler()->handlePubRec()->q=" + message.fixedHeader().qosLevel() + ",msgid=" + msgId );

        ScheduledFuture<?> pubrecFuture = syncMap.get(msgId);
        if (pubrecFuture!=null){
            pubrecFuture.cancel(true);
            log( "MqttMessageHandler()->handlePubRec()->removed scheduledFuture for publish msgid " + msgId);
            syncMap.remove(msgId);
            MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 0);
            MqttMessage mqttPubRelMessage = new MqttMessage(
                    mqttFixedHeader,
                    message.variableHeader(),
                    message.payload());
            if (ctx.channel().isActive()) {
                ctx.channel().writeAndFlush(mqttPubRelMessage);
                log( "MqttMessageHandler()->handlePubRec()->sent PubRel");
                ScheduledFuture<?> pubCompFuture = ctx.channel().eventLoop().schedule(()->{
                    pubCompTimeoutReached(ctx, msgId);
                }, 10, TimeUnit.SECONDS); //schedule a resend if a subAck is not received.
                syncMap.put(msgId, pubCompFuture); //keep track of multiple futures in a concurrent Hashmap
            }
        }
    }

    private void handlePubRel(ChannelHandlerContext ctx, MqttPubAckMessage message){
        if (message==null) {return;}
        int msgId = message.variableHeader().messageId();
        log( "MqttMessageHandler()->handlePubRel()->q=" + message.fixedHeader().qosLevel() + ",msgid=" + msgId );

        ScheduledFuture<?> pubackFuture = syncMap.get(msgId);
        //ScheduledFuture subackFuture = syncMap.get(1); //testing
        if (pubackFuture!=null){
            pubackFuture.cancel(true);
            log( "MqttMessageHandler()->handlePubRel()->removed scheduledFuture for publish msgid " + msgId);
            syncMap.remove(msgId);
        }
    }

    private void handlePubComp(ChannelHandlerContext ctx, MqttPubAckMessage message){
        if (message==null) {return;}
        int msgId = message.variableHeader().messageId();
        log( "MqttMessageHandler()->handlePubComp()->q=" + message.fixedHeader().qosLevel() + ",msgid=" + msgId );

        ScheduledFuture<?> pubRelFuture = syncMap.get(msgId);
        //ScheduledFuture subackFuture = syncMap.get(1); //testing
        if (pubRelFuture!=null){
            pubRelFuture.cancel(true);
            log( "MqttMessageHandler()->handlePubComp()->removed scheduledFuture for publish msgid " + msgId);
            syncMap.remove(msgId);
        }
    }

    private void pubAckTimeoutReached(ChannelHandlerContext ctx, String topic, String payload, int qos, boolean retain, int msgId){
        log( "MqttMessageHandler()->pubAckTimeoutReached()->msgid=" + msgId);
        syncMap.remove(msgId); //remove the old ScheduledFuture reference from the Hashmap
        //Resend the publish message if no puback received.
        sendPublish(ctx,topic, payload,qos, retain,true, msgId);
    }



    private void pubRecTimeoutReached(ChannelHandlerContext ctx, String topic, String payload, int qos, boolean retain, int msgId){
        log( "MqttMessageHandler()->pubRecTimeoutReached()->msgid=" + msgId);
        syncMap.remove(msgId); //remove the old ScheduledFuture reference from the Hashmap
        sendPublish(ctx,topic,payload,qos,retain,true,msgId);
    }

    private void pubCompTimeoutReached(ChannelHandlerContext ctx, int msgId){
        log( "MqttMessageHandler()->pubCompTimeoutReached()->msgid=" + msgId);
        //TODO - What is the correct behaviour if this occurs? -- https://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718058
        //syncMap.remove(msgId); //remove the old ScheduledFuture reference from the Hashmap
        //sendPublish(ctx,topic,payload,qos,retain,true,msgId);
    }


    private void subscribe(ChannelHandlerContext ctx, String topic, int qos){
        subscribe(ctx, topic, qos, false,0);
    }
    private void subscribe(ChannelHandlerContext ctx, String topic, int qos, boolean dup, int msgid){

        byte topic_array[] = topic.getBytes(CharsetUtil.UTF_8);
        String utf_topic = new String(topic_array);

        MqttQoS mqttqos= MqttQoS.AT_MOST_ONCE;

        switch (qos) {
            case 0:
                mqttqos = MqttQoS.AT_MOST_ONCE;
                break;

            case 1:
                mqttqos = MqttQoS.AT_LEAST_ONCE;
                break;

            case 2:
                mqttqos = MqttQoS.EXACTLY_ONCE;
                break;
        }


        // Create a new MqttFixedHeader
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                MqttMessageType.SUBSCRIBE, // Message Type
                dup, // DUP flag
                MqttQoS.AT_LEAST_ONCE, // Quality of Service Level must 1 as per spec - https://stanford-clark.com/MQTT/#subscribe / http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718064

                false, // Retain
                0); // Remaining Length
        // Create a new MqttMessageIdVariableHeader

        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = getMessageId(msgid);
        int finalmsgid = mqttMessageIdVariableHeader.messageId();


        // Create a new MqttSubscribePayload
        MqttTopicSubscription mqttTopicSubscription = new MqttTopicSubscription(
                utf_topic, // Topic to subscribe
                mqttqos); // Quality of Service Level
        MqttSubscribePayload mqttSubscribePayload = new MqttSubscribePayload(Collections.singletonList(mqttTopicSubscription));
        // Create a new MqttSubscribeMessage

        MqttSubscribeMessage mqttSubscribeMessage = new MqttSubscribeMessage(
                mqttFixedHeader,
                mqttMessageIdVariableHeader,
                mqttSubscribePayload);
        // Print out the MqttSubscribeMessage

        if (ctx.channel().isActive()) {
            log("Subscribing on topic - " + topic + " with messageid - " + finalmsgid);
            ChannelFuture subscribeFuture  = ctx.writeAndFlush(mqttSubscribeMessage);
            ScheduledFuture<?> subAckFuture = ctx.channel().eventLoop().schedule(()->{
                subAckTimeoutReached(ctx, topic,qos, finalmsgid);
            }, 10, TimeUnit.SECONDS); //schedule a resend if a subAck is not received.
            syncMap.put(finalmsgid, subAckFuture); //keep track of multiple futures in a concurrent Hashmap
        }

    }

    private void handleSubAck(MqttSubAckMessage message){
        if (message==null) {return;}
        int msgId = message.variableHeader().messageId();
        log( "MqttMessageHandler()->handleSubAck()->q=" + message.payload().grantedQoSLevels() + ",msgid=" + msgId );
        ScheduledFuture<?> subackFuture = syncMap.get(msgId);
        //ScheduledFuture subackFuture = syncMap.get(1); //testing
        if (subackFuture!=null){
            subackFuture.cancel(true);
            log( "MqttMessageHandler()->handleSubAck()->removed scheduledFuture for subscribe msgid " + msgId);
            syncMap.remove(msgId);
        }
    }

    private void subAckTimeoutReached(ChannelHandlerContext ctx, String topic, int qos,  int msgId){
        log( "MqttMessageHandler()->subAckTimeoutReached()->msgid=" + msgId);
        syncMap.remove(msgId); //remove the old ScheduledFuture reference from the Hashmap
        //Resend the subscribe message if no suback received.
        subscribe(ctx,topic,qos,true, msgId);
    }

    private void unsubscribe(ChannelHandlerContext ctx, String topic, int qos){
        unsubscribe(ctx, topic, qos, false,0);
    }
    private void unsubscribe(ChannelHandlerContext ctx, String topic, int qos, boolean dup, int msgid){
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttMessageIdVariableHeader variableHeader = getMessageId(msgid);
        MqttUnsubscribePayload payload = new MqttUnsubscribePayload(Collections.singletonList(topic));
        MqttUnsubscribeMessage mqttUnSubscribeMessage = new MqttUnsubscribeMessage(fixedHeader, variableHeader, payload);
        int finalmsgid = variableHeader.messageId();
        if (ctx.channel().isActive()) {
            log("UnSubscribing on topic - " + topic + " with messageid - " + finalmsgid);
            ChannelFuture subscribeFuture  = ctx.writeAndFlush(mqttUnSubscribeMessage);
            ScheduledFuture<?> unSubAckFuture = ctx.channel().eventLoop().schedule(()->{
                unSubAckTimeoutReached(ctx, topic,qos, finalmsgid);
            }, 10, TimeUnit.SECONDS); //schedule a resend if a subAck is not received.
            syncMap.put(finalmsgid, unSubAckFuture); //keep track of multiple futures in a concurrent Hashmap
        }

    }

    private void handleUnSubAck(MqttUnsubAckMessage message){
        if (message==null) {return;}
        int msgId = message.variableHeader().messageId();
        log( "MqttMessageHandler()->handleUnSubAck()->msgid=" + msgId );
        ScheduledFuture<?> unSubackFuture = syncMap.get(msgId);
        //ScheduledFuture subackFuture = syncMap.get(1); //testing
        if (unSubackFuture!=null){
            unSubackFuture.cancel(true);
            log( "MqttMessageHandler()->handleUnSubAck()->removed scheduledFuture for unsubscribe msgid " + msgId);
            syncMap.remove(msgId);
        }
    }

    private void unSubAckTimeoutReached(ChannelHandlerContext ctx, String topic, int qos,  int msgId){
        log( "MqttMessageHandler()->unSubAckTimeoutReached()->msgid=" + msgId);
        syncMap.remove(msgId); //remove the old ScheduledFuture reference from the Hashmap
        //Resend the subscribe message if no suback received.
        unsubscribe(ctx,topic,qos,true, msgId);
    }



    private void handleIncomingPublish(Channel channel, MqttPublishMessage message) {

        if (message==null) {return;}
        log( "MqttMessageHandler()->handleIncomingPublish()->q=" + message.fixedHeader().qosLevel().value() + ",msgid=" + message.variableHeader().packetId() );
        switch (message.fixedHeader().qosLevel()) {
            case AT_MOST_ONCE:
                log("MqttMessageHandler()->handleIncomingPublish()->QOS 0 - Got publish on topic - " + message.variableHeader().topicName() + " payload=" +message.payload().toString(StandardCharsets.UTF_8));
                break;
            case AT_LEAST_ONCE:
                log("MqttMessageHandler()->handleIncomingPublish()->QOS 1 - Got publish on topic - " + message.variableHeader().topicName() + " payload=" +message.payload().toString(StandardCharsets.UTF_8));
                //These need a PUBACK
                break;

            case EXACTLY_ONCE:
                log("MqttMessageHandler()->handleIncomingPublish()->QOS 2 - Got publish on topic - " + message.variableHeader().topicName() + " payload=" +message.payload().toString(StandardCharsets.UTF_8));
                //These need a PUBACK
                break;
        }
    }


    private void showMapinfo(){
        log( "MqttMessageHandler()->showMapinfo()->syncMap.size " + syncMap.size());
        log( "MqttMessageHandler()->showMapinfo()->syncMap.keySet " + syncMap.keySet());
    }

    private synchronized MqttMessageIdVariableHeader getNewMessageId() {
        this.nextMessageId.compareAndSet(0xffff, 1);
        return MqttMessageIdVariableHeader.from(this.nextMessageId.getAndIncrement());
    }


    private synchronized MqttMessageIdVariableHeader getMessageId(int msgid) {
        if (msgid==0) {
            this.nextMessageId.compareAndSet(0xffff, 1);
            return MqttMessageIdVariableHeader.from(this.nextMessageId.getAndIncrement());
        }else{
            return MqttMessageIdVariableHeader.from(msgid);
        }
    }
}
