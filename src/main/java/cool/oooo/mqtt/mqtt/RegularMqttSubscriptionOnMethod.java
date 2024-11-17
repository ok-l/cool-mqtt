package cool.oooo.mqtt.mqtt;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RegularMqttSubscriptionOnMethod {

    String topic();

    String expression() default "";
}
