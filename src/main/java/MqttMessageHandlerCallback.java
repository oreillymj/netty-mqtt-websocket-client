public interface MqttMessageHandlerCallback {
    void onConnect(String info);
    void onDisconnect(String reason);
}
