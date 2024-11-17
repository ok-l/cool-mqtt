package cool.oooo.mqtt.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@Data
@ConfigurationProperties(prefix = "mqtt")
public class MqttConnectConfPros {

    private String serverUri;
    private String username;
    private String password;
    private String clintId;
    private Integer keepAliveInterval;
    private Integer connectionTimeout;
    private String willTopic;

    private Boolean sslEnable;
    private String caFile;
    private String certificateFile;
    private String keyFile;
}
