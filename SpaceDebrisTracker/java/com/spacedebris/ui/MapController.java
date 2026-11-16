package com.spacedebris.ui;

import com.spacedebris.model.Satellite;
import com.spacedebris.service.DatabaseService;
import com.spacedebris.service.OrbitCalculatorService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.List;

public class MapController {

  private static final double MAP_WIDTH = 900;
  private static final double MAP_HEIGHT = 450;

  // Márgenes internos para que las etiquetas de los ejes X e Y no se encimen
  private static final double PAD_LEFT = 60;
  private static final double PAD_RIGHT = 25;
  private static final double PAD_TOP = 25;
  private static final double PAD_BOTTOM = 30;

  private static final Color[] PALETTE = {
      Color.web("#e74c3c"), Color.web("#f1c40f"), Color.web("#2ecc71"),
      Color.web("#00d2ff"), Color.web("#e056fd"), Color.web("#ff9f43")
  };

  private BorderPane rootLayout;
  private TableView<Satellite> table;
  private ObservableList<Satellite> satelliteData = FXCollections.observableArrayList();

  private OrbitCalculatorService orbitService = new OrbitCalculatorService();
  private DatabaseService dbService = new DatabaseService();

  private Timeline simulationTimeline;
  private Label statusIndicatorLabel;
  private Canvas orbitCanvas;

  public MapController() {
    buildUI();
    loadDataFromDatabase();
    handleRunSimulation(); // primer cálculo inmediato, sin esperar los 5 s
    startAutoSimulation();
  }

  private void buildUI() {
    rootLayout = new BorderPane();
    rootLayout.setPadding(new Insets(15));

    Label titleLabel = new Label("🛰️ Space Debris Tracker - Centro de Control Espacial");
    titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

    statusIndicatorLabel = new Label("⏱️ Estado: Simulación ACTIVA (Actualización cada 5s)");
    statusIndicatorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");

    VBox topBox = new VBox(5, titleLabel, statusIndicatorLabel);
    topBox.setPadding(new Insets(0, 0, 15, 0));
    rootLayout.setTop(topBox);

    TabPane tabPane = new TabPane();
    tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

    // Pestaña 1: tabla de datos
    Tab tableTab = new Tab("📋 Panel de Datos");
    table = new TableView<>();
    table.setItems(satelliteData);

    TableColumn<Satellite, Integer> idCol = new TableColumn<>("ID");
    idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
    idCol.setPrefWidth(80);

    TableColumn<Satellite, String> nameCol = new TableColumn<>("Objeto / Satélite");
    nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
    nameCol.setPrefWidth(220);

    TableColumn<Satellite, Double> latCol = new TableColumn<>("Latitud (°)");
    latCol.setCellValueFactory(new PropertyValueFactory<>("latitude"));
    latCol.setPrefWidth(120);

    TableColumn<Satellite, Double> lonCol = new TableColumn<>("Longitud (°)");
    lonCol.setCellValueFactory(new PropertyValueFactory<>("longitude"));
    lonCol.setPrefWidth(120);

    TableColumn<Satellite, Double> altCol = new TableColumn<>("Altitud (km)");
    altCol.setCellValueFactory(new PropertyValueFactory<>("altitude"));
    altCol.setPrefWidth(120);

    table.getColumns().addAll(List.of(idCol, nameCol, latCol, lonCol, altCol));
    tableTab.setContent(table);

    // Pestaña 2: perfil altitud vs longitud
    Tab mapTab = new Tab("🌍 Perfil Orbital (Altitud vs Longitud)");
    orbitCanvas = new Canvas(MAP_WIDTH, MAP_HEIGHT);
    VBox mapContainer = new VBox(orbitCanvas);
    mapContainer.setStyle("-fx-background-color: #0b1d3a; -fx-alignment: center;");
    mapTab.setContent(mapContainer);

    tabPane.getTabs().addAll(tableTab, mapTab);
    rootLayout.setCenter(tabPane);

    // Botones inferiores
    Button btnRunCpp = new Button("🔄 Forzar Simulación C++");
    btnRunCpp.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
    btnRunCpp.setOnAction(e -> handleRunSimulation());

    Button btnStopAuto = new Button("⏹️ Detener Auto");
    btnStopAuto.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;");
    btnStopAuto.setOnAction(e -> stopAutoSimulation());

    Button btnStartAuto = new Button("▶️ Reanudar Auto");
    btnStartAuto.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white;");
    btnStartAuto.setOnAction(e -> startAutoSimulation());

    Button btnLoadDb = new Button("📂 Cargar desde MySQL");
    btnLoadDb.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white;");
    btnLoadDb.setOnAction(e -> loadDataFromDatabase());

    HBox bottomBox = new HBox(10, btnRunCpp, btnStopAuto, btnStartAuto, btnLoadDb);
    bottomBox.setPadding(new Insets(15, 0, 0, 0));
    rootLayout.setBottom(bottomBox);
  }

