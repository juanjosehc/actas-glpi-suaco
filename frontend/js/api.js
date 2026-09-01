/* ========================================================
   API_BASE — URL única del backend (QA-29)
   --------------------------------------------------------
   Estrategia centralizada: todas las páginas consumen esta
   variable global; ninguna URL del backend queda hardcodeada
   en los JS individuales.

   Override opcional por entorno: definir en el HTML ANTES de
   cargar este script, ej:
     <script>window.SAUCO_API = "https://actas.coltefinanciera.com";</script>
   Si no se define, usa el default de desarrollo (localhost:8001).
   El backend debe correr en el puerto 8001.
   ======================================================== */
var API_BASE = window.SAUCO_API || "http://localhost:8001";