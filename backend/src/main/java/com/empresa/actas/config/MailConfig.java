package com.empresa.actas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuracion del remitente de correo (SMTP).
 *
 * Los valores provienen de {@code application.yml} (claves {@code mail.*})
 * y pueden sobrescribirse con variables de entorno. Si el host no esta
 * definido, se crea un {@link JavaMailSender} vacio para que la aplicacion
 * arranque normalmente; {@link com.empresa.actas.mail.service.MailService}
 * se encarga de omitir el envio en ese caso.
 */
@Configuration
public class MailConfig {

    @Value("${mail.host:}")
    private String host;

    @Value("${mail.port:587}")
    private int port;

    @Value("${mail.username:}")
    private String username;

    @Value("${mail.password:}")
    private String password;

    @Value("${mail.from:}")
    private String from;

    @Value("${mail.properties.mail.smtp.auth:true}")
    private boolean smtpAuth;

    @Value("${mail.properties.mail.smtp.starttls.enable:true}")
    private boolean smtpStarttls;

    @Value("${mail.properties.mail.smtp.connectiontimeout:10000}")
    private int connectionTimeout;

    @Value("${mail.properties.mail.smtp.timeout:15000}")
    private int timeout;

    @Value("${mail.properties.mail.smtp.writetimeout:15000}")
    private int writeTimeout;

    /**
     * Construye el {@link JavaMailSender}. Si {@code mail.host} esta vacio
     * retorna un bean funcional pero sin servidor configurado.
     *
     * @return bean JavaMailSender
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setDefaultEncoding("UTF-8");

        if (host != null && !host.isBlank()) {
            sender.setHost(host);
            sender.setPort(port);
            if (username != null && !username.isBlank()) {
                sender.setUsername(username);
                sender.setPassword(password);
            }

            Properties props = sender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", smtpAuth);
            props.put("mail.smtp.starttls.enable", smtpStarttls);
            props.put("mail.smtp.connectiontimeout", connectionTimeout);
            props.put("mail.smtp.timeout", timeout);
            props.put("mail.smtp.writetimeout", writeTimeout);
        }

        return sender;
    }

    /**
     * Expone la direccion remitente configurada ({@code mail.from}).
     *
     * @return correo remitente o cadena vacia si no esta definido
     */
    @Bean
    public String mailFrom() {
        return from != null ? from.trim() : "";
    }
}
