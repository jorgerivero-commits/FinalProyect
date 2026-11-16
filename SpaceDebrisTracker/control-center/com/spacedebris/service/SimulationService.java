package com.spacedebris.service;

import com.spacedebris.model.Satellite;

import java.time.Instant;
import java.util.List;

public class SimulationService {

  private final OrbitCalculatorService orbitService;
  private final DatabaseService dbService;

  private volatile long timeOffsetSeconds = 0L;
  private volatile String offsetDescription = "Tiempo Real";

  public SimulationService(OrbitCalculatorService orbitService, DatabaseService dbService) {
    this.orbitService = orbitService;
    this.dbService = dbService;
  }

  public void setTimeOffset(long offsetSeconds, String description) {
    this.timeOffsetSeconds = offsetSeconds;
    this.offsetDescription = (description != null && !description.isBlank()) ? description : "Personalizado";
  }

  public void resetToRealTime() {
    this.timeOffsetSeconds = 0L;
    this.offsetDescription = "Tiempo Real";
  }

  public long getTimeOffsetSeconds() {
    return timeOffsetSeconds;
  }

  public String getOffsetDescription() {
    return offsetDescription;
  }

  public long getTargetEpochSeconds() {
    return Instant.now().getEpochSecond() + timeOffsetSeconds;
  }

  public List<Satellite> runSimulationCycle() {
    long targetEpoch = getTargetEpochSeconds();
    System.out.println("Ejecutando ciclo de simulación (" + offsetDescription + ")...");

    List<Satellite> rawTles = dbService.getRawDebrisFromDatabase();
    List<Satellite> simulated = orbitService.propagateOrbits(rawTles, targetEpoch);

    if (simulated.isEmpty()) {
      System.err.println("Advertencia: El Motor Orbital no devolvió resultados en este ciclo.");
    } else {
      dbService.saveAllOrbitalPositions(simulated);
    }

    return dbService.getAllSatellites();
  }

  public List<Satellite> loadLatestPositions() {
    return dbService.getAllSatellites();
  }
}