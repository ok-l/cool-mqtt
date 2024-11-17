package cool.oooo.mqtt.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface RegularMqttListener {

    /**
     * consume mqtt msg
     *
     * @param topic       topic
     * @param mqttMessage msg
     */
    void consume(String topic, MqttMessage mqttMessage);
}
