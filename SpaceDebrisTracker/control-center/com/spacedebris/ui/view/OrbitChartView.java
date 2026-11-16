package com.spacedebris.ui.view;

import com.spacedebris.model.Satellite;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;

public class OrbitChartView extends Pane {

  private static final double PAD_LEFT = 65;
  private static final double PAD_RIGHT = 30;
  private static final double PAD_TOP = 30;
  private static final double PAD_BOTTOM = 35;

  private static final double ALT_HEADROOM_KM = 100.0;
  private static final double DEFAULT_MAX_ALT_KM = 400.0;

  private static final Color[] PALETTE = {
      Color.web("#e74c3c"), Color.web("#f1c40f"), Color.web("#2ecc71"),
      Color.web("#00d2ff"), Color.web("#e056fd"), Color.web("#ff9f43")
  };

  private final Canvas canvas = new Canvas();
  private List<Satellite> currentSatellites = List.of();

  public OrbitChartView() {
    setStyle("-fx-background-color: #0b1d3a;");
    getChildren().add(canvas);

    // El Canvas siempre ocupa el 100% del espacio disponible en la pestaña
    canvas.widthProperty().bind(widthProperty());
    canvas.heightProperty().bind(heightProperty());

    widthProperty().addListener((obs, oldVal, newVal) -> redraw());
    heightProperty().addListener((obs, oldVal, newVal) -> redraw());
  }

  public void updateChart(List<Satellite> satellites) {
    this.currentSatellites = (satellites != null) ? satellites : List.of();
    redraw();
  }

  private void redraw() {
    double w = canvas.getWidth();
    double h = canvas.getHeight();
    if (w <= 0 || h <= 0) {
      return;
    }

    double plotW = w - PAD_LEFT - PAD_RIGHT;
    double plotH = h - PAD_TOP - PAD_BOTTOM;
    if (plotW <= 0 || plotH <= 0) {
      return;
    }

    GraphicsContext gc = canvas.getGraphicsContext2D();
    double maxChartAlt = computeMaxAltitude(currentSatellites);

    drawBackground(gc, w, h);
    drawAltitudeAxis(gc, plotW, plotH, maxChartAlt);
    drawLongitudeAxis(gc, plotW, plotH);

    for (Satellite sat : currentSatellites) {
      drawSatellite(gc, sat, plotW, plotH, maxChartAlt);
    }
    gc.setTextAlign(TextAlignment.LEFT);
  }

  private double computeMaxAltitude(List<Satellite> satellites) {
    double highestAlt = satellites.stream()
        .mapToDouble(Satellite::getAltitude)
        .max()
        .orElse(0.0);
    return (highestAlt > 0.0 ? highestAlt : DEFAULT_MAX_ALT_KM) + ALT_HEADROOM_KM;
  }

  private void drawBackground(GraphicsContext gc, double w, double h) {
    gc.setFill(Color.web("#0b1d3a"));
    gc.fillRect(0, 0, w, h);
    gc.setFont(Font.font(11));
  }

  private void drawAltitudeAxis(GraphicsContext gc, double plotW, double plotH, double maxChartAlt) {
    double stepKm = computeNiceStep(maxChartAlt);
    gc.setTextAlign(TextAlignment.RIGHT);

    for (double alt = 0.0; alt <= maxChartAlt; alt += stepKm) {
      double y = altToY(alt, plotH, maxChartAlt);
      gc.setLineWidth(alt == 0.0 ? 1.5 : 1.0);
      gc.setStroke(alt == 0.0 ? Color.web("#2ecc71") : Color.web("#1e3a60"));
      gc.strokeLine(PAD_LEFT, y, PAD_LEFT + plotW, y);

      gc.setFill(alt == 0.0 ? Color.web("#2ecc71") : Color.web("#6c8ebf"));
      gc.fillText(Math.round(alt) + " km", PAD_LEFT - 8, y + 4);
    }

    double topY = altToY(maxChartAlt, plotH, maxChartAlt);
    gc.setLineWidth(1.0);
    gc.setStroke(Color.web("#3d5a80"));
    gc.strokeLine(PAD_LEFT, topY, PAD_LEFT + plotW, topY);
    gc.setFill(Color.web("#9bb8e0"));
    gc.fillText(String.format("%.0f km", maxChartAlt), PAD_LEFT - 8, topY + 4);
  }

  private void drawLongitudeAxis(GraphicsContext gc, double plotW, double plotH) {
    gc.setTextAlign(TextAlignment.CENTER);
    gc.setLineWidth(1.0);

    for (int lon = -180; lon <= 180; lon += 30) {
      double x = lonToX(lon, plotW);
      gc.setStroke(lon == 0 ? Color.web("#3d5a80") : Color.web("#1e3a60"));
      gc.strokeLine(x, PAD_TOP, x, PAD_TOP + plotH);

      gc.setFill(Color.web("#6c8ebf"));
      gc.fillText(lon + "°", x, PAD_TOP + plotH + 18);
    }
  }

  private void drawSatellite(GraphicsContext gc, Satellite sat, double plotW, double plotH, double maxChartAlt) {
    Color color = PALETTE[Math.floorMod(sat.getId(), PALETTE.length)];
    double x = lonToX(sat.getLongitude(), plotW);
    double y = altToY(sat.getAltitude(), plotH, maxChartAlt);

    gc.setFill(color);
    gc.fillOval(x - 5, y - 5, 10, 10);
    gc.setStroke(Color.WHITE);
    gc.setLineWidth(1);
    gc.strokeOval(x - 5, y - 5, 10, 10);

    gc.setFill(Color.WHITE);
    gc.setFont(Font.font(11));
    if (x > PAD_LEFT + plotW - 130) {
      gc.setTextAlign(TextAlignment.RIGHT);
      gc.fillText(sat.getName().trim(), x - 8, y + 4);
    } else {
      gc.setTextAlign(TextAlignment.LEFT);
      gc.fillText(sat.getName().trim(), x + 8, y + 4);
    }
  }

  private double lonToX(double lon, double plotW) {
    return PAD_LEFT + (lon + 180.0) / 360.0 * plotW;
  }

  private double altToY(double alt, double plotH, double maxChartAlt) {
    double clampedAlt = Math.max(0.0, alt);
    return PAD_TOP + plotH - (clampedAlt / maxChartAlt) * plotH;
  }

  private static double computeNiceStep(double maxVal) {
    double rawStep = maxVal / 5.0;
    double magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
    double residual = rawStep / magnitude;
    if (residual <= 1.0) return 1.0 * magnitude;
    if (residual <= 2.0) return 2.0 * magnitude;
    if (residual <= 5.0) return 5.0 * magnitude;
    return 10.0 * magnitude;
  }
}