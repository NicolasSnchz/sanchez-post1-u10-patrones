#!/usr/bin/env bash
# Deja listo un SonarQube Community recien levantado en Docker: clave de admin nueva,
# modo estandar (Code Smells clasicos), proyecto envios-service y un token de analisis.
set -euo pipefail

URL="http://localhost:9000"
PROYECTO="com.universidad:envios-service"
CLAVE="${CLAVE_SONAR:?Falta la variable CLAVE_SONAR}"

curl -sf -u admin:admin -X POST "$URL/api/users/change_password" \
  --data-urlencode "login=admin" --data-urlencode "previousPassword=admin" --data-urlencode "password=$CLAVE" \
  || echo "La clave de admin ya habia cambiado"
api() { curl -sf -u "admin:$CLAVE" -X POST "$URL/api/$1" "${@:2}"; }

# Modo estandar: la guia trabaja con Code Smells y New Code Smells, que son las metricas de este modo.
api settings/set --data-urlencode "key=sonar.multi-quality-mode.enabled" --data-urlencode "value=false" \
  || echo "Esta version no tiene modo MQR configurable"

api projects/create --data-urlencode "project=$PROYECTO" --data-urlencode "name=Envios Service" >/dev/null \
  || echo "El proyecto ya existia"

TOKEN=$(api user_tokens/generate --data-urlencode "name=analisis-$(date +%s)" | jq -r '.token')
echo "::add-mask::$TOKEN"
echo "TOKEN_SONAR=$TOKEN" >> "$GITHUB_ENV"
echo "Proyecto $PROYECTO creado y token generado"
