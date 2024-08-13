import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.CharsetUtil;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class MqttMessageHandler extends SimpleChannelInboundHandler<MqttMessage> {

    private int keepAlive=60;

    private UUID uuid = UUID.randomUUID();

    private final AtomicInteger nextMessageId = new AtomicInteger(1);

    private final String TAG = "MqttMessageHandler";
    private final boolean enableLogging=true;

    private SimpleLogger logger = new SimpleLogger();

    private void log(String data){
        if (enableLogging){
            logger.log(data);
        }
    }

    MqttMessageHandler(int keepAlive) {
        this.keepAlive = keepAlive;
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
                        sendPublish(ctx.channel(), "test/topic0", "Hello From MQTT Websocket client - QOS0", 0);
                        sendPublish(ctx.channel(), "test/topic1", "Hello From MQTT Websocket client - QOS1", 1);
                    } else {
                        log("MQTT connection failed: " + connAck.variableHeader().connectReturnCode());
                    }
                    break;
                case PUBACK:
                    log("MqttMessageHandler->channelRead0()->MQTT PUBACK received");
                    break;
                case SUBACK:
                    log("MqttMessageHandler->channelRead0()->MQTT SUBACK received");
                    break;
                case PUBREC:
                    log("MqttMessageHandler->channelRead0()->MQTT PUBREC received");
                    break;
                case PUBREL:
                    log("MqttMessageHandler->channelRead0()->MQTT PUBREL received");
                    break;
                case PUBCOMP:
                    log("MqttMessageHandler->channelRead0()->MQTT PUBCOMP received");
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

        MqttConnectVariableHeader mqttConnectVariableHeader = new MqttConnectVariableHeader(
                "MQTT",
                4,
                false,
                false,
                false,
                0,
                false,
                true,
                this.keepAlive);

        MqttConnectPayload mqttConnectPayload = new MqttConnectPayload(
                clientid,
                null,
                null,
                null,
                "".getBytes());

        MqttConnectMessage mqttConnectMessage = new MqttConnectMessage(
                mqttFixedHeader,
                mqttConnectVariableHeader,
                mqttConnectPayload);

        channel.writeAndFlush(mqttConnectMessage);
    }

    private void sendPublish(Channel channel, String topic, String payload, int qos){
        // Publish a message to 'test/topic'
        log("MqttMessageHandler->publish()->sending a test publish");
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

        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBLISH,
                false,
                mqttqos,
                false,
                0);

        MqttPublishVariableHeader mqttPublishVariableHeader = new MqttPublishVariableHeader(
                topic,
                getNewMessageId().messageId());

        ByteBuf msgPayload = channel.alloc().buffer();
        msgPayload.writeBytes(payload.getBytes(CharsetUtil.UTF_8));

        MqttPublishMessage mqttPublishMessage = new MqttPublishMessage(
                mqttFixedHeader,
                mqttPublishVariableHeader,
                msgPayload);

        channel.writeAndFlush(mqttPublishMessage);
    }



    private void subscribe(ChannelHandlerContext ctx, String topic, int qos){

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
                false, // DUP flag
                MqttQoS.AT_LEAST_ONCE, // Quality of Service Level
                false, // Retain
                0); // Remaining Length
        // Create a new MqttMessageIdVariableHeader
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = getNewMessageId();
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
            log("Subscribing on topic - " + topic);
            ctx.writeAndFlush(mqttSubscribeMessage);
        }

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


    private MqttMessageIdVariableHeader getNewMessageId() {
        this.nextMessageId.compareAndSet(0xffff, 1);
        return MqttMessageIdVariableHeader.from(this.nextMessageId.getAndIncrement());
    }

}
