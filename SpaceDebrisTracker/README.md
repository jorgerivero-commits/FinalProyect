# Propuesta Inicial de Proyecto – Programación Orientada a Objetos

* **Carrera:** Ingeniería de Software[cite: 15]
* **Materia:** Programación II[cite: 15]
* **Periodo:** Segundo Parcial / Proyecto Final[cite: 15]
* **Estudiante(s):** Jorge Rivero Justiniano[cite: 15]
* **Fecha de entrega:** 2026-11-14[cite: 15]

---

## 1. Datos Generales del Proyecto

| Campo | Descripción |
| :--- | :--- |
| **Nombre del proyecto:** | **Space Debris Tracker** (Centro de Control Espacial)[cite: 5, 7, 15] |
| **Tipo de aplicación:** | ☑ Escritorio (con script automatizado de ingesta web y motor matemático externo)[cite: 13, 14, 15]<br>☐ Web ☐ Móvil ☐ Otro: __________ |
| **Lenguaje / entorno de desarrollo:** | **Java** (con **JavaFX** para la interfaz gráfica, **JDBC** para persistencia y `ScheduledService`/`Task` para concurrencia)[cite: 2, 6, 15], integrado con **C++17** (motor de cálculo orbital Kepler + J2)[cite: 11, 14, 15], **Bash / Python** (extracción y filtrado de datos TLE)[cite: 13, 15] y **MySQL 8.0+** en entorno Linux/Ubuntu[cite: 12, 15]. |
| **Repositorio Git (URL):** | https://github.com/jorgerivero-commits/FinalProyect[cite: 15] |
| **Uso de Inteligencia Artificial:** | ☐ No ☑ Sí[cite: 15] |
| **Si usas IA, explica brevemente cómo y en qué etapa contribuye:** | Se utilizó IA como herramienta de apoyo para estructurar la arquitectura modular del proyecto, integrar la comunicación entre Java, MySQL y el motor orbital en C++, verificar la aplicación de principios de POO y redactar la documentación técnica[cite: 15]. |

---

## 2. Descripción del Proyecto

### Resumen breve

**Space Debris Tracker** es una aplicación de escritorio dirigida a estudiantes, investigadores y entusiastas de la ingeniería aeroespacial que permite rastrear, visualizar y proyectar en el tiempo la posición orbital de satélites y desechos espaciales[cite: 7, 15]. El sistema resuelve la complejidad de interpretar datos orbitales crudos en formato TLE automatizando su descarga desde CelesTrak hacia una base de datos MySQL y ejecutando un motor físico en C++17 basado en las leyes de Kepler y perturbaciones seculares $J_2$[cite: 11, 13, 15]. Los resultados—incluyendo coordenadas geodésicas y velocidad orbital—se muestran en una tabla de telemetría y un gráfico 2D de Altitud vs. Longitud con soporte para saltos temporales[cite: 1, 7, 8, 9, 10, 15].

### Objetivos principales

1. **Ingesta y filtrado automatizado de datos orbitales:** Extraer elementos TLE reales desde CelesTrak (grupo `VISUAL`) mediante un script en Bash/Python (`tle-collector/fetch_tle.sh`) y almacenarlos en MySQL filtrando hasta 15 satélites con órbitas distintas para evitar superposiciones[cite: 12, 13, 15].
2. **Propagación física y proyección temporal de coordenadas:** Alimentar un motor orbital en C++17 (`orbital-engine/kepler_j2_engine`) con los TLE almacenados y una época Unix objetivo para calcular latitud, longitud y altitud WGS84 tanto en tiempo real como en proyecciones hacia fechas futuras o pasadas[cite: 3, 4, 10, 11, 15].
3. **Visualización tabular y gráfica de telemetría:** Presentar las coordenadas simuladas y la velocidad orbital calculada ($\text{km/s}$) en una interfaz JavaFX mediante una tabla estructurada (`SatelliteTableView`) y un gráfico orbital 2D de Altitud vs. Longitud (`OrbitChartView`)[cite: 1, 7, 8, 9, 15].

---

## 3. Diseño Técnico y Aplicación de POO

### Principios de POO aplicados

