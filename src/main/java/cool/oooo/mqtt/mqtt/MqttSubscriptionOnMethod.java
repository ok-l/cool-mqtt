package cool.oooo.mqtt.mqtt;

import java.lang.annotation.*;


@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MqttSubscriptionOnMethod {

    String topic();

    String expression() default "";
}
