// Capturas reales de SonarQube y de JaCoCo para cada etapa del analisis.
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const BASE = 'http://localhost:9000';
const P = 'com.universidad:envios-service';
const ARCHIVO = `${P}:src/main/java/com/universidad/enviosservice/service/ServicioCalculoEnvio.java`;
const e = encodeURIComponent;
const etapa = process.argv[2];
const jacoco = process.argv[3];
const salida = `evidencias/${etapa}`;

const vistas = {
  inicial: [
    ['sonar-dashboard', `/dashboard?id=${e(P)}&codeScope=overall`],
    ['sonar-complejidad', `/component_measures?id=${e(P)}&metric=complexity&view=list`],
    ['sonar-cobertura-ramas', `/component_measures?id=${e(P)}&metric=branch_coverage&view=list`],
    ['sonar-codigo', `/code?id=${e(P)}&selected=${e(ARCHIVO)}`],
    ['sonar-issues', `/project/issues?id=${e(P)}&resolved=false`],
  ],
  regla: [
    ['sonar-quality-profile', `/profiles/show?language=java&name=${e('Estándar Laboratorio')}`],
    ['sonar-regla-s1541', `/coding_rules?languages=java&rule_key=java%3AS1541&open=java%3AS1541`],
    ['sonar-quality-gate', `/quality_gates/show/${e('Estándar Laboratorio')}`],
    ['sonar-issues-s1541', `/project/issues?id=${e(P)}&resolved=false&rules=java%3AS1541`],
  ],
  final: [
    ['sonar-dashboard', `/dashboard?id=${e(P)}&codeScope=overall`],
    ['sonar-dashboard-codigo-nuevo', `/dashboard?id=${e(P)}&codeScope=new`],
    ['sonar-complejidad', `/component_measures?id=${e(P)}&metric=complexity&view=list`],
    ['sonar-cobertura-ramas', `/component_measures?id=${e(P)}&metric=branch_coverage&view=list`],
    ['sonar-issues', `/project/issues?id=${e(P)}&resolved=false`],
    ['sonar-proyecto-quality-gate', `/project/quality_gate?id=${e(P)}`],
    ['sonar-proyecto-quality-profile', `/project/quality_profiles?id=${e(P)}`],
  ],
};

const paginasJacoco = {
  inicial: [
    ['jacoco', 'index.html'],
    ['jacoco-clase', 'com.universidad.enviosservice.service/ServicioCalculoEnvio.html'],
    ['jacoco-codigo', 'com.universidad.enviosservice.service/ServicioCalculoEnvio.java.html'],
  ],
  regla: [],
  final: [
    ['jacoco', 'index.html'],
    ['jacoco-estrategias', 'com.universidad.enviosservice.estrategia/index.html'],
    ['jacoco-orquestador', 'com.universidad.enviosservice.service/ServicioCalculoEnvio.html'],
  ],
};

(async () => {
  fs.mkdirSync(salida, { recursive: true });
  const navegador = await chromium.launch();
  const pagina = await navegador.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2 });
  await pagina.goto(`${BASE}/sessions/new`);
  await pagina.fill('input[name="login"]', 'admin');
  await pagina.fill('input[name="password"]', process.env.CLAVE_SONAR);
  await pagina.press('input[name="password"]', 'Enter');
  await pagina.waitForURL((url) => !url.pathname.startsWith('/sessions'));
  for (const [nombre, ruta] of vistas[etapa]) {
    await pagina.goto(BASE + ruta, { waitUntil: 'networkidle' });
    await pagina.waitForTimeout(3000);
    await pagina.screenshot({ path: `${salida}/${nombre}.png`, fullPage: true });
  }
  for (const [nombre, archivo] of paginasJacoco[etapa]) {
    const ruta = path.resolve(jacoco, archivo);
    if (!fs.existsSync(ruta)) { console.log('No existe', ruta); continue; }
    await pagina.goto('file://' + ruta);
    await pagina.screenshot({ path: `${salida}/${nombre}.png`, fullPage: true });
  }
  for (const archivo of fs.readdirSync(salida).filter((f) => f.endsWith('.html'))) {
    const terminal = await navegador.newPage({ viewport: { width: 1300, height: 600 }, deviceScaleFactor: 2 });
    await terminal.goto('file://' + path.resolve(salida, archivo));
    await terminal.screenshot({ path: `${salida}/${archivo.replace('.html', '.png')}`, fullPage: true });
    await terminal.close();
  }
  await navegador.close();
})().catch((error) => { console.error(error); process.exit(1); });
