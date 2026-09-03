-- RESPLADO contenido_html (7 registros) previo a DROP de la columna --
-- Fecha: 2026-09-03. Recrear tabla si hace falta: --
CREATE TABLE IF NOT EXISTS public.respaldo_contenido_html (id_acta integer PRIMARY KEY, contenido_html text);

TRUNCATE public.respaldo_contenido_html;

INSERT INTO public.respaldo_contenido_html (id_acta, contenido_html) VALUES (1, '<h1>Acta de Entrega</h1>');
INSERT INTO public.respaldo_contenido_html (id_acta, contenido_html) VALUES (4, '<div class="acta-document">
    <div class="acta-header">
        <div class="acta-header-left">
            <img class="acta-logo" src="img/LogoC.png" alt="Coltefinanciera">
            <div class="acta-company">COMPANIA DE FINANCIAMIENTO COMERCIAL COLTEFINANCIERA S.A.</div>
            <div class="acta-note">NIT 890.000.000-0</div>
        </div>
        <div class="acta-header-right">
            <div class="acta-code">ACTA N° <strong>Pendiente</strong></div>
            <div class="acta-date">Fecha: <strong>2026-07-29</strong></div>
        </div>
    </div>

    <div class="acta-title-section">
        <h1 class="acta-title">MEMORANDO DE ENTREGA DE DISPOSITIVOS</h1>
        <p class="acta-subtitle">Por medio del presente documento se deja constancia de la entrega de equipos de computo y/o dispositivos electronicos al colaborador que se menciona a continuacion:</p>
    </div>

    <div class="acta-body">
        <table class="acta-table">
            <tr>
                <td class="acta-label">FECHA DE ENTREGA:</td>
                <td class="acta-value">2026-07-29</td>
            </tr>
            <tr>
                <td class="acta-label">FUNCIONARIO QUE RECIBE:</td>
                <td class="acta-value">aa</td>
            </tr>
            <tr>
                <td class="acta-label">NUMERO DE CEDULA:</td>
                <td class="acta-value">________________</td>
            </tr>
            <tr>
                <td class="acta-label">CORREO CORPORATIVO:</td>
                <td class="acta-value">________________</td>
            </tr>
            <tr>
                <td class="acta-label">CARGO:</td>
                <td class="acta-value">aa</td>
            </tr>
            <tr>
                <td class="acta-label">DEPARTAMENTO / SEDE:</td>
                <td class="acta-value">________________</td>
            </tr>
            <tr>
                <td class="acta-label">EMPRESA:</td>
                <td class="acta-value">________________</td>
            </tr>
            <tr>
                <td class="acta-label">TICKET GLPI:</td>
                <td class="acta-value">12313</td>
            </tr>
        </table>

        <h2 class="acta-subsection">DATOS DEL EQUIPO ENTREGADO</h2>

        <table class="acta-table">
            <tr>
                <td class="acta-label">TIPO DE EQUIPO:</td>
                <td class="acta-value">HP HP Laptop 14-fq1xxx Ryzen 5</td>
            </tr>
            <tr>
                <td class="acta-label">MARCA / MODELO:</td>
                <td class="acta-value">HP HP Laptop 14-fq1xxx Ryzen 5</td>
            </tr>
            <tr>
                <td class="acta-label">NUMERO DE SERIE:</td>
                <td class="acta-value">5CD2256W6H</td>
            </tr>
            <tr>
                <td class="acta-label">PLACA INTERNA:</td>
                <td class="acta-value">12313</td>
            </tr>
        </table>

        <h2 class="acta-subsection">ESPECIFICACIONES TECNICAS</h2>

        <table class="acta-table">
            <tr>
                <td class="acta-label">PROCESADOR:</td>
                <td class="acta-value">________________</td>
            </tr>
            <tr>
                <td class="acta-label">MEMORIA RAM:</td>
                <td class="acta-value">________________</td>
            </tr>
            <tr>
                <td class="acta-label">DISCO DURO:</td>
                <td class="acta-value">________________</td>
            </tr>
            <tr>
                <td class="acta-label">SISTEMA OPERATIVO:</td>
                <td class="acta-value">Mac OS</td>
            </tr>
            <tr>
                <td class="acta-label">MONITOR:</td>
                <td class="acta-value">________________</td>
            </tr>
            <tr>
                <td class="acta-label">ACCESORIOS:</td>
                <td class="acta-value">________________</td>
            </tr>
        </table>

        <h2 class="acta-subsection">OBSERVACIONES</h2>
        <div class="acta-observations">aaaa</div>

        <div class="acta-clause">
            <p><strong>CLAUSULA DE RESPONSABILIDAD:</strong> El colaborador declara haber recibido los equipos y dispositivos descritos en el presente documento, en buen estado y funcionamiento, y se compromete a hacer uso adecuado de los mismos, respondiendo por cualquier dano, perdida o deterioro causado por mal uso, negligencia o incumplimiento de las politicas de seguridad informatica establecidas por la compania.</p>
        </div>
    </div>

    <div class="acta-footer">
        <div class="acta-signature-section">
            <div class="acta-signature-box">
                <div class="acta-signature-label">ENTREGADO POR:</div>
                <div class="acta-signature-line"></div>
                <div class="acta-signature-name">aa</div>
                <div class="acta-signature-role">Tecnico de Soporte</div>
            </div>
            <div class="acta-signature-box">
                <div class="acta-signature-label">RECIBIDO POR:</div>
                <div class="acta-signature-line"></div>
                <div class="acta-signature-name">aa</div>
                <div class="acta-signature-role">Colaborador</div>
            </div>
        </div>
    </div>

    <div class="acta-watermark">DOCUMENTO DE USO INTERNO - COLTEFINANCIERA</div>
</div>
');
INSERT INTO public.respaldo_contenido_html (id_acta, contenido_html) VALUES (5, '<div class="acta-document" style="font-family:Calibri,sans-serif;font-size:11pt;line-height:1.4;padding:20px 30px;">
<table style="width:100%;border-collapse:collapse;margin:8px 0;">
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">MEMORANDO DE ENTREGA DE DISPOSITIVOS</td></tr>
</table>
<table style="width:100%;border-collapse:collapse;margin:8px 0;">
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Fecha:</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Día</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Mes</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Año</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">29</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">07</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">2026</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Entregado a:</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">a</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Cargo:</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">a</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Entregado por:</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">aa</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Cargo:</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">a</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Asunto:</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">aa</td></tr>
</table>
<p style="margin:0 0 6px 0;">Cordialmente se relaciona el dispositivo que le fue asignado.</p>
<table style="width:100%;border-collapse:collapse;margin:8px 0;">
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">DESCRIPCION DEL EQUIPO DE COMPUTO</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Marca</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Tipo</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Modelo</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Serial</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Nro. Inventario</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">HP</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Notebook</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">HP Laptop 14-fq1xxx Ryzen 5</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">5CD2256W6H</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">1231</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>
</table>
<p style="margin:0 0 6px 0;">Contenido del Dispositivo</p>
<table style="width:100%;border-collapse:collapse;margin:8px 0;">
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">HARDWARE</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">SOFTWARE</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Tipo de Hardware</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Descripción</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Programa</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">aa</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">aa</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">aa</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>
<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>
</table>
<p style="margin:0 0 6px 0;">Atentamente.</p>
<p style="margin:0 0 6px 0;">                                                                                                  </p>
<p style="margin:0 0 6px 0;">_______________________                  _____________________</p>
<p style="margin:0 0 6px 0;">                                                                                                            Recibido por: {{ entregado_a }} </p>
<p style="margin:0 0 6px 0;">Director de Infraestructura                       Cargo: {{ cargo_recibe }}</p>
</div>');
INSERT INTO public.respaldo_contenido_html (id_acta, contenido_html) VALUES (120, '<p>Entrega de equipo</p>');
INSERT INTO public.respaldo_contenido_html (id_acta, contenido_html) VALUES (121, '<p>Entrega equipo</p>');
INSERT INTO public.respaldo_contenido_html (id_acta, contenido_html) VALUES (122, '<p>Entrega equipo</p>');
INSERT INTO public.respaldo_contenido_html (id_acta, contenido_html) VALUES (123, '<p>Entrega equipo</p>');

-- Verificacion: SELECT count(*) AS respaldados FROM public.respaldo_contenido_html; -- debe dar 7