  private void startAutoSimulation() {
    if (simulationTimeline == null) {
      simulationTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> handleRunSimulation()));
      simulationTimeline.setCycleCount(Timeline.INDEFINITE);
    }
    simulationTimeline.play();

    if (statusIndicatorLabel != null) {
      statusIndicatorLabel.setText("⏱️ Estado: Simulación ACTIVA (Actualización cada 5s)");
      statusIndicatorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }
    System.out.println("⏱️ [SISTEMA] Bucle automático de simulación ACTIVADO.");
  }

  private void stopAutoSimulation() {
    if (simulationTimeline != null) {
      simulationTimeline.stop();
      if (statusIndicatorLabel != null) {
        statusIndicatorLabel.setText("⏱️ Estado: Simulación PAUSADA (Modo de inspección manual)");
        statusIndicatorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #c0392b; -fx-font-weight: bold;");
      }
      System.out.println("⏹️ [SISTEMA] Bucle automático de simulación DETENIDO.");
    }
  }

  private void handleRunSimulation() {
    System.out.println("Ejecutando simulación C++ (Kepler + J2)...");
    List<Satellite> simulated = orbitService.fetchSimulatedOrbits();

    if (simulated.isEmpty()) {
      System.err.println("Advertencia: No se obtuvieron datos del motor C++.");
      return;
    }

    for (Satellite sat : simulated) {
      dbService.saveOrbitalPosition(sat.getId(), sat.getLatitude(), sat.getLongitude(), sat.getAltitude());
    }

    loadDataFromDatabase();
  }

  // Tabla y gráfico se alimentan de MySQL: se refrescan siempre juntos.
  private void loadDataFromDatabase() {
    List<Satellite> dbList = dbService.getAllSatellites();
    satelliteData.setAll(dbList);
    drawOrbitMap(dbList);
  }

  // --- Conversión de coordenadas al área del gráfico ---
  private static double lonToX(double lon, double plotW) {
    return PAD_LEFT + (lon + 180.0) / 360.0 * plotW;
  }

  private static double altToY(double alt, double plotH, double maxChartAlt) {
    double clampedAlt = Math.max(0.0, alt);
    return PAD_TOP + plotH - (clampedAlt / maxChartAlt) * plotH;
  }

  // Calcula un paso limpio en km para las líneas horizontales del eje Y
  private static double computeNiceStep(double maxVal) {
    double rawStep = maxVal / 5.0;
    double magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
    double residual = rawStep / magnitude;
    if (residual <= 1.0) return 1.0 * magnitude;
    if (residual <= 2.0) return 2.0 * magnitude;
    if (residual <= 5.0) return 5.0 * magnitude;
    return 10.0 * magnitude;
  }

  // --- GRÁFICO 2D: Longitud (°) en eje X vs Altitud (km) en eje Y ---
  private void drawOrbitMap(List<Satellite> satellites) {
    if (orbitCanvas == null)
      return;

    GraphicsContext gc = orbitCanvas.getGraphicsContext2D();
    double w = orbitCanvas.getWidth();
    double h = orbitCanvas.getHeight();
    double plotW = w - PAD_LEFT - PAD_RIGHT;
    double plotH = h - PAD_TOP - PAD_BOTTOM;

    // 1. Determinar altura máxima: satélite más alto + 10 unidades (km) de margen
    double highestAlt = 0.0;
    for (Satellite sat : satellites) {
      if (sat.getAltitude() > highestAlt) {
        highestAlt = sat.getAltitude();
      }
    }
    if (highestAlt <= 0.0) {
      highestAlt = 400.0; // respaldo si aún no hay datos
    }
    double maxChartAlt = highestAlt + 100.0; 

    // 2. Fondo general
    gc.setFill(Color.web("#0b1d3a"));
    gc.fillRect(0, 0, w, h);

    gc.setFont(Font.font(10));

    // 3. Eje Y: Altitud desde 0 km hasta maxChartAlt
    double stepKm = computeNiceStep(maxChartAlt);
    gc.setTextAlign(TextAlignment.RIGHT);
    for (double alt = 0.0; alt <= maxChartAlt; alt += stepKm) {
      double y = altToY(alt, plotH, maxChartAlt);
      gc.setLineWidth(alt == 0.0 ? 1.5 : 1.0);
      gc.setStroke(alt == 0.0 ? Color.web("#2ecc71") : Color.web("#1e3a60"));
      gc.strokeLine(PAD_LEFT, y, PAD_LEFT + plotW, y);

      gc.setFill(alt == 0.0 ? Color.web("#2ecc71") : Color.web("#6c8ebf"));
      gc.fillText(Math.round(alt) + " km", PAD_LEFT - 6, y + 3);
    }

    // Línea de techo (maxAlt + 10 km)
    double topY = altToY(maxChartAlt, plotH, maxChartAlt);
    gc.setLineWidth(1.0);
    gc.setStroke(Color.web("#3d5a80"));
    gc.strokeLine(PAD_LEFT, topY, PAD_LEFT + plotW, topY);
    gc.setFill(Color.web("#9bb8e0"));
    gc.fillText(String.format("%.0f km", maxChartAlt), PAD_LEFT - 6, topY + 3);

    // 4. Eje X: Longitud en grados (-180° a 180°)
    gc.setTextAlign(TextAlignment.CENTER);
    gc.setLineWidth(1.0);
    for (int lon = -180; lon <= 180; lon += 30) {
      double x = lonToX(lon, plotW);
      gc.setStroke(lon == 0 ? Color.web("#3d5a80") : Color.web("#1e3a60"));
      gc.strokeLine(x, PAD_TOP, x, PAD_TOP + plotH);

      gc.setFill(Color.web("#6c8ebf"));
      gc.fillText(lon + "°", x, PAD_TOP + plotH + 16);
    }

    // 5. Satélites (solo punto actual y etiqueta, sin rastro)
    for (Satellite sat : satellites) {
      Color color = PALETTE[Math.abs(sat.getId()) % PALETTE.length];

      double x = lonToX(sat.getLongitude(), plotW);
      double y = altToY(sat.getAltitude(), plotH, maxChartAlt);

      gc.setFill(color);
      gc.fillOval(x - 5, y - 5, 10, 10);
      gc.setStroke(Color.WHITE);
      gc.setLineWidth(1);
      gc.strokeOval(x - 5, y - 5, 10, 10);

      // Etiqueta con nombre (alineada a la izquierda si está cerca del borde derecho)
      gc.setFill(Color.WHITE);
      gc.setFont(Font.font(10));
      if (x > PAD_LEFT + plotW - 120) {
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(sat.getName().trim(), x - 8, y + 4);
      } else {
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(sat.getName().trim(), x + 8, y + 4);
      }
    }
    gc.setTextAlign(TextAlignment.LEFT);
  }

  public BorderPane getRootLayout() {
    return rootLayout;
  }
}