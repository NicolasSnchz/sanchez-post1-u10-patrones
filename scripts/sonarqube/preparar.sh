#!/usr/bin/env bash
# Deja listo un SonarQube Community recien levantado en Docker: clave de admin nueva,
# proyecto envios-service y un token de analisis.
set -euo pipefail

URL="http://localhost:9000"
PROYECTO="com.universidad:envios-service"
CLAVE="${CLAVE_SONAR:?Falta la variable CLAVE_SONAR}"

curl -sf -u admin:admin -X POST "$URL/api/users/change_password" \
  --data-urlencode "login=admin" --data-urlencode "previousPassword=admin" --data-urlencode "password=$CLAVE" \
  || echo "La clave de admin ya habia cambiado"
api() { curl -sf -u "admin:$CLAVE" -X POST "$URL/api/$1" "${@:2}"; }

# SonarQube 26.x trabaja en modo MQR (en la interfaz los Code Smells aparecen como issues de
# Maintainability), pero sigue calculando code_smells y new_code_smells, que son las que usa la guia.

api projects/create --data-urlencode "project=$PROYECTO" --data-urlencode "name=Envios Service" >/dev/null \
  || echo "El proyecto ya existia"

TOKEN=$(api user_tokens/generate --data-urlencode "name=analisis-$(date +%s)" | jq -r '.token')
if [ -n "${GITHUB_ENV:-}" ]; then
  echo "::add-mask::$TOKEN"
  echo "TOKEN_SONAR=$TOKEN" >> "$GITHUB_ENV"
else
  echo "Token para mvn sonar:sonar -Dsonar.token=...: $TOKEN"
fi
echo "Proyecto $PROYECTO creado y token generado"
