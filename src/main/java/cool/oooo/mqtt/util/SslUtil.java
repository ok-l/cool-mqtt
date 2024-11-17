package cool.oooo.mqtt.util;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;

/**
 * SSL工具
 */
@Slf4j
public class SslUtil {

    public static SSLSocketFactory getSocketFactory(
            String caCrtFile,
            String crtFile,
            String keyFile,
            final String password
    ) {

        try {
            // 注册BouncyCastle 该第三方库提供大量哈希算法和加密算法
            Security.addProvider(new BouncyCastleProvider());
            // 加载CA证书
            PEMReader reader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(caCrtFile)))));
            // 生成X.509证书
            X509Certificate caCert = (X509Certificate) reader.readObject();
            reader.close();
            // 加载客户端证书
            reader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(crtFile)))));
            // 生成X.509证书
            X509Certificate cert = (X509Certificate) reader.readObject();
            reader.close();
            // 加载客户端秘钥
            reader = new PEMReader(
                    new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(keyFile)))),
                    password::toCharArray
            );
            // 从秘钥中生成KeyPair
            KeyPair key = (KeyPair) reader.readObject();
            reader.close();

            // CA 认证
            KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
            caKs.load(null, null);
            caKs.setCertificateEntry("ca-certificate", caCert);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(caKs);

            // 客户端证书与秘钥发送到服务器进行认证
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry("certificate", cert);
            ks.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(), new java.security.cert.Certificate[]{cert});
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, password.toCharArray());

            // 创建SSL上下文 采用TLSv1.2协议
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return context.getSocketFactory();
        } catch (Exception e) {
            log.error("生成SSLSocketFactory出错", e);
        }
        return null;
    }
}

