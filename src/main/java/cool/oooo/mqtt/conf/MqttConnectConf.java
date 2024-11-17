package cool.oooo.mqtt.conf;

import cool.oooo.mqtt.mqtt.*;
import cool.oooo.mqtt.util.SslUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Configuration
@Slf4j
public class MqttConnectConf {

    @Resource
    private MqttConnectConfPros mqttConnectConfPros;
    @Resource
    private MqttCallback mqttCallback;
    @Resource
    private Map<String, MqttListener> consumers;
    @Resource
    private Map<String, MqttBatchListener> batchConsumers;
    @Resource
    private Map<String, RegularMqttListener> regularConsumers;

    /**
     * 初始化mqtt链接
     *
     * @return mqttClient
     */
    @Bean("mqttClient")
    public MqttClient connect() {

        try {
            MqttClient mqttClient = new MqttClient(
                    mqttConnectConfPros.getServerUri(),
                    mqttConnectConfPros.getClintId(),
                    new MemoryPersistence()
            );
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(mqttConnectConfPros.getUsername());
            options.setPassword(mqttConnectConfPros.getPassword().toCharArray());
            options.setCleanSession(true);
            options.setKeepAliveInterval(mqttConnectConfPros.getKeepAliveInterval());
            options.setConnectionTimeout(mqttConnectConfPros.getConnectionTimeout());
            options.setWill(mqttConnectConfPros.getWillTopic(), (mqttConnectConfPros.getClintId() + "与服务器断开连接").getBytes(), 0, false);

            // 配置 ssl
            setSsl(options);
            mqttClient.setCallback(mqttCallback);
            mqttClient.connect(options);
            String[] topics = getTopics();
            mqttClient.subscribe(topics);
            return mqttClient;
        } catch (Exception e) {
            log.error("Mqtt connect error", e);
            throw new RuntimeException("Connect Mqtt Failed");
        }
    }

    /**
     * 设置证书配置
     * @param options 链接配置项
     */
    private void setSsl(MqttConnectOptions options) {

        if(mqttConnectConfPros.getSslEnable() == null || !mqttConnectConfPros.getSslEnable()){
            return;
        }
        options.setSocketFactory(SslUtil.getSocketFactory(
                mqttConnectConfPros.getCaFile(),
                mqttConnectConfPros.getCertificateFile(),
                mqttConnectConfPros.getKeyFile(),
                mqttConnectConfPros.getPassword()
        ));
    }

    /**
     * 获取topics topics 来自Consumer的注解
     *
     * @return topic 数组
     */
    private String[] getTopics() {

        List<String> topicList = new ArrayList<>();
        consumers.values().forEach(consumer -> {
            MqttSubscription mqttSubscription = consumer.getClass().getAnnotation(MqttSubscription.class);
            if (mqttSubscription != null) {
                topicList.add(mqttSubscription.topic());
            }
        });
        regularConsumers.values().forEach(consumer -> {
            RegularMqttSubscription regularMqttSubscription = consumer.getClass().getAnnotation(RegularMqttSubscription.class);
            if (regularMqttSubscription != null) {
                topicList.add(regularMqttSubscription.topic());
            }
        });
        batchConsumers.values().forEach(consumer -> {
            Method[] methods = consumer.getClass().getMethods();
            for (Method method : methods) {
                MqttSubscriptionOnMethod mqttSubscriptionOnMethod = method.getAnnotation(MqttSubscriptionOnMethod.class);
                if (mqttSubscriptionOnMethod != null) {
                    topicList.add(mqttSubscriptionOnMethod.topic());
                }
                RegularMqttSubscriptionOnMethod regularMqttSubscriptionOnMethod = method.getAnnotation(RegularMqttSubscriptionOnMethod.class);
                if (regularMqttSubscriptionOnMethod != null) {
                    topicList.add(regularMqttSubscriptionOnMethod.topic());
                }
            }
        });
        return topicList.toArray(new String[]{});
    }
}
