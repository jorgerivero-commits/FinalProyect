package com.spacedebris.ui;

import com.spacedebris.model.Satellite;
import com.spacedebris.service.SimulationService;
import com.spacedebris.ui.view.MainView;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.Callable;

public class MapController {

  private final MainView view = new MainView();
  private final SimulationService simulationService;
  private final ScheduledService<List<Satellite>> autoSimulationService;
  private boolean autoRunning = false;

  public MapController(SimulationService simulationService) {
    this.simulationService = simulationService;

    this.autoSimulationService = new ScheduledService<>() {
      @Override
      protected Task<List<Satellite>> createTask() {
        return new Task<>() {
          @Override
          protected List<Satellite> call() {
            return simulationService.runSimulationCycle();
          }
        };
      }
    };
    autoSimulationService.setPeriod(Duration.seconds(5));
    autoSimulationService.setOnSucceeded(e -> refreshView(autoSimulationService.getValue()));
    autoSimulationService.setOnFailed(e ->
        System.err.println("❌ Error en simulación automática: " + autoSimulationService.getException().getMessage()));

    wireEvents();
    startAutoSimulation();
  }

  private void wireEvents() {
    view.setOnRunSimulation(this::forceSimulation);
    view.setOnStopAuto(this::stopAutoSimulation);
    view.setOnStartAuto(this::startAutoSimulation);
    view.setOnLoadFromDb(() -> runAsync(simulationService::loadLatestPositions));

    view.setOnTimeJump((offsetSeconds, description) -> {
      simulationService.setTimeOffset(offsetSeconds, description);
      refreshClock();
      forceSimulation();
    });

    view.setOnResetTime(() -> {
      simulationService.resetToRealTime();
      refreshClock();
      forceSimulation();
    });
  }

  private void refreshView(List<Satellite> satellites) {
    view.updateSatellites(satellites);
    refreshClock();
  }

  private void refreshClock() {
    view.updateSimulatedEpoch(
        simulationService.getTargetEpochSeconds(),
        simulationService.getOffsetDescription(),
        simulationService.getTimeOffsetSeconds() != 0L);
  }

  private void forceSimulation() {
    if (autoRunning) {
      autoSimulationService.restart();
    } else {
      runAsync(simulationService::runSimulationCycle);
    }
  }

  private void startAutoSimulation() {
    autoRunning = true;
    autoSimulationService.restart();
    view.setAutoSimulationActive(true);
    System.out.println("⏱️ [SISTEMA] Bucle automático de simulación ACTIVADO.");
  }

  private void stopAutoSimulation() {
    autoRunning = false;
    autoSimulationService.cancel();
    view.setAutoSimulationActive(false);
    System.out.println("⏹️ [SISTEMA] Bucle automático de simulación DETENIDO.");
  }

  private void runAsync(Callable<List<Satellite>> work) {
    Task<List<Satellite>> task = new Task<>() {
      @Override
      protected List<Satellite> call() throws Exception {
        return work.call();
      }
    };
    task.setOnSucceeded(e -> refreshView(task.getValue()));
    task.setOnFailed(e ->
        System.err.println("❌ Error en tarea asíncrona: " + task.getException().getMessage()));

    Thread worker = new Thread(task, "simulation-worker");
    worker.setDaemon(true);
    worker.start();
  }

  public BorderPane getRootLayout() {
    return view;
  }

  public void shutdown() {
    autoSimulationService.cancel();
  }
}