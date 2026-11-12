# Envios Service: análisis de métricas de calidad

Post-contenido de la unidad 10 de Patrones de Diseño de Software (UDES). Nicolás Andrés Sánchez Villamizar.

`envios-service` calcula el costo de un envío según el método de transporte, el peso, la distancia, la fragilidad y el tipo de cliente. En esta primera parte el código está tal como lo da la guía: [`ServicioCalculoEnvio`](src/main/java/com/universidad/enviosservice/service/ServicioCalculoEnvio.java) concentra toda la lógica en un solo método y la [suite de partida](src/test/java/com/universidad/enviosservice/service/ServicioCalculoEnvioTest.java) solo tiene dos pruebas. Aquí no se corrige nada todavía: se mide, se lee el reporte real y se documenta.

## Entorno de análisis

**Decisión de diseño 1: SonarQube Community en Docker.** Usé la imagen oficial `sonarqube:community` (versión 26.11.0.134175) con el mismo comando del Paso 1, ejecutada dentro de un runner de GitHub Actions. Lo elegí sobre SonarCloud por dos criterios de la tabla de la guía:

- **No depende de una cuenta ni de los límites de un plan gratuito.** SonarCloud exige entrar con la cuenta de GitHub y vincular el repositorio; con Docker basta la imagen.
- **Control total de la configuración.** El proyecto, el token y, en la Parte 2, el Quality Profile y el Quality Gate se crean por script con la API de SonarQube ([`scripts/sonarqube`](scripts/sonarqube)), así que cualquiera puede repetir exactamente el mismo análisis.

El costo que acepto es el de la tabla: el servidor no queda encendido. Cada análisis arranca un SonarQube nuevo y el dashboard desaparece al terminar. Por eso guardo las capturas en `docs/` y las respuestas reales de la API en [`docs/analisis`](docs/analisis), para que los números se puedan revisar después.

## Exclusiones del análisis

**Decisión de diseño 2: no excluir nada.** Todo el código de `src/main/java` es lógica de negocio: no hay una clase `Main` ni código generado que distorsione la cobertura. Excluir algo solo haría que el porcentaje se viera mejor sin decir nada nuevo sobre la calidad. Lo dejé escrito en [`sonar-project.properties`](sonar-project.properties), con la línea de `sonar.exclusions` comentada.

## Estado inicial del análisis

Valores reales del análisis de [`mvn clean verify sonar:sonar`](docs/mvn-sonar-inicial.png) sobre el commit de la suite inicial:

| Métrica | Valor real observado | Fuente |
|---|---|---|
| Complejidad ciclomática de `calcularCosto()` | **20** | JaCoCo por método (columna Cxty) y SonarQube por archivo |
| Complejidad cognitiva de `ServicioCalculoEnvio` | **32** | SonarQube |
| Cobertura de líneas de `ServicioCalculoEnvio` | **23,8 %** (10 de 42 líneas) | SonarQube y JaCoCo |
| Cobertura de ramas de `ServicioCalculoEnvio` | **18,4 %** (7 de 38 condiciones) | SonarQube y JaCoCo |
| Code Smells reportados | **0** | SonarQube con el perfil por defecto («Sonar way core») |
| Quality Gate por defecto | Passed | SonarQube |

## Interpretación

**Qué método concentra el problema.** Las dos señales apuntan a `calcularCosto()`, el único método con lógica de la clase. JaCoCo le asigna una complejidad de 20, de la cual solo 2 caminos están cubiertos, y SonarQube le da 20 al archivo completo. Coincide con la cuenta de la guía (1 + 19 puntos de decisión).

**Qué ramas quedaron sin cubrir.** El reporte HTML de JaCoCo ([`docs/jacoco-inicial-codigo.png`](docs/jacoco-inicial-codigo.png)) marca 31 de las 38 condiciones sin ejercitar. Las dos pruebas solo recorren envíos ESTANDAR de 3 kg a 100 km, así que quedan sin cubrir:

| Línea | Condición | Qué falta |
|---|---|---|
| 8 | `metodoEnvio.equals("ESTANDAR")` | El caso falso: ningún otro método de envío |
| 9 y 11 | Tramos de peso de ESTANDAR | Más de 5 kg y más de 20 kg |
| 16 | `distanciaKm > 500` en ESTANDAR | El recargo por distancia |
| 19 a 27 | Todo EXPRESS | Sus tres tramos de peso y su recargo |
| 31 a 37 | Todo OVERNIGHT | La excepción de más de 300 km y sus tres tramos |
| 42 a 50 | Todo INTERNACIONAL | Sus tres tramos y el recargo de más de 5000 km |
| 54 | `else` final | La excepción de método no soportado |
| 57 | `esFragil` | El recargo por fragilidad |
| 63 | `tipoCliente.equals("CORPORATIVO")` | El descuento corporativo |

**Por qué hay 0 Code Smells con una complejidad de 20.** El perfil que SonarQube asigna por defecto en esta versión («Sonar way core») no reportó ningún issue sobre el método. La métrica de complejidad existe en el dashboard, pero ninguna regla activa la está vigilando. Por eso, en la Parte 2, se activa la regla S1541 con umbral 10.

**Decisión de diseño 3: la complejidad ciclomática como señal principal.** De las tres señales disponibles, elegí la complejidad ciclomática porque es la que explica a las otras dos en este caso. Una CC de 20 significa al menos 20 caminos independientes que probar, y con dos pruebas es imposible pasar de una cobertura de ramas baja: el 18,4 % es consecuencia directa de esa complejidad, no un problema aparte. La complejidad cognitiva (32) confirma el mismo método, pero mide qué tan difícil es leerlo, no cuántos caminos hay que cubrir. La cobertura sola dice *que* faltan pruebas, pero no *por qué* cuesta tanto escribirlas. Las tres coinciden porque la clase tiene un único método con lógica; en un proyecto más grande podrían apuntar a métodos distintos, y ahí la CC seguiría siendo la mejor guía para decidir qué refactorizar.

## Capturas del análisis inicial

![Dashboard de SonarQube del análisis inicial](docs/sonar-dashboard-inicial.png)

![Reporte de JaCoCo del análisis inicial](docs/jacoco-inicial.png)

![Complejidad por método en el reporte de JaCoCo](docs/jacoco-inicial-metodo.png)

![Ramas sin cubrir en el código de calcularCosto según JaCoCo](docs/jacoco-inicial-codigo.png)

![Complejidad ciclomática en SonarQube](docs/sonar-complejidad-inicial.png)

![Cobertura de ramas en SonarQube](docs/sonar-cobertura-ramas-inicial.png)

![Salida de mvn clean verify sonar:sonar con BUILD SUCCESS](docs/mvn-sonar-inicial.png)

## Cómo ejecutar el análisis

```bash
docker run -d --name sonarqube -p 9000:9000 -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true sonarqube:community
mvn clean verify sonar:sonar -Dsonar.token=TU_TOKEN
```

El reporte de JaCoCo queda en `target/site/jacoco/index.html`.
