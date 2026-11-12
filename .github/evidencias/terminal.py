"""Convierte las lineas clave de un registro real de Maven en una pagina con estilo de terminal."""
import html
import re
import sys
from pathlib import Path

registro, titulo, salida = sys.argv[1], sys.argv[2], sys.argv[3]
lineas = Path(registro).read_text(encoding="utf-8", errors="replace").splitlines()
claves = ("Tests run:", "Sensor JaCoCo", "ANALYSIS SUCCESSFUL", "QUALITY GATE", "BUILD SUCCESS", "BUILD FAILURE",
          "Total time", "Finished at", "Analyzed bundle", "Loading execution data", "Quality profile for java")
elegidas = [re.sub(r"^\d{4}-\d\d-\d\dT[\d:.]+Z ", "", l) for l in lineas if any(c in l for c in claves)]


def color(linea):
    texto = html.escape(linea)
    if "BUILD SUCCESS" in linea or "ANALYSIS SUCCESSFUL" in linea or "Failures: 0, Errors: 0" in linea:
        return f"<span style='color:#3fb950'>{texto}</span>"
    if "FAILURE" in linea or "ERROR" in linea:
        return f"<span style='color:#f85149'>{texto}</span>"
    return texto


Path(salida).write_text(
    "<!doctype html><meta charset='utf-8'><style>body{margin:0;background:#0d1117;color:#c9d1d9;"
    "font:14px/1.5 ui-monospace,Menlo,Consolas,monospace}.b{background:#161b22;padding:10px 18px;color:#8b949e;"
    "border-bottom:1px solid #30363d;font-family:system-ui,sans-serif}pre{margin:0;padding:18px;white-space:pre-wrap}"
    f"</style><div class='b'>{html.escape(titulo)}</div><pre>{chr(10).join(color(l) for l in elegidas)}</pre>",
    encoding="utf-8")
print(len(elegidas), "lineas")
