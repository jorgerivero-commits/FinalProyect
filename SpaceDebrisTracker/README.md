# Initial Project Proposal – Object-Oriented Programming

* **Degree Programme:** Software Engineering
* **Module:** Programming II
* **Term:** Second Midterm / Final Project
* **Student(s):** Jorge Rivero Justiniano
* **Submission Date:** 14 November 2026

---

## 1. General Project Information

| Field | Description |
| --- | --- |
| **Project Name:** | **Space Debris Tracker** (Space Control Centre)

 |
| **Application Type:** | ☑ Desktop (with an automated web ingestion script and external mathematical engine)

<br>

<br> ☐ Web   ☐ Mobile   ☐ Other: __________ |
| **Language / Development Environment:** | **Java** (with **JavaFX** for the graphical user interface and **JDBC** for persistence), integrated with **C++17** (Kepler + J2 orbital calculation engine), **Bash / Python** (data extraction), and **MySQL 8.0+** on a Linux/Ubuntu environment.

 |
| **Git Repository (URL):** | (https://github.com/jorgerivero-commits/FinalProyect) |
| **Use of Artificial Intelligence:** | ☐ No   ☑ Yes (describe below) |
| **If you use AI, briefly explain how and at which stage it contributes:** | AI was used as a support tool to structure the modular architecture of the project, integrate communication between Java, MySQL, and the C++ orbital engine, verify the application of OOP principles, and draft the technical documentation. |

---

## 2. Project Description

### Brief Summary

**Space Debris Tracker** is a desktop application that tracks and simulates the orbital positions of satellites and space stations. The system addresses the complexity of interpreting raw orbital data (TLE format) by automating its download from the web (CelesTrak) into a MySQL database. Using the stored records, the application executes a physics engine based on Kepler's laws and J2 perturbations to compute the future coordinates of each object. Finally, it displays the results in a periodically updated telemetry table and a 2D graph plotting altitude against longitude.

### Main Objectives

1. **Automated ingestion and storage:** Extract real orbital data (TLE elements) from the web (CelesTrak) and store them in a relational MySQL database, filtering objects with distinct orbits to prevent overlapping.


2. **Simulation and calculation of future coordinates:** Use the TLE data stored in the database to feed an orbital propagation engine (Kepler + J2) that computes the new geographical coordinates (latitude, longitude, and altitude) of the satellites.


3. **Tabular and graphical visualisation:** Display the simulated coordinates in a desktop interface via a structured data table and a 2D orbital graph (Altitude vs. Longitude) showing the projected location of each satellite in real time.



---

## 3. Technical Design and Application of OOP

### Applied OOP Principles

* ☑ **Encapsulation (private attributes and public methods):** Implemented in the `Satellite` model class through private attributes (`id`, `name`, `latitude`, `longitude`, `altitude`, `tleLine1`, `tleLine2`) protected with public getters and setters, as well as in the service and controller classes (`DatabaseService`, `OrbitCalculatorService`, and `MapController`).


* ☑ **Use of constructors:** Applied in the `Satellite` class via constructor overloading: an empty constructor to instantiate objects when reading TLEs from MySQL, and a parameterised constructor to initialise satellites with their calculated coordinates.


* ☑ **Inheritance:** Applied in the main `MainApp` class, which inherits from `javafx.application.Application` (`extends Application`) to initialise and manage the lifecycle of the primary window.


* ☑ **Polymorphism:** Applied by overriding the `start(Stage primaryStage)` method (`@Override`) in `MainApp` and through the polymorphic use of event handlers (`EventHandler`) in the JavaFX interface controls and timers.


* ☐ **Interfaces or abstract classes:** (No custom interfaces or abstract classes were defined in the domain model; only standard JavaFX functional interfaces are used for UI events).



### Data Persistence

* ☑ **Database:** Relational **MySQL** database (`space_debris_db`) connected via JDBC (`mysql-connector-j.jar`), structured into the `space_debris` table (TLE catalogue) and `orbital_positions` table (history of calculated positions).


* ☑ **Local files:** Temporary text file `./data/debris_tle.txt` used as a local buffer during download from CelesTrak prior to injecting records into MySQL.


* ☐ **In-memory (temporary)**
* ☐ **Other:** __________

---

## 4. Main Functionalities

| No. | Functionality Name | Brief Description | Current Status |
| --- | --- | --- | --- |
| **1** | **Web TLE data download and ingestion** | Automated script (`fetch_tle.sh`) that downloads orbital elements from CelesTrak, filters up to 5 satellites with distinct orbits, and saves them to MySQL.

 | ☐ Planned <br>

<br> ☑ In development |
| **2** | **MySQL persistence management** | `DatabaseService` class responsible for querying raw TLEs, logging each coordinate simulation into the history table (`orbital_positions`), and retrieving the latest positions.

 | ☐ Planned <br>

<br> ☑ In development |
| **3** | **Orbital simulation engine (Kepler + J2)** | Execution from Java (`OrbitCalculatorService`) of the compiled C++ engine (`kepler_j2_engine`) to calculate future latitude, longitude, and altitude from TLEs.

 | ☐ Planned <br>

<br> ☑ In development |
| **4** | **Data table and 2D orbital graph** | JavaFX interface (`MapController`) featuring two tabs: a data table (`TableView`) with each satellite's numerical values and a `Canvas` chart plotting Altitude (km) vs. Longitude (°).

 | ☐ Planned <br>

<br> ☑ In development |
| **5** | **Automatic updates and manual controls** | Automated loop (`Timeline`) that recalculates and refreshes the table and graph every 5 seconds, alongside buttons to force simulation, pause, resume, or reload from the database.

 | ☐ Planned <br>

 ### Cross-Platform Notes (Linux / macOS / Windows)

- **Linux & macOS:** Run `./bash/fetch_tle.sh` and `./run.sh` directly in a Bash terminal.
- **Windows (Git Bash / WSL):** Both `.sh` scripts work out of the box inside Git Bash or WSL.
- **Windows (PowerShell / CMD):** Set the environment variables from `.env` in your session, compile the C++ engine as `.exe`, and run Java using `;` as the classpath separator:
  powershell
  g++ -O2 -std=c++17 -o cpp/kepler_j2_engine.exe cpp/kepler_j2_engine.cpp
  javac --module-path "$env:FX_PATH" --add-modules javafx.controls,javafx.graphics,javafx.base -cp "bin;lib/mysql-connector-j.jar" java/com/spacedebris/model/*.java java/com/spacedebris/service/*.java java/com/spacedebris/ui/*.java -d bin
  java --module-path "$env:FX_PATH" --add-modules javafx.controls,javafx.graphics,javafx.base -cp "bin;lib/mysql-connector-j.jar" com.spacedebris.ui.MainApp

<br> ☑ In development |

---
