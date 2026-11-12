# Envios Service: diagnóstico y mejora guiada por métricas

Post-contenido de la unidad 10 de Patrones de Diseño de Software (UDES). Nicolás Andrés Sánchez Villamizar.

`envios-service` calcula el costo de un envío según el método de transporte, el peso, la distancia, la fragilidad y el tipo de cliente. En la Parte 1 el cálculo vivía en un solo método con complejidad ciclomática 20, y la suite de partida cubría apenas el 18,4 % de sus ramas. En la Parte 2 lo refactoricé con Strategy, agregué las pruebas que faltaban, configuré el Quality Profile y el Quality Gate «Estándar Laboratorio», y volví a medir. Todos los números de este README salen de análisis reales de SonarQube y JaCoCo: las capturas están en [`docs/`](docs) y las respuestas originales de la API, en [`docs/analisis`](docs/analisis).

| | Antes | Después |
|---|---|---|
| Complejidad ciclomática de `calcularCosto()` | 20 | **5** (75 % menos) |
| Cobertura de ramas | 18,4 % | **100 %** |
| Quality Gate «Estándar Laboratorio» | Failed | **Passed** |

## Contenido

1. [Decisiones de diseño](#decisiones-de-diseño)
2. [Parte 1: análisis inicial](#parte-1-análisis-inicial)
3. [Parte 2: refactorización guiada por métricas](#parte-2-refactorización-guiada-por-métricas)
4. [Comparación antes / después](#comparación-antes--después)
5. [Capturas](#capturas)
6. [Inconsistencias del enunciado y cómo las resolví](#inconsistencias-del-enunciado-y-cómo-las-resolví)
7. [Estructura, ejecución y herramientas](#estructura-ejecución-y-herramientas)
8. [Conclusiones](#conclusiones)

## Decisiones de diseño

### 1. Entorno de análisis: SonarQube Community en Docker

Usé la imagen oficial `sonarqube:community` (SonarQube Community Build 26.11.0.134175), levantada con el mismo `docker run` del Paso 1 dentro de un runner de GitHub Actions. La elegí sobre SonarCloud por dos criterios de la tabla de la guía:

- **No depende de una cuenta ni de los límites de un plan gratuito.** SonarCloud exige entrar con la cuenta de GitHub y vincular el repositorio; con Docker basta la imagen.
- **Control total de la configuración.** El proyecto, el token, el Quality Profile y el Quality Gate se crean por script con la API de SonarQube ([`scripts/sonarqube`](scripts/sonarqube)), así que el análisis se puede repetir idéntico en cualquier máquina con Docker.

El costo que acepto es el de la tabla: el servidor no queda encendido y el dashboard desaparece al terminar cada análisis. Por eso guardo las capturas y las respuestas reales de la API en `docs/`.

### 2. Exclusiones del análisis: ninguna

Todo el código de `src/main/java` es lógica de negocio: no hay una clase `Main` ni código generado. Excluir algo solo maquillaría el porcentaje sin decir nada nuevo sobre la calidad. La línea de `sonar.exclusions` queda comentada y explicada en [`sonar-project.properties`](sonar-project.properties).

### 3. Métrica usada como señal principal: complejidad ciclomática

De las tres señales que expone SonarQube, elegí la complejidad ciclomática porque en este caso explica a las otras dos. Una CC de 20 significa al menos 20 caminos independientes que probar, y con dos pruebas es imposible pasar de una cobertura de ramas baja: el 18,4 % es consecuencia de esa complejidad, no un problema aparte. La complejidad cognitiva (32) confirma el mismo método, pero mide qué tan difícil es leerlo, no cuántos caminos hay que cubrir. La cobertura sola dice *que* faltan pruebas, pero no *por qué* cuesta tanto escribirlas. Las tres coinciden en `calcularCosto()` porque es el único método con lógica; en un proyecto más grande podrían señalar métodos distintos, y la CC seguiría siendo la mejor guía para decidir qué refactorizar.

### 4. Patrón elegido: Strategy

Antes de escribir código revisé qué hacen los condicionales de `calcularCosto()` con la tabla del Paso 7:

| Pregunta de la guía | En este método | Patrón que descarta |
|---|---|---|
| ¿Guarda algún estado entre llamadas que condicione su comportamiento futuro? | No. `ServicioCalculoEnvio` no tiene atributos que cambien; cada llamada depende solo de sus parámetros. | **State**, que necesita un objeto que recuerde en qué estado está y transiciones válidas entre estados. |
| ¿Hay una secuencia de responsables donde el orden importa y más de uno podría aplicar? | No. Para un mismo envío aplica exactamente una rama de `metodoEnvio`, y elegir ESTANDAR descarta las demás. | **Chain of Responsibility**, que tiene sentido cuando la solicitud recorre manejadores en orden hasta que alguno la resuelve. |
| ¿Cada rama calcula el mismo tipo de resultado con su propia fórmula, elegida una sola vez por un valor de entrada? | Sí. Las cuatro ramas devuelven un costo, cada una con sus tarifas por kilómetro, tramos de peso y recargos, y se elige una sola vez por `metodoEnvio`. | Es la señal de **Strategy**. |

**Qué se movió y qué se quedó.** Cada fórmula por método de envío pasó a su estrategia, y el orquestador ([línea 19](https://github.com/NicolasSnchz/sanchez-post1-u10-patrones/blob/main/src/main/java/com/universidad/enviosservice/service/ServicioCalculoEnvio.java#L19)) quedó con CC 5: elegir la estrategia del mapa, rechazar el método no soportado y aplicar fragilidad y tipo de cliente. Esos dos ajustes se quedaron a propósito en el orquestador: son reglas transversales a los cuatro métodos, y meterlas en cada estrategia las habría duplicado cuatro veces.

**Un ajuste que pidió la propia métrica.** Con las cuatro estrategias escritas como el ejemplo de la guía, el segundo análisis marcó **11,5 % de líneas duplicadas** (Estándar y Express quedaban casi idénticas) y el Quality Gate falló por la condición de duplicación. En lugar de solo documentarlo, lo corregí guiado por esa medición: agregué [`EstrategiaPorTramos`](https://github.com/NicolasSnchz/sanchez-post1-u10-patrones/blob/main/src/main/java/com/universidad/enviosservice/estrategia/EstrategiaPorTramos.java), una clase base que implementa `EstrategiaEnvio` y fija el esqueleto común (Template Method): cobro por kilómetro, cargo fijo y cargo por peso en tres tramos ([línea 41](https://github.com/NicolasSnchz/sanchez-post1-u10-patrones/blob/main/src/main/java/com/universidad/enviosservice/estrategia/EstrategiaPorTramos.java#L41)). Cada estrategia concreta solo declara sus tarifas y su ajuste por distancia; OVERNIGHT además conserva su `IllegalArgumentException` para más de 300 km ([línea 10](https://github.com/NicolasSnchz/sanchez-post1-u10-patrones/blob/main/src/main/java/com/universidad/enviosservice/estrategia/EnvioOvernightStrategy.java#L10)). Strategy sigue siendo el patrón que resuelve el condicional; la clase base solo evita repetir la estructura. Con eso la duplicación bajó a 0 % y el gate pasó.

### 5. Criterio de pruebas agregadas

Cada prueba nueva cubre una rama que el reporte de JaCoCo de la Parte 1 dejó sin ejercitar, o un valor límite de esa rama, y verifica un valor esperado concreto. Los números de línea son los del [método original](https://github.com/NicolasSnchz/sanchez-post1-u10-patrones/blob/9203d407736cde8e3c3a47065814b94311d2fbe3/src/main/java/com/universidad/enviosservice/service/ServicioCalculoEnvio.java), que sigue en el historial:

| Rama sin cubrir en la Parte 1 (línea del método original) | Prueba que la cubre ahora |
|---|---|
| ESTANDAR de más de 5 kg y de más de 20 kg (9 y 11) | `EnvioEstandarStrategyTest.calculaCostoPorTramoDePeso`: 8 kg y 25 kg, más los límites exactos de 5 y 20 kg |
| Recargo de ESTANDAR por más de 500 km (16) | `EnvioEstandarStrategyTest.aplicaRecargoSoloPorEncimaDe500Km`: 500 km sin recargo y 501 km con recargo |
| Todo EXPRESS (19 a 27) | `EnvioExpressStrategyTest`: los tres tramos, los dos límites y el recargo, con el factor 1,3 |
| Todo OVERNIGHT, incluida la excepción (31 a 37) | `EnvioOvernightStrategyTest`: los tres tramos, 300 km exactos aceptados y `rechazaDistanciasMayoresA300Km` con 301 km |
| Todo INTERNACIONAL (42 a 50) | `EnvioInternacionalStrategyTest`: los tres tramos, los límites y el recargo de más de 5000 km |
| Método no soportado (54) | `ServicioCalculoEnvioTest.rechazaMetodoDeEnvioNoSoportado`, que verifica también el mensaje |
| Fragilidad (57) | `ServicioCalculoEnvioTest.aplicaAjustesPorTipoDeClienteYFragilidad`: fragilidad sola y fragilidad antes del descuento PREMIUM |
| Descuento CORPORATIVO (63) | `ServicioCalculoEnvioTest.aplicaAjustesPorTipoDeClienteYFragilidad` |
| Que el orquestador use la estrategia correcta | `ServicioCalculoEnvioTest.delegaEnLaEstrategiaDeCadaMetodo` para EXPRESS, OVERNIGHT e INTERNACIONAL |

Los valores límite (exactamente 5 kg, 20 kg, 300 km, 500 km y 5000 km) son los que más fácil se rompen si alguien cambia un `<=` por un `<`, y la suite inicial no tenía ninguno. Las dos pruebas originales siguen tal como las da la guía. En total pasaron de 2 a 35 casos, todos en verde.

### 6. Realismo del Quality Gate

- **CC máxima de 10 por método.** La Tabla 3.2 de la guía pone 10 como techo del rango «Moderada», y además es el umbral por defecto de la propia regla S1541 en SonarQube. Con 5 no quedaría margen: el orquestador ya está justo en 5 y cualquier regla nueva obligaría a partirlo aunque todavía se lea bien. Con 20 el `calcularCosto()` original, que la guía clasifica como «Alta», habría pasado sin aviso.
- **70 % de cobertura y no 100 %.** El 100 % empuja a escribir pruebas triviales solo para llenar el número: la métrica deja de medir calidad y se vuelve la meta. Con 70 % se exige probar lo importante sin obligar a mentir con pruebas vacías. Aquí la cobertura final quedó en 100 % porque cada línea del cálculo es lógica de negocio, no por perseguir la cifra.
- **New Code Smells mayor que 0 y duplicación mayor que 5 %.** Son las condiciones que convierten la regla S1541 en un bloqueo real para el código nuevo. La de duplicación demostró que no es decorativa: fue la que tumbó el primer intento de la Parte 2.

**Resultado:** el gate falla con la versión inicial (cobertura 21,3 %, menor a 70 %) y pasa con la versión final, con las tres condiciones en verde.

## Parte 1: análisis inicial

Valores reales del análisis de `mvn clean verify sonar:sonar` sobre la suite inicial, con el perfil por defecto:

| Métrica | Valor real observado | Fuente |
|---|---|---|
| Complejidad ciclomática de `calcularCosto()` | **20** | JaCoCo por método (columna Cxty) y SonarQube por archivo |
| Complejidad cognitiva de `ServicioCalculoEnvio` | **32** | SonarQube |
| Cobertura de líneas de `ServicioCalculoEnvio` | **23,8 %** (10 de 42 líneas) | SonarQube y JaCoCo |
| Cobertura de ramas de `ServicioCalculoEnvio` | **18,4 %** (7 de 38 condiciones) | SonarQube y JaCoCo |
| Code Smells reportados | **0** | SonarQube con el perfil por defecto («Sonar way core») |

**Qué método concentra el problema.** `calcularCosto()`, el único método con lógica. JaCoCo le asigna CC 20, con solo 2 caminos cubiertos, y SonarQube le da 20 al archivo completo; coincide con la cuenta de la guía (1 + 19 puntos de decisión). Las dos pruebas solo recorren envíos ESTANDAR de 3 kg a 100 km, así que quedan sin cubrir todas las ramas de la tabla de la [Decisión 5](#5-criterio-de-pruebas-agregadas).

**Por qué 0 Code Smells con CC 20.** El perfil por defecto de esta versión no reportó ningún issue: la métrica existe en el dashboard, pero ninguna regla activa la vigila. Al activar S1541 con umbral 10, la misma versión inicial del código pasó a tener 1 Code Smell, con este mensaje de SonarQube: *«The Cyclomatic Complexity of this method "calcularCosto" is 20 which is greater than 10 authorized»* ([captura](docs/sonar-s1541-calcularcosto.png)).

## Parte 2: refactorización guiada por métricas

**Código resultante.**

```text
src/main/java/com/universidad/enviosservice/
├── service/ServicioCalculoEnvio.java          orquestador: elige la estrategia y aplica fragilidad y cliente (CC 5)
└── estrategia/
    ├── EstrategiaEnvio.java                   interfaz Strategy
    ├── EstrategiaPorTramos.java               esqueleto común por tramos (Template Method)
    ├── EnvioEstandarStrategy.java             tarifas y recargo de más de 500 km
    ├── EnvioExpressStrategy.java              tarifas, recargo de más de 500 km y factor 1,3
    ├── EnvioOvernightStrategy.java            tarifas y rechazo de más de 300 km
    └── EnvioInternacionalStrategy.java        tarifas y recargo de más de 5000 km
```

Según JaCoCo, ningún método supera ahora CC 5: el orquestador queda en 5, `cargoPorPeso` en 3 y cada ajuste o validación por distancia en 2 o menos.

**Quality Profile y Quality Gate.** Los crea [`estandar-laboratorio.sh`](https://github.com/NicolasSnchz/sanchez-post1-u10-patrones/blob/main/scripts/sonarqube/estandar-laboratorio.sh): duplica el perfil por defecto con el nombre «Estándar Laboratorio», activa `java:S1541` con `Threshold = 10` y lo asigna al proyecto. Luego crea el gate «Estándar Laboratorio» con exactamente las tres condiciones de la guía y lo asigna. El registro real de esa configuración está en [`docs/analisis/regla/configuracion.log`](docs/analisis/regla/configuracion.log).

**Línea base del código nuevo.** La condición «New Code Smells» compara contra una línea base. Antes del refactor analicé la versión inicial con el perfil nuevo y fijé ese análisis como punto de partida del código nuevo. Así el análisis final evalúa exactamente lo que cambió en la Parte 2.

**El segundo análisis, en dos tiempos.**

| Análisis | Duplicación | Code Smells nuevos | Cobertura | Quality Gate |
|---|---|---|---|---|
| Strategy tal como el ejemplo de la guía | 11,5 % | 0 | 100 % | **Failed** (duplicación mayor a 5 %) |
| Strategy con la clase base por tramos (final) | **0,0 %** | **0** | **100 %** | **Passed** |

**El refactor no cambió el comportamiento.** Además de las 35 pruebas, comparé la versión original y la final del cálculo ejecutándolas lado a lado en 2.400 combinaciones de método de envío (incluido uno no soportado), tipo de cliente, fragilidad, pesos y distancias alrededor de cada límite. Dieron el mismo costo y las mismas excepciones en todas.

## Comparación antes / después

| Métrica | Antes (Parte 1) | Después (Parte 2) |
|---|---|---|
| Complejidad ciclomática de `calcularCosto()` | 20 | **5** (75 % menos; la guía pide al menos 50 %) |
| Complejidad máxima de cualquier método | 20 | **5** |
| Complejidad cognitiva del proyecto | 32 | **10** |
| Cobertura de ramas de la lógica de envío | 18,4 % (7 de 38) | **100 %** (20 de 20); suben 81,6 puntos y la guía pide al menos 15 |
| Cobertura de líneas | 23,8 % | **100 %** |
| Code Smells | 0 con el perfil por defecto; 1 al activar S1541 | **0** |
| Duplicación | 0,0 % | **0,0 %** |
| Pruebas | 2 | **35** |
| Quality Gate «Estándar Laboratorio» | Failed (cobertura 21,3 %) | **Passed** |

## Capturas

**Dashboard inicial y final de SonarQube**

![Dashboard inicial](docs/sonar-dashboard-inicial.png)

![Dashboard final con el Quality Gate en Passed](docs/sonar-dashboard-final.png)

**Reporte de JaCoCo inicial y final**

![JaCoCo inicial](docs/jacoco-inicial.png)

![JaCoCo final](docs/jacoco-final.png)

**Detalle del análisis inicial**

![Complejidad por método en JaCoCo: calcularCosto con Cxty 20](docs/jacoco-inicial-metodo.png)

![Ramas sin cubrir de calcularCosto en JaCoCo](docs/jacoco-inicial-codigo.png)

![Complejidad ciclomática inicial en SonarQube](docs/sonar-complejidad-inicial.png)

![Cobertura de ramas inicial en SonarQube](docs/sonar-cobertura-ramas-inicial.png)

![mvn clean verify sonar:sonar inicial con BUILD SUCCESS](docs/mvn-sonar-inicial.png)

**Quality Profile, regla S1541 y Quality Gate**

![Quality Profile Estándar Laboratorio](docs/sonar-quality-profile.png)

![Regla S1541 con Threshold 10](docs/sonar-regla-s1541.png)

![S1541 detecta calcularCosto con complejidad 20](docs/sonar-s1541-calcularcosto.png)

![Quality Gate Estándar Laboratorio con sus tres condiciones](docs/sonar-quality-gate.png)

![Quality Gate asignado al proyecto](docs/sonar-proyecto-quality-gate.png)

![Quality Profile asignado al proyecto](docs/sonar-proyecto-quality-profile.png)

**Primer intento de la Parte 2: el gate falla por duplicación**

![Quality Gate en Failed por 11,5 % de duplicación](docs/sonar-dashboard-estrategia.png)

**Detalle del análisis final**

![Código nuevo del análisis final](docs/sonar-dashboard-codigo-nuevo-final.png)

![Complejidad final en SonarQube](docs/sonar-complejidad-final.png)

![JaCoCo de las estrategias](docs/jacoco-estrategias-final.png)

![JaCoCo del orquestador](docs/jacoco-orquestador-final.png)

![mvn clean verify sonar:sonar final con las 35 pruebas y BUILD SUCCESS](docs/mvn-sonar-final.png)

## Inconsistencias del enunciado y cómo las resolví

| Qué dice el enunciado | El problema | Cómo lo resolví |
|---|---|---|
| En el Paso 8, el caso de 25 kg de OVERNIGHT espera 250,0 | Con la fórmula del propio método da 100 × 2 + 20 + 15 × 2,5 + 5 × 3 = 272,5; esa prueba falla tal como está escrita | Uso 272,5 y lo dejo comentado en la prueba ([línea 16](https://github.com/NicolasSnchz/sanchez-post1-u10-patrones/blob/main/src/test/java/com/universidad/enviosservice/estrategia/EnvioOvernightStrategyTest.java#L16)) |
| El Paso 3 configura el análisis en `sonar-project.properties` y el Paso 4 lo corre con `mvn sonar:sonar` | El escáner de Maven no lee ese archivo; toma la configuración del `pom.xml` | El archivo queda comentado (sirve para `sonar-scanner`) y los mismos valores están en las propiedades del [`pom.xml`](https://github.com/NicolasSnchz/sanchez-post1-u10-patrones/blob/main/pom.xml) |
| El Paso 5 dice que la CC por método se ve en la pestaña Code de SonarQube | SonarQube muestra la complejidad por archivo, no por método | La CC por método sale de la columna Cxty de JaCoCo y del mensaje de la regla S1541 |
| El Paso 9 pide duplicar «Sonar Way» y buscar la regla «Cyclomatic Complexity of methods should not exceed the authorized threshold» | En SonarQube 26.11 el perfil por defecto de Java se llama «Sonar way core» y S1541 se llama «Methods should not be too complex» (su parámetro sí es `Threshold`) | El script duplica el perfil por defecto y verifica el nombre y el parámetro de la regla por API antes de activarla |
| El Paso 9 pide un Quality Gate con tres condiciones | En SonarQube 26.11 un gate nuevo nace con cuatro condiciones adicionales («Clean as You Code»), como se ve en el registro de configuración | El script las elimina y deja exactamente las tres de la guía |
| La condición «New Code Smells is greater than 0» | En un primer análisis no hay código nuevo y la condición no se evalúa | Fijé como línea base el análisis de la versión inicial con el perfil nuevo |
| La implementación de referencia del Paso 7 | Sus estrategias Estándar y Express quedan casi idénticas: 11,5 % de duplicación, y su propio gate falla | Clase base por tramos (ver [Decisión 4](#4-patrón-elegido-strategy)) |
| El enunciado da por hecho que el arquetipo Maven deja un `.gitignore` | El arquetipo `quickstart` no lo genera | Agregué un [`.gitignore`](.gitignore) con `target/` y archivos del IDE |
| La guía habla de Code Smells | SonarQube 26.x trabaja en modo MQR y en la interfaz los muestra como issues de Maintainability | Las métricas `code_smells` y `new_code_smells` se siguen calculando, y son las que usa el gate |

## Estructura, ejecución y herramientas

```text
pom.xml                     JaCoCo (prepare-agent y report) y propiedades de SonarQube
sonar-project.properties    configuración del análisis, comentada
src/main/java/com/universidad/enviosservice/{service,estrategia}
src/test/java/com/universidad/enviosservice/{service,estrategia}
scripts/sonarqube           preparar SonarQube, esperar el análisis y crear el perfil y el gate
docs/                       capturas; docs/analisis guarda las respuestas reales de la API
```

```bash
docker run -d --name sonarqube -p 9000:9000 -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true sonarqube:community
bash scripts/sonarqube/preparar.sh             # con CLAVE_SONAR definida; imprime el token
bash scripts/sonarqube/estandar-laboratorio.sh # perfil y gate de la Parte 2
mvn clean verify sonar:sonar -Dsonar.token=TU_TOKEN
```

El reporte de JaCoCo queda en `target/site/jacoco/index.html`.

**Herramientas:** Java 21, Apache Maven, JUnit 5, JaCoCo 0.8.13, SonarQube Community Build 26.11 en Docker, GitHub Actions, Git y GitHub.

## Conclusiones

Lo más valioso fue ver que las métricas no son un adorno del final sino la guía del trabajo: la complejidad de 20 explicaba por sí sola la cobertura tan baja, y bajarla a 5 con Strategy fue lo que volvió posible cubrir el 100 % de las ramas con pruebas que verifican valores reales, no pruebas de relleno. También aprendí que elegir el patrón exige mirar la naturaleza de los condicionales y no solo su cantidad: aquí no había estado ni una cadena de responsables, sino fórmulas intercambiables elegidas una sola vez. El momento más útil fue cuando el Quality Gate falló por duplicación con la implementación de referencia, porque obligó a mejorar el diseño con una clase base en vez de aceptar el número. Por último, comprobé que una herramienta configurada por defecto puede no vigilar nada: con el perfil inicial, un método de complejidad 20 no generaba ni un Code Smell hasta que se activó la regla correcta.
