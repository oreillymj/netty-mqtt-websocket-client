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

public class MqttMessageHandler extends SimpleChannelInboundHandler<MqttMessage> {

    private int keepAlive=60;

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
        System.out.println("MqttMessageHandler->userEventTriggered()->");
        System.out.println("MqttMessageHandler->userEventTriggered()-> " + evt.toString());
        if (evt instanceof WebSocketClientProtocolHandler.ClientHandshakeStateEvent) {
            WebSocketClientProtocolHandler.ClientHandshakeStateEvent handshakeEvent =
                    (WebSocketClientProtocolHandler.ClientHandshakeStateEvent) evt;
            if (handshakeEvent == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                System.out.println("WebSocket handshake complete. Connecting....");

                connect(ctx.channel());


/*
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
                        false,
                        60);

                MqttConnectPayload mqttConnectPayload = new MqttConnectPayload(
                        "netty_websocket_clientId",
                        null,
                        null,
                        null,
                        "".getBytes());

                MqttConnectMessage mqttConnectMessage = new MqttConnectMessage(
                        mqttFixedHeader,
                        mqttConnectVariableHeader,
                        mqttConnectPayload);

                ctx.writeAndFlush(mqttConnectMessage);*/
            }
        }
    }

/*    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("MqttMessageHandler->exceptionCaught()->");
        cause.printStackTrace();
        ctx.close();
    }*/

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) {
        System.out.println("MqttMessageHandler->channelRead0()->");
        if (msg instanceof MqttMessage) {
            MqttMessageType type =  msg.fixedHeader().messageType();
            System.out.println("MqttMessageHandler->channelRead0()->MQTT Message Type=" + type.toString());

            // Handle other MQTT messages here if necessary

            switch (type) {
                case CONNACK:
                    MqttConnAckMessage connAck = (MqttConnAckMessage) msg;
                    if (connAck.variableHeader().connectReturnCode() == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                        System.out.println("MqttMessageHandler->channelRead0()->MQTT CONNACK connection accepted, publishing....");
                        //subscribe(ctx, "topic/147b6211-8c2b-4e76-a5c1-4030218e97d0", 0);


                        sendPublish(ctx.channel(), "test/topic", "Hello From MQTT Websocket client", 0);
                        sendPublish(ctx.channel(), "test/topic2", "Hello From MQTT Websocket client", 0);


                        subscribe(ctx, "topic/147b6211-8c2b-4e76-a5c1-4030218e97d0", 1);

                    } else {
                        System.out.println("MQTT connection failed: " + connAck.variableHeader().connectReturnCode());
                    }
                    break;
                case PUBACK:
                    System.out.println("MqttMessageHandler->channelRead0()->MQTT PUBACK received");
                    break;
                case SUBACK:
                    System.out.println("MqttMessageHandler->channelRead0()->MQTT SUBACK received");
                    break;
                case PUBREC:
                    System.out.println("MqttMessageHandler->channelRead0()->MQTT PUBREC received");
                    break;
                case PUBREL:
                    System.out.println("MqttMessageHandler->channelRead0()->MQTT PUBREL received");
                    break;
                case PUBCOMP:
                    System.out.println("MqttMessageHandler->channelRead0()->MQTT PUBCOMP received");
                    break;
                case DISCONNECT:
                    System.out.println("MqttMessageHandler->channelRead0()->MQTT DISCONNECT received");
                    break;
                case PUBLISH:
                    System.out.println("MqttMessageHandler->channelRead0()->MQTT incoming PUBLISH received");
                    handleIncomingPublish(ctx.channel(), (MqttPublishMessage) msg);
                    break;
            }

        }else{
            System.out.println("MqttMessageHandler->channelRead0()->Got some other message type");
        }
    }




    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        // The handler has been added to the channel, initialize any state here if needed
    }


    private void connect(Channel channel){
        System.out.println("MqttMessageHandler->connect()->");
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
                "netty_websocket_clientId",
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
        System.out.println("MqttMessageHandler->publish()->sending a test publish");
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
                0);

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
        /*
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, mqttqos, false, 0);
        MqttTopicSubscription subscription = new MqttTopicSubscription(utf_topic, mqttqos);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(12345);
        MqttSubscribePayload payload = new MqttSubscribePayload(Collections.singletonList(subscription));
        MqttSubscribeMessage mqttSubscribeMessage = new MqttSubscribeMessage(fixedHeader, variableHeader, payload);
        System.out.println("Subscribing on topic - " + topic);
        */

        // Create a new MqttFixedHeader
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                MqttMessageType.SUBSCRIBE, // Message Type
                false, // DUP flag
                mqttqos, // Quality of Service Level
                false, // Retain
                0); // Remaining Length
        // Create a new MqttMessageIdVariableHeader
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(12345);
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
            System.out.println("Subscribing on topic - " + topic);
            ctx.writeAndFlush(mqttSubscribeMessage);
        }

    }

    private void subscribe(Channel channel, String topic, int qos){

        byte topic_array[] = topic.getBytes(StandardCharsets.UTF_8);
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
        /*
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, mqttqos, false, 0);
        MqttTopicSubscription subscription = new MqttTopicSubscription(utf_topic, mqttqos);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(12345);
        MqttSubscribePayload payload = new MqttSubscribePayload(Collections.singletonList(subscription));
        MqttSubscribeMessage mqttSubscribeMessage = new MqttSubscribeMessage(fixedHeader, variableHeader, payload);
        System.out.println("Subscribing on topic - " + topic);
        */

        // Create a new MqttFixedHeader
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                MqttMessageType.SUBSCRIBE, // Message Type
                false, // DUP flag
                mqttqos, // Quality of Service Level
                false, // Retain
                0); // Remaining Length
        // Create a new MqttMessageIdVariableHeader
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(12345);
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

        if (channel.isActive()) {
            System.out.println("Subscribing on topic - " + topic);
            channel.writeAndFlush(mqttSubscribeMessage);
        }

    }


    private void handleIncomingPublish(Channel channel, MqttPublishMessage message) {

        if (message==null) {return;}
        System.out.println( "MqttMessageHandler()->handleIncomingPublish, q=" + message.fixedHeader().qosLevel().value() + ",msgid=" + message.variableHeader().packetId() );
        switch (message.fixedHeader().qosLevel()) {
            case AT_MOST_ONCE:
                System.out.println("Got publish on topic - " + message.variableHeader().topicName() + " payload=" +message.payload().toString(StandardCharsets.UTF_8));
                break;
            case AT_LEAST_ONCE:
                System.out.println("Got publish on topic - " + message.variableHeader().topicName() + " payload=" +message.payload().toString(StandardCharsets.UTF_8));
                //These need a PUBACK
                break;

            case EXACTLY_ONCE:
                System.out.println("Got publish on topic - " + message.variableHeader().topicName() + " payload=" +message.payload().toString(StandardCharsets.UTF_8));
                //These need a PUBACK
                break;
        }
    }


}
