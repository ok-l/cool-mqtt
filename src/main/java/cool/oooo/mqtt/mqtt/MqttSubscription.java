package cool.oooo.mqtt.mqtt;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;


@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Component
public @interface MqttSubscription {

    String topic();

    String expression() default "";
}
