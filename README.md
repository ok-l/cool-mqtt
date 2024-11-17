# cool-mqtt

#### 介绍
提供一种通过注解的方式来订阅mqtt消息

#### 1. 快速接入
通过java代码来订阅Mqtt消息的时候，一般涉及两个步骤，首先在初始化Mqtt链接的时候，订阅指定的topisc：
```
client = new MqttClient(serverUri,clientId,new MemoryPersistence());
MqttConnectOptions options = new MqttConnectOptions();
options.setCleanSession(true);
options.setUserName(username);
options.setPassword(password.toCharArray());
options.setConnectionTimeout(10);
options.setKeepAliveInterval(60);
options.setWill("testtopic",(clientId + "与服务器断开连接").getBytes(),1,false);
client.setCallback(new MqttCallBackImpl());
client.connect(options);
int[] qos = {1,1};
String[] topics = {"topicA","topicB"};
//这里订阅指定topic数组
client.subscribe(topics,qos);
```
然后，通过实现MqttCallback来消费消息：
```
public class MqttCallBackImpl implements MqttCallback{

    @Override
    public void connectionLost(Throwable throwable) {
       
    }

    /**
     * 这里消费消息
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if(topic.equals("topic1")){
            //处理逻辑
            
        }
        if(topic.equals("topic2")){
            //处理逻辑
        }
    }

 
    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }
}
```
这种方式比较流行，但是不够优雅，每次新增topic订阅，都需要去动上面两块代码，尤其是MqttCallback实现类，可能会有很多开发者共同使用，会导致该类向滚雪球一样越滚越大，越来越乱。

下面介绍一种通过java注解的方式来优雅订阅mqtt消息(下面讲的案例是在spring-boot工程下进行的)。
首先引入依赖(jar包依赖见后面章节)：
```
<dependency>
  <groupId>com.leador.scd</groupId>
  <artifactId>mqtt-listener</artifactId>
  <version>1.0.0</version>
</dependency>
```
接下来在application配置mqtt链接相关信息：
```
mqtt:
  server-uri: tcp://127.0.0.1:11883
  username: admin
  password: 123456
  clintId: test-client
  keep-alive-interval: 10
  connection-timeout: 60
  will-topic: testtopic
```
最后，通过注解的方式，订阅mqtt消息：
##### 单个topic订阅
订阅topic1
```
@MqttSubscription(topic = "topic1")
public class Consumer implements MqttListener {

    @Override
    public void consume(MqttMessage mqttMessage) {
        System.out.println("consume topic1");
        System.out.println(new String(mqttMessage.getPayload()));
    }
}
```
订阅topic2
```
@MqttSubscription(topic = "topic2")
public class Consumer2 implements MqttListener {

    @Override
    public void consume(MqttMessage mqttMessage) {
        System.out.println("consume topic2");
        System.out.println(new String(mqttMessage.getPayload()));
    }
}
```
经过简单的3步就完成了，对mqtt消息的监听，不存在对已有代码的修改，也不存在代码入侵，简单高效。

##### 一个类多个topic监听
如果想在一个类里面完成多个topic的监听，可以通过在方法上用@MqttSubscriptionOnMethod注解实现
```
@Component
public class BatchConsumer implements MqttBatchListener {

    @MqttSubscriptionOnMethod(topic = "name")
    public void getName(MqttMessage mqttMessage){
        System.out.println("consume name");
        System.out.println(new String(mqttMessage.getPayload()));
    }

    @MqttSubscriptionOnMethod(topic = "age")
    public void getAge(MqttMessage mqttMessage){
        System.out.println("consume age");
        System.out.println(new String(mqttMessage.getPayload()));
    }

    @MqttSubscriptionOnMethod(topic = "class")
    public void getClass(MqttMessage mqttMessage){
        System.out.println("consume class");
        System.out.println(new String(mqttMessage.getPayload()));
    }
}
```

