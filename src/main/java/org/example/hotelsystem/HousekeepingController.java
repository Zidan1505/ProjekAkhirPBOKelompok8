package org.example.hotelsystem;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.hotelsystem.model.Room;
import org.example.hotelsystem.util.DatabaseConnection;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class HousekeepingController implements Initializable {

    private Timeline tableRefreshTimeline;
    private Timeline forcedLogoutTimeline;
    private int loggedInHousekeeperId;

    @FXML private TableView<Room> maintenanceRoomTable;
    @FXML private TableColumn<Room, Integer> roomNumberColumn;
    @FXML private TableColumn<Room, String> statusColumn;
    @FXML private TableColumn<Room, String> checkStatusColumn;
    @FXML private ComboBox<String> changeStatusComboBox;
    @FXML private TextArea catatanTextArea;
    @FXML private Button cancelButton;
    @FXML private Button sendButton;

    public void initData(int housekeeperId) {
        this.loggedInHousekeeperId = housekeeperId;
        Main.currentUserId = this.loggedInHousekeeperId;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        roomNumberColumn.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        checkStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checkStatus"));
        changeStatusComboBox.getItems().addAll("available", "not available");

        loadMaintenanceRooms();

        this.tableRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            Room selected = maintenanceRoomTable.getSelectionModel().getSelectedItem();
            int selectedId = (selected != null) ? selected.getRoomNumber() : -1;
            loadMaintenanceRooms();
            if (selectedId != -1) {
                for (Room item : maintenanceRoomTable.getItems()) {
                    if (item.getRoomNumber() == selectedId) {
                        maintenanceRoomTable.getSelectionModel().select(item);
                        break;
                    }
                }
            }
        }));
        this.tableRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        this.tableRefreshTimeline.play();

        this.forcedLogoutTimeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> checkMyStatus()));
        this.forcedLogoutTimeline.setCycleCount(Timeline.INDEFINITE);
        this.forcedLogoutTimeline.play();
    }

    private void loadMaintenanceRooms() {
        ObservableList<Room> roomList = FXCollections.observableArrayList();
        String sql = "SELECT * FROM public.room WHERE status = 'maintenance'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                java.sql.Date lastModified = rs.getDate("last_modified_date");
                roomList.add(new Room(
                        rs.getInt("room_number"), rs.getString("status"),
                        rs.getFloat("price"), rs.getBoolean("ac"),
                        rs.getBoolean("single_bed"), rs.getBoolean("twin_bed"),
                        rs.getBoolean("double_bed"), rs.getString("check_status"),
                        rs.getInt("last_change_by_admin_id"),
                        (lastModified != null) ? lastModified.toLocalDate() : null
                ));
            }
            maintenanceRoomTable.setItems(roomList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onSendButtonAction(ActionEvent event) {
        Room selectedRoom = maintenanceRoomTable.getSelectionModel().getSelectedItem();
        String newStatus = changeStatusComboBox.getSelectionModel().getSelectedItem();
        String notes = catatanTextArea.getText();
        if (selectedRoom == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih kamar dari tabel terlebih dahulu.");
            return;
        }
        if (newStatus == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih status baru untuk kamar.");
            return;
        }
        String sqlUpdateRoom = "UPDATE public.room SET status = ? WHERE room_number = ?";
        String sqlInsertRecord = "INSERT INTO public.housekeeping_record (housekeeping_id, room_number, record_date, notes) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdateRoom);
                 PreparedStatement psInsert = conn.prepareStatement(sqlInsertRecord)) {

                psUpdate.setString(1, newStatus);
                psUpdate.setInt(2, selectedRoom.getRoomNumber());
                psUpdate.executeUpdate();

                String finalNotes = "Status diubah menjadi '" + newStatus + "'. Catatan: " + (notes.trim().isEmpty() ? "Tidak ada." : notes);
                psInsert.setInt(1, this.loggedInHousekeeperId);
                psInsert.setInt(2, selectedRoom.getRoomNumber());
                psInsert.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
                psInsert.setString(4, finalNotes);
                psInsert.executeUpdate();

                conn.commit();
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data untuk kamar " + selectedRoom.getRoomNumber() + " telah diperbarui.");
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Gagal", "Gagal memperbarui status kamar.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        loadMaintenanceRooms();
        catatanTextArea.clear();
        changeStatusComboBox.getSelectionModel().clearSelection();
        maintenanceRoomTable.getSelectionModel().clearSelection();
    }

    @FXML
    void onCancelButtonAction(ActionEvent event) {
        maintenanceRoomTable.getSelectionModel().clearSelection();
        changeStatusComboBox.getSelectionModel().clearSelection();
        catatanTextArea.clear();
    }

    private void checkMyStatus() {
        if (this.loggedInHousekeeperId <= 0) return;
        String sql = "SELECT status_employee FROM public.employee WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, this.loggedInHousekeeperId);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && "Logged-Out".equals(rs.getString("status_employee"))) {
                Platform.runLater(this::forceLogout);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void forceLogout() {
        if (tableRefreshTimeline != null) tableRefreshTimeline.stop();
        if (forcedLogoutTimeline != null) forcedLogoutTimeline.stop();
        try {
            Main.currentUserId = -1;
            Stage stage = (Stage) sendButton.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Login.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            stage.setScene(scene);
            stage.setTitle("Login Staf");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
