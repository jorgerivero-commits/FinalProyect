package com.spacedebris.ui.view;

import com.spacedebris.model.Satellite;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;

public class MainView extends BorderPane {

  private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  private final SatelliteTableView tableView = new SatelliteTableView();
  private final OrbitChartView chartView = new OrbitChartView();

  private final Label statusIndicatorLabel = new Label();
  private final Label simulatedClockLabel = new Label();

  private final Button btnRunCpp = new Button("🔄 Forzar Simulación C++");
  private final Button btnStopAuto = new Button("⏹️ Detener Auto");
  private final Button btnStartAuto = new Button("▶️ Reanudar Auto");
  private final Button btnLoadDb = new Button("📂 Cargar desde MySQL");

  private final ComboBox<String> presetCombo = new ComboBox<>();
  private final Button btnCustomTime = new Button("📅 Salto Personalizado...");
  private final Button btnResetTime = new Button("⏪ Volver a Ahora");

  private BiConsumer<Long, String> onTimeJump;
  private Runnable onResetTime;
  private boolean updatingCombo = false;

  public MainView() {
    setPadding(new Insets(15));

    Label titleLabel = new Label("🛰️ Space Debris Tracker - Centro de Control Espacial");
    titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

    setAutoSimulationActive(true);
    updateSimulatedEpoch(Instant.now().getEpochSecond(), "Tiempo Real", false);

    HBox statusRow = new HBox(20, statusIndicatorLabel, simulatedClockLabel);
    statusRow.setAlignment(Pos.CENTER_LEFT);

    VBox topBox = new VBox(5, titleLabel, statusRow);
    topBox.setPadding(new Insets(0, 0, 15, 0));
    setTop(topBox);

    TabPane tabPane = new TabPane();
    tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    tabPane.tabMinWidthProperty().bind(tabPane.widthProperty().divide(2).subtract(24));
    tabPane.tabMaxWidthProperty().bind(tabPane.widthProperty().divide(2).subtract(24));

    Tab tableTab = new Tab("📋 Panel de Datos", tableView);
    Tab mapTab = new Tab("🌍 Perfil Orbital (Altitud vs Longitud)", chartView);

    tabPane.getTabs().addAll(tableTab, mapTab);
    setCenter(tabPane);

    // Fila 1: Controles de simulación
    btnRunCpp.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
    btnStopAuto.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;");
    btnStartAuto.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white;");
    btnLoadDb.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white;");

    HBox simulationBar = new HBox(10, btnRunCpp, btnStopAuto, btnStartAuto, btnLoadDb);
    simulationBar.setAlignment(Pos.CENTER_LEFT);

    // Fila 2: Proyección temporal / Viaje en el tiempo
    Label timeLabel = new Label("⏩ Proyección Temporal:");
    timeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

    presetCombo.getItems().addAll(
        "Tiempo Real (Ahora)",
        "+1 Hora",
        "+1 Día",
        "+1 Semana",
        "+1 Mes",
        "+2 Meses",
        "+6 Meses",
        "+1 Año");
    presetCombo.setValue("Tiempo Real (Ahora)");
    presetCombo.setOnAction(e -> handlePresetSelection());

    btnCustomTime.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-weight: bold;");
    btnCustomTime.setOnAction(e -> openCustomTimeDialog());

    btnResetTime.setStyle("-fx-background-color: #34495e; -fx-text-fill: white;");
    btnResetTime.setDisable(true);
    btnResetTime.setOnAction(e -> {
      if (onResetTime != null) {
        onResetTime.run();
      }
    });

    HBox timeBar = new HBox(10, timeLabel, presetCombo, btnCustomTime, btnResetTime);
    timeBar.setAlignment(Pos.CENTER_LEFT);

    VBox bottomBox = new VBox(10, simulationBar, timeBar);
    bottomBox.setPadding(new Insets(12, 0, 0, 0));
    setBottom(bottomBox);
  }

  private void handlePresetSelection() {
    if (updatingCombo || onTimeJump == null) {
      return;
    }

    String choice = presetCombo.getValue();
    if (choice == null) {
      return;
    }

    ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
    ZonedDateTime target;

    switch (choice) {
      case "+1 Hora" -> target = now.plusHours(1);
      case "+1 Día" -> target = now.plusDays(1);
      case "+1 Semana" -> target = now.plusWeeks(1);
      case "+1 Mes" -> target = now.plusMonths(1);
      case "+2 Meses" -> target = now.plusMonths(2);
      case "+6 Meses" -> target = now.plusMonths(6);
      case "+1 Año" -> target = now.plusYears(1);
      default -> {
        if (onResetTime != null) {
          onResetTime.run();
        }
        return;
      }
    }

    long offsetSeconds = java.time.Duration.between(now, target).getSeconds();
    onTimeJump.accept(offsetSeconds, choice);
  }

  private void openCustomTimeDialog() {
    TimeJumpDialog dialog = new TimeJumpDialog();
    dialog.initOwner(getScene() != null ? getScene().getWindow() : null);

    dialog.showAndWait().ifPresent(result -> {
      updatingCombo = true;
      presetCombo.setValue(result.getOffsetSeconds() == 0 ? "Tiempo Real (Ahora)" : null);
      presetCombo.setPromptText(result.getDescription());
      updatingCombo = false;

      if (onTimeJump != null) {
        onTimeJump.accept(result.getOffsetSeconds(), result.getDescription());
      }
    });
  }

  public void updateSatellites(List<Satellite> satellites) {
    tableView.updateData(satellites);
    chartView.updateChart(satellites);
  }

  public void updateSimulatedEpoch(long epochSeconds, String description, boolean isProjected) {
    ZonedDateTime zdt = Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault());
    String formatted = zdt.format(CLOCK_FORMAT);

    if (isProjected) {
      simulatedClockLabel.setText("🚀 Época Simulada: " + formatted + " (" + description + ")");
      simulatedClockLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8e44ad; -fx-font-weight: bold;");
      btnResetTime.setDisable(false);
    } else {
      simulatedClockLabel.setText("🕒 Época Simulada: " + formatted + " (Tiempo Real)");
      simulatedClockLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50; -fx-font-weight: bold;");
      btnResetTime.setDisable(true);
      updatingCombo = true;
      presetCombo.setValue("Tiempo Real (Ahora)");
      updatingCombo = false;
    }
  }

  public void setAutoSimulationActive(boolean active) {
    if (active) {
      statusIndicatorLabel.setText("⏱️ Estado: Simulación ACTIVA (Actualización cada 5s)");
      statusIndicatorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
    } else {
      statusIndicatorLabel.setText("⏹️ Estado: Simulación PAUSADA (Modo de inspección manual)");
      statusIndicatorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #c0392b; -fx-font-weight: bold;");
    }
  }

  public void setOnRunSimulation(Runnable action) {
    btnRunCpp.setOnAction(e -> action.run());
  }

  public void setOnStopAuto(Runnable action) {
    btnStopAuto.setOnAction(e -> action.run());
  }

  public void setOnStartAuto(Runnable action) {
    btnStartAuto.setOnAction(e -> action.run());
  }

  public void setOnLoadFromDb(Runnable action) {
    btnLoadDb.setOnAction(e -> action.run());
  }

  public void setOnTimeJump(BiConsumer<Long, String> action) {
    this.onTimeJump = action;
  }

  public void setOnResetTime(Runnable action) {
    this.onResetTime = action;
  }
}