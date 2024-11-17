package cool.oooo.mqtt.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;


public interface MqttListener {

    /**
     * consume mqtt msg
     *
     * @param mqttMessage msg
     */
    void consume(MqttMessage mqttMessage);
}
