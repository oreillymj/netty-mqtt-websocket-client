public class Main {
    public static void main(String[] args) {

        // https://nikoskatsanos.com/blog/2022/01/netty-websocket-ssl/?utm_source=pocket_reader

        System.out.println("Web socket testing");


        //WebSocketMqttClient cloud_hivemq = new WebSocketMqttClient("ws://broker.hivemq.com:8000/mqtt");
        //cloud_hivemq.start();


        //WebSocketMqttClient cloud_emqx = new WebSocketMqttClient("ws://broker.emqx.io:8083/mqtt");
        //cloud_emqx.start();


        MqttWebSocketClient local_mosquitto = new MqttWebSocketClient("ws://localhost:9001/mqtt");
        local_mosquitto.start();

    }
}