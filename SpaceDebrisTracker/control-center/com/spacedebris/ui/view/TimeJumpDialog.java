package com.spacedebris.ui.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TimeJumpDialog extends Dialog<TimeJumpDialog.TimeJumpResult> {

  public static class TimeJumpResult {
    private final long offsetSeconds;
    private final String description;

    public TimeJumpResult(long offsetSeconds, String description) {
      this.offsetSeconds = offsetSeconds;
      this.description = description;
    }

    public long getOffsetSeconds() {
      return offsetSeconds;
    }

    public String getDescription() {
      return description;
    }
  }

  private final Spinner<Integer> yearsSpinner = createIntSpinner(0, 50, 0);
  private final Spinner<Integer> monthsSpinner = createIntSpinner(0, 11, 0);
  private final Spinner<Integer> daysSpinner = createIntSpinner(0, 365, 0);
  private final Spinner<Integer> hoursSpinner = createIntSpinner(0, 23, 0);
  private final Spinner<Integer> minutesSpinner = createIntSpinner(0, 59, 0);

  private final DatePicker datePicker = new DatePicker(LocalDate.now().plusMonths(2));
  private final Spinner<Integer> targetHourSpinner = createIntSpinner(0, 23, LocalTime.now().getHour());
  private final Spinner<Integer> targetMinuteSpinner = createIntSpinner(0, 59, LocalTime.now().getMinute());

  public TimeJumpDialog() {
    setTitle("Proyección Orbital en el Tiempo");
    setHeaderText("Selecciona cuánto tiempo avanzar o elige una fecha exacta en el calendario");

    ButtonType applyButtonType = new ButtonType("Aplicar Proyección", ButtonBar.ButtonData.OK_DONE);
    getDialogPane().getButtonTypes().addAll(applyButtonType, ButtonType.CANCEL);

    TabPane modeTabs = new TabPane();
    modeTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

    // Pestaña 1: Salto por duración (ej. 1 año, 3 meses, 22 días, 20 minutos)
    GridPane durationGrid = new GridPane();
    durationGrid.setHgap(12);
    durationGrid.setVgap(10);
    durationGrid.setPadding(new Insets(15));

    durationGrid.add(new Label("Años:"), 0, 0);
    durationGrid.add(yearsSpinner, 1, 0);
    durationGrid.add(new Label("Meses:"), 2, 0);
    durationGrid.add(monthsSpinner, 3, 0);

    durationGrid.add(new Label("Días:"), 0, 1);
    durationGrid.add(daysSpinner, 1, 1);
    durationGrid.add(new Label("Horas:"), 2, 1);
    durationGrid.add(hoursSpinner, 3, 1);

    durationGrid.add(new Label("Minutos:"), 0, 2);
    durationGrid.add(minutesSpinner, 1, 2);

    Label hintDuration = new Label("Ejemplo: 1 año, 3 meses, 22 días y 20 minutos hacia el futuro.");
    hintDuration.setStyle("-fx-text-fill: #5d6d7e; -fx-font-size: 11px;");
    VBox durationBox = new VBox(5, durationGrid, hintDuration);
    durationBox.setPadding(new Insets(0, 10, 10, 10));

    Tab durationTab = new Tab("⏱️ Por Duración (Relativo)", durationBox);

    // Pestaña 2: Fecha y hora exacta en calendario
    GridPane dateGrid = new GridPane();
    dateGrid.setHgap(12);
    dateGrid.setVgap(10);
    dateGrid.setPadding(new Insets(15));

    datePicker.setEditable(false);
    dateGrid.add(new Label("Fecha objetivo:"), 0, 0);
    dateGrid.add(datePicker, 1, 0, 3, 1);

    dateGrid.add(new Label("Hora (0-23):"), 0, 1);
    dateGrid.add(targetHourSpinner, 1, 1);
    dateGrid.add(new Label("Minuto (0-59):"), 2, 1);
    dateGrid.add(targetMinuteSpinner, 3, 1);

    Tab dateTab = new Tab("📅 Por Fecha Exacta (Calendario)", dateGrid);

    modeTabs.getTabs().addAll(durationTab, dateTab);
    getDialogPane().setContent(modeTabs);

    setResultConverter(dialogButton -> {
      if (dialogButton != applyButtonType) {
        return null;
      }

      commitAllSpinners();
      ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());

      if (modeTabs.getSelectionModel().getSelectedItem() == durationTab) {
        int y = yearsSpinner.getValue();
        int m = monthsSpinner.getValue();
        int d = daysSpinner.getValue();
        int h = hoursSpinner.getValue();
        int min = minutesSpinner.getValue();

        ZonedDateTime target = now
            .plusYears(y)
            .plusMonths(m)
            .plusDays(d)
            .plusHours(h)
            .plusMinutes(min);

        long offsetSeconds = Duration.between(now, target).getSeconds();
        if (offsetSeconds == 0) {
          return new TimeJumpResult(0L, "Tiempo Real");
        }
        return new TimeJumpResult(offsetSeconds, buildDurationLabel(y, m, d, h, min));
      } else {
        LocalDate pickedDate = (datePicker.getValue() != null) ? datePicker.getValue() : LocalDate.now();
        LocalTime pickedTime = LocalTime.of(targetHourSpinner.getValue(), targetMinuteSpinner.getValue());
        ZonedDateTime target = ZonedDateTime.of(pickedDate, pickedTime, ZoneId.systemDefault());

        long offsetSeconds = Duration.between(now, target).getSeconds();
        String formattedDate = target.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        return new TimeJumpResult(offsetSeconds, "Fecha: " + formattedDate);
      }
    });
  }

  private void commitAllSpinners() {
    commitSpinner(yearsSpinner);
    commitSpinner(monthsSpinner);
    commitSpinner(daysSpinner);
    commitSpinner(hoursSpinner);
    commitSpinner(minutesSpinner);
    commitSpinner(targetHourSpinner);
    commitSpinner(targetMinuteSpinner);
  }

  private static void commitSpinner(Spinner<Integer> spinner) {
    try {
      String text = spinner.getEditor().getText();
      int val = Integer.parseInt(text.trim());
      spinner.getValueFactory().setValue(val);
    } catch (NumberFormatException ignored) {
      // Mantiene el último valor válido del spinner
    }
  }

  private static String buildDurationLabel(int y, int m, int d, int h, int min) {
    List<String> parts = new ArrayList<>();
    if (y > 0) parts.add(y + (y == 1 ? " año" : " años"));
    if (m > 0) parts.add(m + (m == 1 ? " mes" : " meses"));
    if (d > 0) parts.add(d + (d == 1 ? " día" : " días"));
    if (h > 0) parts.add(h + " h");
    if (min > 0) parts.add(min + " min");
    return "+" + String.join(", ", parts);
  }

  private static Spinner<Integer> createIntSpinner(int min, int max, int initial) {
    Spinner<Integer> spinner = new Spinner<>(min, max, initial);
    spinner.setEditable(true);
    spinner.setPrefWidth(80);
    return spinner;
  }
}