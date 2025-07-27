package org.example.hotelsystem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.hotelsystem.model.Room;
import org.example.hotelsystem.util.DatabaseConnection;

import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ResourceBundle;

public class UpdateRoomController implements Initializable {
    @FXML private Label titleLabel;
    @FXML private TextField idField;
    @FXML private TextField priceField;
    @FXML private ComboBox<String> statusRoomComboBox;
    @FXML private ComboBox<String> checkStatusRoomComboBox;
    @FXML private ComboBox<String> acComboBox;
    @FXML private ComboBox<String> singleBedComboBox;
    @FXML private ComboBox<String> twinBedComboBox;
    @FXML private ComboBox<String> doubleBedComboBox;
    @FXML private Button saveButton;

    private Room roomToEdit;
    private AdministratorController adminController;
    private int adminId;

    public void initData(Room room, AdministratorController adminController, int adminId) {
        this.roomToEdit = room;
        this.adminController = adminController;
        this.adminId = adminId;

        if (roomToEdit != null) {
            titleLabel.setText("Edit Kamar No: " + room.getRoomNumber());
            saveButton.setText("Simpan Perubahan");
            idField.setText(String.valueOf(room.getRoomNumber()));
            idField.setDisable(true);
            populateForm();
        } else {
            titleLabel.setText("Tambah Kamar Baru");
            saveButton.setText("Tambah Kamar");
            idField.setDisable(false);
            idField.setPromptText("Masukkan nomor kamar baru");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        statusRoomComboBox.getItems().addAll("available", "not available", "maintenance");
        checkStatusRoomComboBox.getItems().addAll("Check-In", "Check-Out", "Waiting for Check-In", "Waiting for Check-Out");

        ObservableList<String> booleanOptions = FXCollections.observableArrayList("True", "False");
        acComboBox.setItems(booleanOptions);
        singleBedComboBox.setItems(booleanOptions);
        twinBedComboBox.setItems(booleanOptions);
        doubleBedComboBox.setItems(booleanOptions);
    }

    private void populateForm() {
        priceField.setText(String.valueOf(roomToEdit.getPrice()));
        statusRoomComboBox.setValue(roomToEdit.getStatus());
        checkStatusRoomComboBox.setValue(roomToEdit.getCheckStatus());
        acComboBox.setValue(roomToEdit.isAc() ? "True" : "False");
        singleBedComboBox.setValue(roomToEdit.isSingleBed() ? "True" : "False");
        twinBedComboBox.setValue(roomToEdit.isTwinBed() ? "True" : "False");
        doubleBedComboBox.setValue(roomToEdit.isDoubleBed() ? "True" : "False");
    }

    @FXML
    void onSaveButtonAction(ActionEvent event) {
        if (roomToEdit != null) {
            updateRoom();
        } else {
            insertNewRoom();
        }
    }

    private void updateRoom() {
        try {
            if (priceField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Input Error", "Harga kamar harus diisi.");
                return;
            }

            float updatedPrice = Float.parseFloat(priceField.getText());
            String updatedStatus = statusRoomComboBox.getValue();
            String updatedCheckStatus = checkStatusRoomComboBox.getValue();
            boolean updatedAc = "True".equals(acComboBox.getValue());
            boolean updatedSingle = "True".equals(singleBedComboBox.getValue());
            boolean updatedTwin = "True".equals(twinBedComboBox.getValue());
            boolean updatedDouble = "True".equals(doubleBedComboBox.getValue());

            if (updatedStatus == null || updatedCheckStatus == null ||
                    acComboBox.getValue() == null || singleBedComboBox.getValue() == null ||
                    twinBedComboBox.getValue() == null || doubleBedComboBox.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Input Error", "Semua field harus diisi.");
                return;
            }

            String sql = "UPDATE public.room SET status = ?, price = ?, ac = ?, single_bed = ?, " +
                    "twin_bed = ?, double_bed = ?, check_status = ?, last_change_by_admin_id = ?, " +
                    "last_modified_date = ? WHERE room_number = ?";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, updatedStatus);
                ps.setFloat(2, updatedPrice);
                ps.setBoolean(3, updatedAc);
                ps.setBoolean(4, updatedSingle);
                ps.setBoolean(5, updatedTwin);
                ps.setBoolean(6, updatedDouble);
                ps.setString(7, updatedCheckStatus);
                ps.setInt(8, this.adminId);
                ps.setDate(9, Date.valueOf(LocalDate.now(ZoneId.of("Asia/Jakarta"))));
                ps.setInt(10, roomToEdit.getRoomNumber());

                int rowsAffected = ps.executeUpdate();

                if (rowsAffected > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Sukses",
                            "Kamar No. " + roomToEdit.getRoomNumber() + " berhasil diperbarui.");
                    adminController.loadAllRoomsData();
                    closeWindow();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Gagal",
                            "Tidak ada data yang diperbarui. Kamar mungkin tidak ditemukan.");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Database Error",
                        "Gagal memperbarui kamar: " + e.getMessage());
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Input Error",
                    "Harga harus berupa angka yang valid.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Terjadi kesalahan: " + e.getMessage());
        }
    }

    private void insertNewRoom() {
        try {
            int newRoomNumber = Integer.parseInt(idField.getText());
            float newPrice = Float.parseFloat(priceField.getText());
            String newStatus = statusRoomComboBox.getValue();
            String newCheckStatus = checkStatusRoomComboBox.getValue();
            boolean newAc = "True".equals(acComboBox.getValue());
            boolean newSingle = "True".equals(singleBedComboBox.getValue());
            boolean newTwin = "True".equals(twinBedComboBox.getValue());
            boolean newDouble = "True".equals(doubleBedComboBox.getValue());

            if (newStatus == null || newCheckStatus == null || acComboBox.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Input Error", "Semua status dan fasilitas harus diisi.");
                return;
            }

            if (isRoomNumberExists(newRoomNumber)) {
                showAlert(Alert.AlertType.ERROR, "Gagal", "Nomor kamar " + newRoomNumber + " sudah digunakan.");
                return;
            }

            String sql = "INSERT INTO public.room (room_number, status, price, ac, single_bed, twin_bed, double_bed, check_status, last_change_by_admin_id, last_modified_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, newRoomNumber);
                ps.setString(2, newStatus);
                ps.setFloat(3, newPrice);
                ps.setBoolean(4, newAc);
                ps.setBoolean(5, newSingle);
                ps.setBoolean(6, newTwin);
                ps.setBoolean(7, newDouble);
                ps.setString(8, newCheckStatus);
                ps.setInt(9, this.adminId);
                ps.setDate(10, Date.valueOf(LocalDate.now(ZoneId.of("Asia/Jakarta"))));

                ps.executeUpdate();
                showAlert(Alert.AlertType.INFORMATION, "Sukses",
                        "Kamar baru (" + newRoomNumber + ") berhasil ditambahkan.");
                adminController.loadAllRoomsData();
                closeWindow();

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Gagal",
                        "Gagal menambahkan kamar baru: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Input Error",
                    "Nomor kamar dan harga harus berupa angka.");
        }
    }

    private boolean isRoomNumberExists(int roomNumber) {
        String sql = "SELECT COUNT(*) FROM public.room WHERE room_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void closeWindow() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
