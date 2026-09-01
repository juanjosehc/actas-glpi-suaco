package com.empresa.actas.mail.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
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
     * Envia el correo de solicitud de firma (compat: sin OTP).
     */
    public boolean enviarCorreoFirma(String destinatario,
                                     String nombreUsuario,
                                     String tipoActa,
                                     String serialEquipo,
                                     String urlFirma) {
        return enviarCorreoFirma(destinatario, nombreUsuario, tipoActa, serialEquipo, urlFirma, null, 0);
    }

    /**
     * Envia el correo de solicitud de firma. Correo unico: si llega
     * {@code codigoOtp} se incluye el bloque "Codigo OTP" con su vigencia.
     *
     * @param destinatario  correo del usuario firmante
     * @param nombreUsuario nombre del usuario firmante
     * @param tipoActa      tipo de acta (ENTREGA/DEVOLUCION)
     * @param serialEquipo  serial del equipo (puede ser null/vacio)
     * @param urlFirma      enlace publico para firmar el acta
     * @param codigoOtp     OTP de 6 digitos (null/vacio = sin bloque OTP)
     * @param expiraMinutos vigencia del OTP en minutos (para el texto del correo)
     * @return {@code true} si el correo se envio correctamente
     */
    public boolean enviarCorreoFirma(String destinatario,
                                     String nombreUsuario,
                                     String tipoActa,
                                     String serialEquipo,
                                     String urlFirma,
                                     String codigoOtp,
                                     int expiraMinutos) {
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("Correo de firma omitido: destinatario vacio");
            return false;
        }

        if (!smtpConfigurado()) {
            log.warn("SMTP no configurado (mail.host/mail.from). Envio a '{}' omitido.", destinatario);
            return false;
        }

        String cuerpoHtml = construirPlantilla(nombreUsuario, tipoActa, serialEquipo, urlFirma, codigoOtp, expiraMinutos);

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(destinatario.trim());
            helper.setSubject(ASUNTO);
            helper.setText(cuerpoHtml, true);
            // Logo corporativo embebido (CID). El nombre DEBE coincidir con
            // el archivo real en recursos (entradas de JAR son case-sensitive:
            // email/logo.png != email/LOGOAZUL.png y exists() daria false,
            // dejando el cid:imageCorreo huerfano y la imagen rota).
            // El contentType explicito es obligatorio: sin el,
            // SpringResourceDataSource no detecta image/png y el cliente de
            // correo no pinta el part como imagen (icono roto).
            ClassPathResource logo = new ClassPathResource("email/LOGOAZUL.png");
            if (logo.exists()) {
                helper.addInline("logoCorreo", logo, "image/png");
            }
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
     * Si {@code codigoOtp} no es null/vacio se agrega el bloque OTP destacado.
     */
    private String construirPlantilla(String nombreUsuario,
                                      String tipoActa,
                                      String serialEquipo,
                                      String urlFirma,
                                      String codigoOtp,
                                      int expiraMinutos) {
        String nombre = nombreUsuario != null && !nombreUsuario.isBlank()
                ? nombreUsuario.trim() : "usuario";
        String tipo = tipoActa != null ? tipoActa : "-";
        String serial = serialEquipo != null && !serialEquipo.isBlank()
                ? serialEquipo : "-";

        String bloqueOtp = "";
        if (codigoOtp != null && !codigoOtp.isBlank()) {
            bloqueOtp = """
                <tr>
                  <td style="padding:0 28px 8px;">
                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#EFF6FF;border:2px solid #2563EB;border-radius:10px;">
                      <tr>
                        <td style="padding:18px 20px;">
                          <div style="color:#1E3A8A;font-size:12px;font-weight:bold;letter-spacing:1px;text-transform:uppercase;">Codigo de verificacion · OTP</div>
                          <div style="color:#0F172A;font-size:34px;font-weight:bold;letter-spacing:8px;margin:8px 0 6px;line-height:1.2;">@@OTP@@</div>
                          <div style="color:#475569;font-size:12px;line-height:1.5;">Ingreselo al abrir el enlace de firma. Vigencia de <strong>@@MINUTOS@@ minutos</strong>; es de uso unico e intransferible.</div>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
                """;
        }

        String plantilla = """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0;padding:0;background-color:#F1F5F9;font-family:Arial,Helvetica,sans-serif;">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#F1F5F9;padding:24px 12px;">
                  <tr>
                    <td align="center">
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;background-color:#FFFFFF;border-radius:10px;border:1px solid #E2E8F0;box-shadow:0 2px 8px rgba(15,23,42,0.06);">

                        <!-- Cabecera corporativa -->
                        <tr>
                          <td style="background-color:#FFFFFF;">
                            <div style="background-color:#1E3A8A;height:6px;font-size:0;line-height:0;">&nbsp;</div>
                            <div style="padding:18px 28px 10px;text-align:center;">
                              <img src="cid:logoCorreo" width="340" alt="SAUCO" style="max-width:100%;height:auto;border:0;outline:none;">
                              <div style="color:#0F172A;font-size:18px;font-weight:bold;margin-top:8px;letter-spacing:1px;">SAUCO</div>
                              <div style="color:#64748B;font-size:12px;margin-top:2px;letter-spacing:0.3px;">Sistema de Actas y Control Operativo</div>
                            </div>
                          </td>
                        </tr>

                        <!-- Cuerpo -->
                        <tr>
                          <td style="padding:24px 28px;">
                            <p style="margin:0 0 14px;color:#0F172A;font-size:15px;line-height:1.5;">Cordial saludo, <strong>@@NOMBRE@@</strong>:</p>
                            <p style="margin:0 0 20px;color:#0F172A;font-size:15px;line-height:1.5;">Se le ha generado un acta que requiere su firma digital. A continuacion encontrara el detalle y el enlace para completar el tramite:</p>

                            <!-- Datos del acta -->
                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border:1px solid #E2E8F0;border-radius:8px;margin:0 0 24px;">
                              <tr>
                                <td style="padding:10px 14px;background-color:#F8FAFC;border-bottom:1px solid #E2E8F0;color:#64748B;font-size:12px;">Tipo de acta</td>
                                <td style="padding:10px 14px;border-bottom:1px solid #E2E8F0;color:#0F172A;font-size:13px;font-weight:bold;">@@TIPO@@</td>
                              </tr>
                              <tr>
                                <td style="padding:10px 14px;background-color:#F8FAFC;border-bottom:1px solid #E2E8F0;color:#64748B;font-size:12px;">Serial del equipo</td>
                                <td style="padding:10px 14px;border-bottom:1px solid #E2E8F0;color:#0F172A;font-size:13px;font-weight:bold;">@@SERIAL@@</td>
                              </tr>
                              <tr>
                                <td style="padding:10px 14px;background-color:#F8FAFC;color:#64748B;font-size:12px;">Firmante</td>
                                <td style="padding:10px 14px;color:#0F172A;font-size:13px;font-weight:bold;">@@NOMBRE@@</td>
                              </tr>
                            </table>
                            """;

        plantilla += """
                            <!-- Solicitud de firma -->
                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin:0 0 16px;">
                              <tr>
                                <td align="center" style="border-radius:8px;background-color:#2563EB;">
                                  <a href="@@URL@@"
                                     style="display:inline-block;width:100%;padding:14px 0;color:#FFFFFF;text-decoration:none;font-size:14px;font-weight:bold;letter-spacing:0.3px;text-align:center;">
                                    Firmar el Acta
                                  </a>
                                </td>
                              </tr>
                            </table>
                            <p style="margin:0 0 8px;color:#334155;font-size:13px;line-height:1.5;">Si el boton no funciona, copie y pegue este enlace en su navegador:</p>
                            <p style="margin:0 0 24px;padding:10px 12px;background-color:#F8FAFC;border:1px solid #E2E8F0;border-radius:6px;color:#1D4ED8;font-size:12px;word-break:break-all;">@@URL@@</p>
                          </td>
                        </tr>

                        @@BLOQUE_OTP@@

                        <!-- Advertencia de seguridad -->
                        <tr>
                          <td style="padding:0 28px 0;">
                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#FFF7ED;border:1px solid #FDBA74;border-radius:8px;">
                              <tr>
                                <td style="padding:12px 16px;">
                                  <div style="color:#B45309;font-size:12px;font-weight:bold;letter-spacing:0.5px;">IMPORTANTE — SEGURIDAD</div>
                                  <div style="color:#78350F;font-size:12px;line-height:1.5;margin-top:4px;">No comparta el enlace de firma ni el codigo de verificacion con terceros. Si no solicito este tramite, ignore este mensaje y reporte el hecho al area de TI.</div>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>

                        <!-- Pie corporativo -->
                        <tr>
                          <td style="background-color:#F8FAFC;padding:16px 28px;border-top:1px solid #E2E8F0;">
                            <div style="color:#0F172A;font-size:11px;font-weight:bold;letter-spacing:0.5px;">SAUCO · Sistema de Actas y Control Operativo</div>
                            <div style="color:#64748B;font-size:11px;line-height:1.5;margin-top:2px;">Correo generado automaticamente, no responda a este mensaje.</div>
                            <div style="color:#94A3B8;font-size:10px;margin-top:2px;">© 2026 Coltefinanciera · Uso interno</div>
                          </td>
                        </tr>

                      </table>
                    </td>
                  </tr>
                </table>
                </body>
                </html>
                """;

        return plantilla
                // Insertar el bloque OTP PRIMERO: sus @@OTP@@ y @@MINUTOS@@
                // deben quedar dentro del texto global para que los replaces
                // posteriores los alcancen (antes quedaban literales).
                .replace("@@BLOQUE_OTP@@", bloqueOtp)
                .replace("@@OTP@@", escapeHtml(codigoOtp != null ? codigoOtp : ""))
                .replace("@@MINUTOS@@", String.valueOf(expiraMinutos))
                .replace("@@NOMBRE@@", escapeHtml(nombre))
                .replace("@@TIPO@@", escapeHtml(tipo))
                .replace("@@SERIAL@@", escapeHtml(serial))
                .replace("@@URL@@", escapeHtml(urlFirma));
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
