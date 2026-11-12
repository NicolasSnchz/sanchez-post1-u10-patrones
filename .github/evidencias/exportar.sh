#!/usr/bin/env bash
# Guarda en evidencias/<etapa>/ las metricas reales del ultimo analisis procesado.
set -uo pipefail
ETAPA="$1"
URL="http://localhost:9000"
P="com.universidad:envios-service"
D="evidencias/$ETAPA"
mkdir -p "$D"
get() { curl -sf -u "admin:$CLAVE_SONAR" "$URL/api/$1"; }
M="complexity,cognitive_complexity,coverage,line_coverage,branch_coverage,lines_to_cover,uncovered_lines,conditions_to_cover,uncovered_conditions,code_smells,new_code_smells,new_coverage,duplicated_lines_density,ncloc,functions,sqale_index"
get "measures/component?component=$P&metricKeys=$M" > "$D/proyecto.json"
get "measures/component_tree?component=$P&qualifiers=FIL&metricKeys=$M&ps=100" > "$D/archivos.json"
get "issues/search?componentKeys=$P&resolved=false&ps=500" > "$D/issues.json"
get "qualitygates/project_status?projectKey=$P" > "$D/quality-gate.json"
get "qualitygates/get_by_project?project=$P" > "$D/quality-gate-asignado.json"
get "qualitygates/show?name=$(jq -rn --arg n "Estándar Laboratorio" '$n|@uri')" > "$D/quality-gate-condiciones.json" || true
get "settings/values?keys=sonar.multi-quality-mode.enabled" > "$D/modo.json" || true
get "qualityprofiles/search?project=$P" > "$D/quality-profile.json"
get "rules/show?key=java:S1541" > "$D/regla-s1541.json"
get "project_analyses/search?project=$P&ps=10" > "$D/analisis.json"
echo "Metricas de la etapa $ETAPA guardadas"
