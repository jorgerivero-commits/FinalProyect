package com.spacedebris.service;

import com.spacedebris.model.Satellite;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrbitCalculatorService {

  private final File engineBinary;

  public OrbitCalculatorService() {
    boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
    this.engineBinary = new File(
        isWindows ? "./orbital-engine/kepler_j2_engine.exe" : "./orbital-engine/kepler_j2_engine");
  }

  public List<Satellite> propagateOrbits(List<Satellite> rawDebrisList) {
    return propagateOrbits(rawDebrisList, Instant.now().getEpochSecond());
  }

  public List<Satellite> propagateOrbits(List<Satellite> rawDebrisList, long targetEpochSeconds) {
    List<Satellite> updatedSatellites = new ArrayList<>();

    if (rawDebrisList == null || rawDebrisList.isEmpty()) {
      System.err.println("Advertencia: No hay TLEs para enviar al Motor Orbital.");
      return updatedSatellites;
    }

    if (!engineBinary.exists()) {
      System.err.println("Advertencia: Binario del Motor Orbital no encontrado en " + engineBinary.getPath());
      return updatedSatellites;
    }

    try {
      ProcessBuilder pb = new ProcessBuilder(
          engineBinary.getPath(),
          String.valueOf(targetEpochSeconds));
      pb.redirectErrorStream(true);
      Process process = pb.start();

      try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
        for (Satellite sat : rawDebrisList) {
          writer.write(sat.getId() + "\n");
          writer.write(sat.getName() + "\n");
          writer.write(sat.getTleLine1() + "\n");
          writer.write(sat.getTleLine2() + "\n");
        }
        writer.flush();
      }

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.startsWith("DEBRIS_DATA:")) {
            String[] parts = line.substring(12).split("\\|");
            if (parts.length >= 5) {
              int id = Integer.parseInt(parts[0].trim());
              String name = parts[1].trim();
              double lat = Double.parseDouble(parts[2].trim());
              double lon = Double.parseDouble(parts[3].trim());
              double alt = Double.parseDouble(parts[4].trim());

              updatedSatellites.add(new Satellite(id, name, lat, lon, alt));
            }
          } else {
            System.out.println("[Motor Orbital C++]: " + line);
          }
        }
      }

      process.waitFor();

    } catch (Exception e) {
      System.err.println("Error al ejecutar el Motor Orbital C++ (Kepler + J2): " + e.getMessage());
    }

    return updatedSatellites;
  }
}