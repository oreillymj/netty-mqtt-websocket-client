import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;

public final class MqttSubscribeFixedHeader {


    public static MqttFixedHeader getFixedHeader(){
        // Create a new MqttFixedHeader
        return  getFixedHeader(false);
    }


    public static MqttFixedHeader getFixedHeader(boolean duplicate){
        // Create a new MqttFixedHeader
        return  new MqttFixedHeader(
                MqttMessageType.SUBSCRIBE, // Message Type
                duplicate, // DUP flag - Oasis spec says this must be false - http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718064
                MqttQoS.AT_LEAST_ONCE, // Quality of Service Level must 1 as per spec - https://stanford-clark.com/MQTT/#subscribe / http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718064
                false, // Retain
                0); // Remaining Length
    }
}