* ☑ **Encapsulamiento (atributos privados y métodos públicos):** Implementado en la clase modelo `Satellite` mediante atributos privados (`id`, `name`, `latitude`, `longitude`, `altitude`, `velocity`, `tleLine1`, `tleLine2`) y constantes físicas (`MU`, `EARTH_RADIUS_KM`) protegidos con getters y setters (donde `setAltitude()` calcula automáticamente `velocity`)[cite: 1], así como en los servicios (`DatabaseService`, `OrbitCalculatorService`, `SimulationService`) y componentes visuales (`MapController`, `MainView`, `OrbitChartView`, `SatelliteTableView`, `TimeJumpDialog`)[cite: 2, 3, 4, 6, 7, 8, 9, 10, 15].
* ☑ **Uso de constructores:** Aplicado en la clase `Satellite` mediante sobrecarga de constructores: un constructor vacío `Satellite()` para instanciar objetos al leer TLEs crudos desde MySQL y un constructor parametrizado `Satellite(int id, String name, double latitude, double longitude, double altitude)` para inicializar satélites con sus coordenadas calculadas[cite: 1, 2, 3, 15].
* ☑ **Herencia:** Aplicada en `MainApp` (`extends Application`) para gestionar el ciclo de vida de la ventana principal[cite: 5, 15], en todas las vistas personalizadas de JavaFX (`MainView extends BorderPane`, `OrbitChartView extends Pane`, `SatelliteTableView extends TableView<Satellite>`, `TimeJumpDialog extends Dialog<TimeJumpResult>`)[cite: 7, 8, 9, 10], y en las subclases anónimas de `ScheduledService` y `Task` dentro de `MapController` para ejecutar tareas en hilos secundarios[cite: 6].
* ☑ **Polimorfismo:** Aplicado mediante sobreescritura (`@Override`) en `MainApp.start(Stage)`[cite: 5, 15] y en los métodos `createTask()` y `call()` de `MapController`[cite: 6]; mediante sobrecarga en `OrbitCalculatorService.propagateOrbits(...)`[cite: 3] y en los constructores de `Satellite`[cite: 1]; y mediante polimorfismo de subtipos al integrar `OrbitChartView` y `SatelliteTableView` como nodos (`Node`/`Parent`) dentro de `MainView` y `Scene`[cite: 5, 6, 7, 8, 9].
* ☐ **Interfaces o clases abstractas:** No se definieron interfaces ni clases abstractas propias en el modelo de dominio[cite: 15]; se utilizan interfaces funcionales estándar de Java (`Runnable`, `BiConsumer<Long, String>`, `Callable`) para desacoplar las acciones de `MainView` y `TimeJumpDialog` respecto a `MapController`[cite: 6, 7, 10], además de abstracción por capas entre los servicios de datos y cálculo[cite: 2, 3, 4].

### Persistencia de datos

* ☑ **Archivos locales:** Archivo de texto temporal `./data/debris_tle.txt` utilizado como búfer local al descargar los conjuntos TLE desde CelesTrak antes de filtrarlos e inyectarlos en MySQL[cite: 13, 15].
* ☑ **Base de datos:** Base de datos relacional **MySQL** (`space_debris_db`, definida en `schema.sql`) conectada vía JDBC (`lib/mysql-connector-j.jar`), estructurada en las tablas `space_debris` (catálogo de TLEs) y `orbital_positions` (historial de posiciones calculadas con integridad referencial `ON DELETE CASCADE`)[cite: 2, 12, 14, 15].
* ☑ **En memoria (temporal):** Estado activo del desplazamiento temporal (`timeOffsetSeconds`, `offsetDescription`) mantenido en `SimulationService`[cite: 4] y colecciones observables (`ObservableList<Satellite>`) vinculadas a la interfaz JavaFX[cite: 9].
* ☐ **Otro:** __________

---

## 4. Funcionalidades Principales

| Nº | Nombre de la funcionalidad | Descripción breve | Estado actual |
| :--- | :--- | :--- | :--- |
| **1** | **Descarga y filtrado web de datos TLE** | Script automatizado (`tle-collector/fetch_tle.sh`) en Bash y Python que descarga elementos orbitales desde CelesTrak (grupo `VISUAL`), filtra hasta **15 satélites** con órbitas distintas y los guarda en MySQL[cite: 13, 15]. | ☐ Planeada ☐ En desarrollo ☑ Implementada |
| **2** | **Gestión de persistencia en MySQL** | Clase `DatabaseService` encargada de consultar TLEs crudos, registrar lotes de coordenadas simuladas en `orbital_positions` mediante transacciones JDBC y recuperar las últimas posiciones[cite: 2, 15]. | ☐ Planeada ☐ En desarrollo ☑ Implementada |
| **3** | **Motor de propagación orbital (Kepler + J2)** | Ejecución desde Java (`OrbitCalculatorService`) del binario compilado en C++17 (`orbital-engine/kepler_j2_engine`) para calcular latitud, longitud y altitud WGS84 a partir de los TLE[cite: 3, 11, 14, 15]. | ☐ Planeada ☐ En desarrollo ☑ Implementada |
| **4** | **Tabla de telemetría y gráfico orbital 2D** | Interfaz JavaFX (`MainView`) con dos pestañas: `SatelliteTableView` (muestra ID, nombre, latitud, longitud, altitud y velocidad orbital en $\text{km/s}$) y `OrbitChartView` (gráfico `Canvas` de Altitud vs. Longitud)[cite: 1, 7, 8, 9, 15]. | ☐ Planeada ☐ En desarrollo ☑ Implementada |
| **5** | **Simulación automática y controles manuales** | Bucle automático en `MapController` (`ScheduledService` y `Task` cada 5 segundos en hilos secundarios) junto con botones para forzar simulación, pausar, reanudar o recargar desde MySQL[cite: 6, 7, 15]. | ☐ Planeada ☐ En desarrollo ☑ Implementada |
| **6** | **Proyección temporal (Viaje en el tiempo)** | Permite simular posiciones futuras o pasadas mediante intervalos predefinidos (`+1 Hora` a `+1 Año`) en `MainView` o saltos personalizados por duración y fecha exacta en calendario mediante `TimeJumpDialog`[cite: 4, 7, 10]. | ☐ Planeada ☐ En desarrollo ☑ Implementada |