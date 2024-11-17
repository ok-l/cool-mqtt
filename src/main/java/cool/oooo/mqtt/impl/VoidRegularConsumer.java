package cool.oooo.mqtt.impl;

import cool.oooo.mqtt.mqtt.RegularMqttListener;
import cool.oooo.mqtt.mqtt.RegularMqttSubscription;
import org.eclipse.paho.client.mqttv3.MqttMessage;


@RegularMqttSubscription(topic = "/+/+/test/topic")
public class VoidRegularConsumer implements RegularMqttListener {

    @Override
    public void consume(String topic, MqttMessage mqttMessage) {

    }
}
