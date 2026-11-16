package com.spacedebris.ui.view;

import com.spacedebris.model.Satellite;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class SatelliteTableView extends TableView<Satellite> {

  private final ObservableList<Satellite> satelliteData = FXCollections.observableArrayList();

  @SuppressWarnings("deprecation")
  public SatelliteTableView() {
    setItems(satelliteData);
    setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

    getColumns().addAll(List.of(
        createColumn("ID", "id", 80),
        createColumn("Objeto / Satélite", "name", 220),
        createColumn("Latitud (°)", "latitude", 130),
        createColumn("Longitud (°)", "longitude", 130),
        createColumn("Altitud (km)", "altitude", 140),
        createColumn("Velocidad (km/s)", "velocity", 140)));
  }

  public void updateData(List<Satellite> satellites) {
    satelliteData.setAll(satellites);
  }

  private static <T> TableColumn<Satellite, T> createColumn(String title, String property, double width) {
    TableColumn<Satellite, T> col = new TableColumn<>(title);
    col.setCellValueFactory(new PropertyValueFactory<>(property));
    col.setPrefWidth(width);
    return col;
  }
}