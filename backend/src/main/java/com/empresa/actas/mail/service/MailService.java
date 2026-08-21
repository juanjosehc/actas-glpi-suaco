package com.empresa.actas.mail.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Servicio de envio de correos electronicos.
 *
 * Envia la solicitud de firma del acta al usuario receptor. El cuerpo es
 * un correo HTML corporativo con los datos del acta y el enlace de firma.
 *
 * La configuracion SMTP es externa ({@code mail.*}). Si no esta definida
 * o el envio falla, el servicio registra el problema en el log y devuelve
 * {@code false} sin interrumpir el flujo principal del sistema.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private static final String ASUNTO = "Solicitud de Firma de Acta";

    private final JavaMailSender javaMailSender;
    private final String mailFrom;
    private final String mailHost;

    public MailService(JavaMailSender javaMailSender,
                       @Qualifier("mailFrom") String mailFrom,
                       @Value("${mail.host:}") String mailHost) {
        this.javaMailSender = javaMailSender;
        this.mailFrom = mailFrom;
        this.mailHost = mailHost;
    }

    /**
     * Envia el correo de solicitud de firma.
     *
     * @param destinatario correo del usuario firmante
     * @param nombreUsuario nombre del usuario firmante
     * @param tipoActa      tipo de acta (ENTREGA/DEVOLUCION)
     * @param serialEquipo  serial del equipo (puede ser null/vacio)
     * @param urlFirma      enlace publico para firmar el acta
     * @return {@code true} si el correo se envio correctamente
     */
    public boolean enviarCorreoFirma(String destinatario,
                                     String nombreUsuario,
                                     String tipoActa,
                                     String serialEquipo,
                                     String urlFirma) {
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("Correo de firma omitido: destinatario vacio");
            return false;
        }

        if (!smtpConfigurado()) {
            log.warn("SMTP no configurado (mail.host/mail.from). Envio a '{}' omitido.", destinatario);
            return false;
        }

        String cuerpoHtml = construirPlantilla(nombreUsuario, tipoActa, serialEquipo, urlFirma);

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(destinatario.trim());
            helper.setSubject(ASUNTO);
            helper.setText(cuerpoHtml, true);
            javaMailSender.send(message);
            log.info("Correo de firma enviado a '{}' con enlace '{}'", destinatario, urlFirma);
            return true;
        } catch (MailException | jakarta.mail.MessagingException e) {
            log.error("Error al enviar correo de firma a '{}': {}", destinatario, e.getMessage(), e);
            return false;
        }
    }

    private boolean smtpConfigurado() {
        boolean hostOk = mailHost != null && !mailHost.isBlank();
        boolean fromOk = mailFrom != null && !mailFrom.isBlank();
        return hostOk && fromOk;
    }

    /**
     * Construye la plantilla HTML corporativa de la solicitud de firma.
     */
    private String construirPlantilla(String nombreUsuario,
                                      String tipoActa,
                                      String serialEquipo,
                                      String urlFirma) {
        String nombre = nombreUsuario != null && !nombreUsuario.isBlank()
                ? nombreUsuario.trim() : "usuario";
        String tipo = tipoActa != null ? tipoActa : "-";
        String serial = serialEquipo != null && !serialEquipo.isBlank()
                ? serialEquipo : "-";

        return "<!DOCTYPE html>"
                + "<html lang=\"es\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "</head><body style=\"margin:0;padding:0;background-color:#F1F5F9;font-family:Arial,Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#F1F5F9;padding:24px 12px;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" style=\"max-width:600px;background-color:#FFFFFF;border-radius:8px;overflow:hidden;border:1px solid #E2E8F0;\">"
                + "<tr><td style=\"background-color:#1E3A8A;padding:20px 28px;\">"
                + "<div style=\"color:#FFFFFF;font-size:20px;font-weight:bold;\">Solicitud de Firma de Acta</div>"
                + "<div style=\"color:#BFDBFE;font-size:12px;margin-top:2px;\">Sistema de Actas</div>"
                + "</td></tr>"
                + "<tr><td style=\"padding:28px;\">"
                + "<p style=\"margin:0 0 16px;color:#0F172A;font-size:15px;line-height:1.5;\">"
                + "Cordial saludo, <strong>" + escapeHtml(nombre) + "</strong>:</p>"
                + "<p style=\"margin:0 0 16px;color:#0F172A;font-size:15px;line-height:1.5;\">"
                + "Se le ha generado un <strong>Acta de " + escapeHtml(tipo) + "</strong> "
                + "relacionada con el equipo de serial <strong>" + escapeHtml(serial) + "</strong>. "
                + "Para revisar el documento, firmarlo o rechazarlo, haga clic en el siguiente boton:</p>"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:24px 0;\">"
                + "<tr><td style=\"border-radius:6px;background-color:#2563EB;\">"
                + "<a href=\"" + escapeHtml(urlFirma) + "\" style=\"display:inline-block;padding:12px 28px;color:#FFFFFF;text-decoration:none;font-size:14px;font-weight:bold;\">"
                + "Ir a Firmar el Acta</a>"
                + "</td></tr></table>"
                + "<p style=\"margin:0 0 8px;color:#334155;font-size:13px;line-height:1.5;\">"
                + "Si el boton no funciona, copie y pegue este enlace en su navegador:</p>"
                + "<p style=\"margin:0;padding:10px 12px;background-color:#F8FAFC;border:1px solid #E2E8F0;border-radius:6px;color:#1D4ED8;font-size:12px;word-break:break-all;\">"
                + escapeHtml(urlFirma) + "</p>"
                + "</td></tr>"
                + "<tr><td style=\"background-color:#F8FAFC;padding:14px 28px;border-top:1px solid #E2E8F0;\">"
                + "<div style=\"color:#64748B;font-size:11px;\">Este es un correo generado automaticamente. Por favor no responda a este mensaje.</div>"
                + "</td></tr>"
                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    private String escapeHtml(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
