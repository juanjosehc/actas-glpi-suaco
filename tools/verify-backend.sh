#!/usr/bin/env bash
# =============================================================
# Verificacion rapida del backend (compila + arranca + smoke).
# Sin clean: compila incremental, luego arranca spring-boot:run,
# sondea/v3/api-docs (publico, sin GLPI) y hace smoke de login.
#
# Uso:
#   bash tools/verify-backend.sh             -> solo compila (rapido)
#   bash tools/verify-backend.sh --run       -> compila + arranca + smoke + mata
#   bash tools/verify-backend.sh --skip-compile --run   -> arranca lo ya compilado
# =============================================================
set -euo pipefail

SKIP_COMPILE=false
RUN=false
for arg in "$@"; do
  case "$arg" in
    --run) RUN=true ;;
    --skip-compile) SKIP_COMPILE=true ;;
  esac
done

cd "$(dirname "$0")/../backend"

# mvnd (Maven daemon) si esta en PATH; si no, mvn clasico.
MVN=$(command -v mvnd || echo mvn)

if [[ "$SKIP_COMPILE" != "true" ]]; then
  echo "[1/2] $MVN compile (incremental, sin clean)..."
  "$MVN" compile -DskipTests -q
  echo "[ok] Compila."
fi

if [[ "$RUN" != "true" ]]; then
  exit 0
fi

echo "[2/2] Arrancando backend en :8001..."
LOG=$(mktemp)
"$MVN" spring-boot:run -q >"$LOG" 2>&1 &
PID=$!
trap 'kill "$PID" 2>/dev/null || true' EXIT

# Liveness: cualquier reepuesta de la app (200/404/401) = viva, no hace falta
# depender de GLPI. Ventana de 90s por si primer arranque compila plugins.
code=""
for i in $(seq 1 90); do
  code=$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8001/v3/api-docs || true)
  if [[ "$code" =~ ^(200|4[0-9]{2})$ ]]; then
    break
  fi
  sleep 1
done

if [[ ! "$code" =~ ^(200|4[0-9]{2})$ ]]; then
  echo "[FALLO] Sin respuesta en 90s. Ultimas lineas del log:"
  tail -30 "$LOG"
  exit 1
fi
echo "[ok] App responde (HTTP $code) tras ~${i}s."

# Smoke real: login con credenciales invalidas debe devolver 401 (app + security + DB vivos).
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST http://127.0.0.1:8001/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"verify_script_invalid","password":"verify_script_invalid"}' || true)
if [[ "$code" == "401" ]]; then
  echo "[ok] Login rechaza invalido (HTTP 401)."
else
  echo "[AVISO] Login devolvio HTTP $code (esperado 401). Revisar:"
  tail -30 "$LOG"
  exit 1
fi

# Solo problemas de verdad: nivel ERROR en log, o Exception que no sea la
# BadCredentials esperada del smoke ni el WARN benigno del AuthenticationManager.
ISSUES=$(grep -iE " ERROR |Exception" "$LOG" | grep -viE "BadCredentialsException|InitializeUserDetailsManager|Global AuthenticationManager|AuthenticationProvider bean" || true)
if [[ -n "$ISSUES" ]]; then
  echo "[AVISO] Problemas en el log del arranque:"
  echo "$ISSUES" | tail -15
fi

echo "[ok] Verificacion completa. Log: $LOG"