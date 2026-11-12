#!/usr/bin/env bash
# Paso 9 de la guia: Quality Profile y Quality Gate "Estandar Laboratorio" asignados a envios-service.
set -euo pipefail

URL="http://localhost:9000"
PROYECTO="com.universidad:envios-service"
CLAVE="${CLAVE_SONAR:?Falta la variable CLAVE_SONAR}"
NOMBRE="Estándar Laboratorio"
api() { curl -sf -u "admin:$CLAVE" -X POST "$URL/api/$1" "${@:2}"; }
get() { curl -sf -u "admin:$CLAVE" "$URL/api/$1"; }

# Quality Profile: copia del perfil integrado por defecto (no se puede editar directamente) con S1541
# activa y umbral 10. La guia lo llama "Sonar Way"; en SonarQube 26.x el perfil por defecto de Java
# se llama "Sonar way core", y es el que se uso en el analisis inicial.
ORIGEN=$(get "qualityprofiles/search?language=java" | jq -r '.profiles[] | select(.isDefault) | .key')
echo "Se duplica el perfil: $(get "qualityprofiles/search?language=java" | jq -r '.profiles[] | select(.isDefault) | .name')"
echo "Perfiles de Java disponibles: $(get "qualityprofiles/search?language=java" | jq -r '[.profiles[].name] | join(", ")')"
PERFIL=$(api qualityprofiles/copy --data-urlencode "fromKey=$ORIGEN" --data-urlencode "toName=$NOMBRE" | jq -r '.key')
PARAMETRO=$(get "rules/show?key=java:S1541" | jq -r '.rule.params[0].key')
echo "La regla java:S1541 se llama: $(get "rules/show?key=java:S1541" | jq -r '.rule.name')"
echo "Su parametro de umbral se llama: $PARAMETRO"
api qualityprofiles/activate_rule --data-urlencode "key=$PERFIL" --data-urlencode "rule=java:S1541" \
  --data-urlencode "params=$PARAMETRO=10"
api qualityprofiles/add_project --data-urlencode "language=java" --data-urlencode "qualityProfile=$NOMBRE" \
  --data-urlencode "project=$PROYECTO"
echo "Quality Profile \"$NOMBRE\" asignado con java:S1541 en 10"

# Quality Gate con las tres condiciones de la guia.
api qualitygates/create --data-urlencode "name=$NOMBRE" >/dev/null
# Desde SonarQube 10 una puerta nueva nace con las condiciones de "Clean as You Code" (issues nuevos,
# hotspots revisados, cobertura y duplicacion del codigo nuevo). La guia pide exactamente tres, asi que se quitan.
for id in $(get "qualitygates/show?name=$(jq -rn --arg n "$NOMBRE" '$n|@uri')" | jq -r '.conditions[].id'); do
  api qualitygates/delete_condition --data-urlencode "id=$id"
  echo "Condicion predeterminada $id eliminada"
done
condicion() {
  api qualitygates/create_condition --data-urlencode "gateName=$NOMBRE" --data-urlencode "metric=$1" \
    --data-urlencode "op=$2" --data-urlencode "error=$3" >/dev/null
  echo "Condicion: $1 $2 $3"
}
condicion coverage LT 70                 # Coverage is less than 70 %
condicion new_code_smells GT 0           # New Code Smells is greater than 0
condicion duplicated_lines_density GT 5  # Duplicated Lines (%) is greater than 5 %
api qualitygates/select --data-urlencode "gateName=$NOMBRE" --data-urlencode "projectKey=$PROYECTO"
echo "Quality Gate \"$NOMBRE\" asignado con estas condiciones:"
get "qualitygates/show?name=$(jq -rn --arg n "$NOMBRE" '$n|@uri')" | jq -r '.conditions[] | "  \(.metric) \(.op) \(.error)"'
