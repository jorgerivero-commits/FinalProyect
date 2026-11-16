package com.spacedebris.ui;

import com.spacedebris.service.DatabaseService;
import com.spacedebris.service.OrbitCalculatorService;
import com.spacedebris.service.SimulationService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

  @Override
  public void start(Stage primaryStage) {
    OrbitCalculatorService orbitService = new OrbitCalculatorService();
    DatabaseService dbService = new DatabaseService();
    SimulationService simulationService = new SimulationService(orbitService, dbService);

    MapController controller = new MapController(simulationService);

    Scene scene = new Scene(controller.getRootLayout(), 960, 680);
    primaryStage.setTitle("Space Debris Tracker - Centro de Control Espacial");
    primaryStage.setScene(scene);
    primaryStage.setOnCloseRequest(e -> controller.shutdown());
    primaryStage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}