#### 2. 原理简介
该方式原理很简单，首先，在初始化链接的时候，读取类或者方法注解中的topic，进行订阅：
```
@Configuration
@Slf4j
public class MqttConnectConf {

    @Resource
    private MqttConnectConfPros mqttConnectConfPros;
    /**
     * 这是mqtt回调实现类
     */
    @Resource
    private MqttCallback mqttCallback;
    /**
     * 这里注入Spring上下文中的单topic订阅者 用户代码中通过类注解实现的订阅 即实现了MqttListener接口的对象
     */
    @Resource
    private Map<String, MqttListener> consumers;
    /**
     * 这里注入Spring上下文中的批量订阅者 用户代码中通过方法注解实现的订阅 即实现了MqttBatchListener接口的对象
     */
    @Resource
    private Map<String, MqttBatchListener> batchConsumers;

    /**
     * 初始化mqtt链接
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

            mqttClient.setCallback(mqttCallback);
            mqttClient.connect(options);
            //从Spring上下文中的对象注解中读取topic
            String[] topics = getTopics();
            //订阅Spring上下文中的对象注解中读取的topic
            mqttClient.subscribe(topics);
            return mqttClient;
        } catch (Exception e) {
            log.error("Mqtt connect error", e);
            throw new RuntimeException("Connect Mqtt Failed");
        }
    }

    /**
     * 获取topics topics 来自Consumer的注解 从spring上下文中获取consumers
     * 然后 从consumers的MqttSubscription注解以及MqttSubscriptionOnMethod中读取topic
     *
     * @return topic 数组
     */
    private String[] getTopics() {
		if(CollectionUtils.isEmpty(consumers)){
            consumers = new HashMap<>(0);
        }
        if(CollectionUtils.isEmpty(batchConsumers)){
            batchConsumers = new HashMap<>(0);
        }
        List<String> topicList = new ArrayList<>();
        //单topic订阅 类注解MqttSubscription实现的订阅
        consumers.values().forEach(consumer -> {
            MqttSubscription mqttSubscription = consumer.getClass().getAnnotation(MqttSubscription.class);
            if (mqttSubscription != null) {
                topicList.add(mqttSubscription.topic());
            }
        });
        //批量topic订阅 方法注解MqttSubscriptionOnMethod实现的订阅
        batchConsumers.values().forEach(consumer -> {
            Method[] methods = consumer.getClass().getMethods();
            for (Method method : methods) {
                MqttSubscriptionOnMethod mqttSubscriptionOnMethod = method.getAnnotation(MqttSubscriptionOnMethod.class);
                if (mqttSubscriptionOnMethod != null) {
                    topicList.add(mqttSubscriptionOnMethod.topic());
                }
            }
        });
        return topicList.toArray(new String[]{});
    }
}
```

mqtt回调实现类中，调用用户实现的consum方法或者自定义方法来处理订阅消息：
```
@Configuration
@Slf4j
public class MqttCallBackImpl implements MqttCallback {

    /**
     * spring上下文中的订阅对象 即实现了MqttListener接口的对象
     */
    @Resource
    private Map<String, MqttListener> consumers;
    /**
     * spring上下文中的批量订阅对象 即实现了MqttBatchListener接口的对象
     */
    @Resource
    private Map<String, MqttBatchListener> batchConsumers;

    @Override
    public void connectionLost(Throwable throwable) {
        log.error("Mqtt 连接断开", throwable);
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {
		
        if(CollectionUtils.isEmpty(consumers)){
            consumers = new HashMap<>(0);
        }
        if(CollectionUtils.isEmpty(batchConsumers)){
            batchConsumers = new HashMap<>(0);
        }
        //单topic 订阅
        consumers.values().forEach(consumer -> {
            MqttSubscription mqttSubscription = consumer.getClass().getAnnotation(MqttSubscription.class);
            if (mqttSubscription == null) {
                return;
            }
            if (Objects.equals(topic, mqttSubscription.topic())) {
                //这里根据到达的topic消息 来选择对应的consumer对象来处理消息
                consumer.consume(mqttMessage);
            }
        });
        //批量topic订阅
        batchConsumers.values().forEach(consumer -> {
            Method[] methods = consumer.getClass().getMethods();
            for (Method method : methods) {
                MqttSubscriptionOnMethod mqttSubscriptionOnMethod = method.getAnnotation(MqttSubscriptionOnMethod.class);
                if (mqttSubscriptionOnMethod != null && Objects.equals(topic, mqttSubscriptionOnMethod.topic())) {
                    //这里根据到达的topic消息 来选择对应的consumer对象的具体方法来处理消息
                    consumeMsg(mqttMessage, consumer, method);
                }
            }
        });
    }

    /**
     * 处理消息
     *
     * @param mqttMessage 消息
     * @param consumer    消费者
     * @param method      方法
     */
    private void consumeMsg(MqttMessage mqttMessage, MqttBatchListener consumer, Method method) {
        try {
            method.invoke(consumer, mqttMessage);
        } catch (Exception e) {
            log.error("MqttCallBackImpl consumeMsg error methodName={}", method.getName(), e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        String[] topics = iMqttDeliveryToken.getTopics();
        for (String topic : topics) {
            log.info("topics: " + topic + " clientId: " + iMqttDeliveryToken.getClient().getClientId() + " " + "消息发布成功");
        }
    }
}
```

MqttListener是个函数式接口，只有一个方法：
```
public interface MqttListener {

    /**
     * consume mqtt msg
     *
     * @param mqttMessage msg
     */
    void consume(MqttMessage mqttMessage);
}
```
MqttBatchListener是一个没有任何方法的接口：
```
public interface MqttBatchListener {
}
```

MqttSubscription注解如下：
```
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Component
public @interface MqttSubscription {

        String topic();
}
```

MqttSubscriptionOnMethod注解如下：
```
Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MqttSubscriptionOnMethod {

    String topic();
}
```


