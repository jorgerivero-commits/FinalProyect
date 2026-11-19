package com.spacedebris.service;

import com.spacedebris.model.Satellite;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseService {

  private static final String HOST = getEnvOrDefault("DB_HOST", "localhost");
  private static final String PORT = getEnvOrDefault("DB_PORT", "3306");
  private static final String DB_NAME = getEnvOrDefault("DB_NAME", "space_debris_db");
  private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME
      + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
  private static final String USER = System.getenv("DB_USER");
  private static final String PASSWORD = System.getenv("DB_PASS");

  private static String getEnvOrDefault(String key, String defaultValue) {
    String val = System.getenv(key);
    return (val != null && !val.isBlank()) ? val : defaultValue;
  }

  public Connection connect() throws SQLException {
    if (USER == null || USER.isBlank() || PASSWORD == null || PASSWORD.isBlank()) {
      throw new SQLException("Faltan las variables de entorno DB_USER o DB_PASS. Revisa tu archivo .env.");
    }
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      throw new SQLException("Driver de MySQL no encontrado en el classpath: " + e.getMessage());
    }
    return DriverManager.getConnection(URL, USER, PASSWORD);
  }

  public List<Satellite> getAllSatellites() {
    List<Satellite> list = new ArrayList<>();
    String query = "SELECT d.norad_cat_id, d.object_name, " +
            "IFNULL(p.latitude, 0.0) as lat, " +
            "IFNULL(p.longitude, 0.0) as lon, " +
            "IFNULL(p.altitude, 400.0) as alt " +
            "FROM space_debris d " +
            "LEFT JOIN orbital_positions p ON d.norad_cat_id = p.norad_cat_id " +
            "AND p.id = (" +
            "    SELECT MAX(p2.id) " +
            "    FROM orbital_positions p2 " +
            "    WHERE p2.norad_cat_id = d.norad_cat_id" +
            ")";
    try (Connection conn = connect();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query)) {

      while (rs.next()) {
        list.add(new Satellite(
            rs.getInt("norad_cat_id"),
            rs.getString("object_name"),
            rs.getDouble("lat"),
            rs.getDouble("lon"),
            rs.getDouble("alt")));
      }
      System.out.println("✅ [DatabaseService] Satélites consultados con éxito: " + list.size());
    } catch (SQLException e) {
      System.err.println("❌ [DatabaseService] Error al consultar satélites desde MySQL: " + e.getMessage());
    }
    return list;
  }

  public List<Satellite> getRawDebrisFromDatabase() {
    List<Satellite> list = new ArrayList<>();
    String query = "SELECT norad_cat_id, object_name, tle_line1, tle_line2 FROM space_debris";

    try (Connection conn = connect();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query)) {

      while (rs.next()) {
        Satellite sat = new Satellite();
        sat.setId(rs.getInt("norad_cat_id"));
        sat.setName(rs.getString("object_name"));
        sat.setTleLine1(rs.getString("tle_line1"));
        sat.setTleLine2(rs.getString("tle_line2"));
        list.add(sat);
      }
    } catch (SQLException e) {
      System.err.println("❌ [DatabaseService] Error al consultar TLEs crudos desde MySQL: " + e.getMessage());
    }
    return list;
  }

  public void saveAllOrbitalPositions(List<Satellite> satellites) {
    if (satellites == null || satellites.isEmpty()) {
      return;
    }

    String query = "INSERT INTO orbital_positions (norad_cat_id, latitude, longitude, altitude) VALUES (?, ?, ?, ?)";

    try (Connection conn = connect()) {
      conn.setAutoCommit(false);
      try (PreparedStatement pstmt = conn.prepareStatement(query)) {
        for (Satellite sat : satellites) {
          pstmt.setInt(1, sat.getId());
          pstmt.setDouble(2, sat.getLatitude());
          pstmt.setDouble(3, sat.getLongitude());
          pstmt.setDouble(4, sat.getAltitude());
          pstmt.addBatch();
        }
        pstmt.executeBatch();
        conn.commit();
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      }
    } catch (SQLException e) {
      System.err.println("❌ [DatabaseService] Error al guardar lote de posiciones orbitales: " + e.getMessage());
    }
  }

  public Map<Integer, List<double[]>> getRecentTrails(int pointsPerSat) {
    Map<Integer, List<double[]>> trails = new HashMap<>();
    String query = "SELECT norad_cat_id, latitude, longitude FROM ( " +
        "  SELECT norad_cat_id, latitude, longitude, id, " +
        "         ROW_NUMBER() OVER (PARTITION BY norad_cat_id ORDER BY id DESC) AS rn " +
        "  FROM orbital_positions " +
        "  WHERE id > (SELECT IFNULL(MAX(id), 0) FROM orbital_positions) " +
        "            - (? * (SELECT COUNT(*) FROM space_debris)) " +
        ") t WHERE rn <= ? ORDER BY norad_cat_id, id ASC";

    try (Connection conn = connect();
        PreparedStatement pstmt = conn.prepareStatement(query)) {

      pstmt.setInt(1, pointsPerSat);
      pstmt.setInt(2, pointsPerSat);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          trails.computeIfAbsent(rs.getInt("norad_cat_id"), k -> new ArrayList<>())
              .add(new double[] { rs.getDouble("latitude"), rs.getDouble("longitude") });
        }
      }
    } catch (SQLException e) {
      System.err.println("❌ [DatabaseService] Error al consultar los rastros orbitales: " + e.getMessage());
    }
    return trails;
  }
}