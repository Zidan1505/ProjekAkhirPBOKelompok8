package org.example.hotelsystem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.hotelsystem.model.Reservation;
import org.example.hotelsystem.util.DatabaseConnection;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ResourceBundle;

public class UpdateReservationController implements Initializable {

    @FXML private Label titleLabel;
    @FXML private TextField reservationIdField;
    @FXML private TextField guestNameField;
    @FXML private TextField guestContactField;
    @FXML private ComboBox<Integer> roomNumberComboBox;
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private Button saveButton;

    private Reservation reservationToEdit;
    private AdministratorController adminController;
    private int receptionistId;

    public void initData(Reservation reservation, AdministratorController controller, int receptionistId) {
        this.reservationToEdit = reservation;
        this.adminController = controller;
        this.receptionistId = receptionistId;

        if (reservationToEdit != null) {
            titleLabel.setText("Edit Reservasi ID: " + reservation.getReservationId());
            saveButton.setText("Simpan Perubahan");
            populateForm();
        } else {
            titleLabel.setText("Tambah Reservasi Baru");
            saveButton.setText("Tambah Reservasi");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        statusComboBox.getItems().addAll("CONFIRMED", "CHECKED-IN", "CHECKED-OUT", "CANCELLED");
        loadAvailableRooms();
    }

    private void loadAvailableRooms() {
        ObservableList<Integer> roomList = FXCollections.observableArrayList();
        String sql = "SELECT room_number FROM public.room WHERE status = 'available' ORDER BY room_number ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                roomList.add(rs.getInt("room_number"));
            }
            roomNumberComboBox.setItems(roomList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Gagal memuat data kamar.");
        }
    }

    private void populateForm() {
        reservationIdField.setText(String.valueOf(reservationToEdit.getReservationId()));
        guestNameField.setText(reservationToEdit.getGuestName());
        guestContactField.setText(reservationToEdit.getGuestContact());
        if (!roomNumberComboBox.getItems().contains(reservationToEdit.getRoomNumber())) {
            roomNumberComboBox.getItems().add(reservationToEdit.getRoomNumber());
        }
        roomNumberComboBox.setValue(reservationToEdit.getRoomNumber());
        checkInPicker.setValue(reservationToEdit.getCheckInDate());
        checkOutPicker.setValue(reservationToEdit.getCheckOutDate());
        statusComboBox.setValue(reservationToEdit.getReservationStatus());
    }

    @FXML
    void onSaveButtonAction(ActionEvent event) {
        if (reservationToEdit != null) {
            updateReservation();
        } else {
            insertNewReservation();
        }
    }

    private void updateReservation() {
        if (guestNameField.getText().isEmpty() || roomNumberComboBox.getValue() == null || checkInPicker.getValue() == null || checkOutPicker.getValue() == null || statusComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Input Tidak Lengkap", "Semua field harus diisi.");
            return;
        }

        String sql = "UPDATE public.reservation SET room_number = ?, guest_name = ?, guest_contact = ?, check_in_date = ?, check_out_date = ?, reservation_status = ? WHERE reservation_id = ?";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomNumberComboBox.getValue());
            ps.setString(2, guestNameField.getText());
            ps.setString(3, guestContactField.getText());
            ps.setDate(4, Date.valueOf(checkInPicker.getValue()));
            ps.setDate(5, Date.valueOf(checkOutPicker.getValue()));
            ps.setString(6, statusComboBox.getValue());
            ps.setInt(7, reservationToEdit.getReservationId());

            ps.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data reservasi berhasil diperbarui.");
            adminController.loadAllRoomsData();
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Gagal", "Gagal memperbarui data: " + e.getMessage());
        }
    }

    private void insertNewReservation() {
        if (guestNameField.getText().isEmpty() || roomNumberComboBox.getValue() == null || checkInPicker.getValue() == null || checkOutPicker.getValue() == null || statusComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Input Tidak Lengkap", "Semua field harus diisi.");
            return;
        }

        String sql = "INSERT INTO public.reservation (receptionist_id, room_number, guest_name, guest_contact, reservation_date, check_in_date, check_out_date, reservation_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, this.receptionistId);
            ps.setInt(2, roomNumberComboBox.getValue());
            ps.setString(3, guestNameField.getText());
            ps.setString(4, guestContactField.getText());
            ps.setDate(5, Date.valueOf(LocalDate.now(ZoneId.of("Asia/Jakarta"))));
            ps.setDate(6, Date.valueOf(checkInPicker.getValue()));
            ps.setDate(7, Date.valueOf(checkOutPicker.getValue()));
            ps.setString(8, statusComboBox.getValue());

            ps.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Reservasi baru berhasil ditambahkan.");
            adminController.loadAllRoomsData();
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Gagal", "Gagal menambahkan reservasi: " + e.getMessage());
        }
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
