#!/usr/bin/env bash
# mvn sonar:sonar sube el reporte y termina; el servidor lo procesa despues. Espera a que termine.
set -euo pipefail
URL="http://localhost:9000"
PROYECTO="com.universidad:envios-service"
for _ in $(seq 1 90); do
  estado=$(curl -sf -u "admin:$CLAVE_SONAR" "$URL/api/ce/component?component=$PROYECTO" \
    | jq -r 'if (.queue | length) > 0 then "PENDIENTE" else (.current.status // "NINGUNO") end')
  if [ "$estado" = "SUCCESS" ]; then echo "Analisis procesado"; exit 0; fi
  if [ "$estado" = "FAILED" ]; then echo "El servidor fallo al procesar el analisis"; exit 1; fi
  sleep 2
done
echo "El analisis no termino a tiempo"; exit 1
