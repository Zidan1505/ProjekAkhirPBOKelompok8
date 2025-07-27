package org.example.hotelsystem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.hotelsystem.model.HousekeepingRecord;
import org.example.hotelsystem.util.DatabaseConnection;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class UpdateHousekeepingController implements Initializable {

    @FXML private Label titleLabel;
    @FXML private TextField recordIdField;
    @FXML private ComboBox<Integer> housekeeperIdComboBox;
    @FXML private ComboBox<Integer> roomNumberComboBox;
    @FXML private DatePicker recordDatePicker;
    @FXML private TextArea notesArea;
    @FXML private Button saveButton;

    private HousekeepingRecord recordToEdit;
    private AdministratorController adminController;

    public void initData(HousekeepingRecord record, AdministratorController controller) {
        this.recordToEdit = record;
        this.adminController = controller;

        if (recordToEdit != null) {
            titleLabel.setText("Edit Record ID: " + record.getRecordId());
            saveButton.setText("Simpan Perubahan");
            populateForm();
        } else {
            titleLabel.setText("Tambah Record Baru");
            saveButton.setText("Tambah Record");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadHousekeepers();
        loadRooms();
        recordDatePicker.setValue(LocalDate.now());
    }

    private void loadHousekeepers() {
        ObservableList<Integer> employeeList = FXCollections.observableArrayList();
        String sql = "SELECT id FROM public.employee WHERE CAST(id AS TEXT) LIKE '3%' ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                employeeList.add(rs.getInt("id"));
            }
            housekeeperIdComboBox.setItems(employeeList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadRooms() {
        ObservableList<Integer> roomList = FXCollections.observableArrayList();
        String sql = "SELECT room_number FROM public.room ORDER BY room_number ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                roomList.add(rs.getInt("room_number"));
            }
            roomNumberComboBox.setItems(roomList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void populateForm() {
        recordIdField.setText(String.valueOf(recordToEdit.getRecordId()));
        housekeeperIdComboBox.setValue(recordToEdit.getHousekeepingId());
        roomNumberComboBox.setValue(recordToEdit.getRoomNumber());
        recordDatePicker.setValue(recordToEdit.getRecordDate());
        notesArea.setText(recordToEdit.getNotes());
    }

    @FXML
    void onSaveButtonAction(ActionEvent event) {
        if (recordToEdit != null) {
            updateRecord();
        } else {
            insertNewRecord();
        }
    }

    private void updateRecord() {
        if (housekeeperIdComboBox.getValue() == null || roomNumberComboBox.getValue() == null || recordDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Input Tidak Lengkap", "Housekeeper, Nomor Kamar, dan Tanggal harus diisi.");
            return;
        }

        String sql = "UPDATE public.housekeeping_record SET housekeeping_id = ?, room_number = ?, record_date = ?, notes = ? " +
                "WHERE record_id = ?";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, housekeeperIdComboBox.getValue());
            ps.setInt(2, roomNumberComboBox.getValue());
            ps.setDate(3, Date.valueOf(recordDatePicker.getValue()));
            ps.setString(4, notesArea.getText());
            ps.setInt(5, recordToEdit.getRecordId());

            ps.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Record berhasil diperbarui.");
            adminController.loadAllRoomsData();
            closeWindow();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Gagal", "Gagal memperbarui record: " + e.getMessage());
        }
    }

    private void insertNewRecord() {
        if (housekeeperIdComboBox.getValue() == null || roomNumberComboBox.getValue() == null || recordDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Input Tidak Lengkap", "Housekeeper, Nomor Kamar, dan Tanggal harus diisi.");
            return;
        }

        String sql = "INSERT INTO public.housekeeping_record (housekeeping_id, room_number, record_date, notes) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, housekeeperIdComboBox.getValue());
            ps.setInt(2, roomNumberComboBox.getValue());
            ps.setDate(3, Date.valueOf(recordDatePicker.getValue()));
            ps.setString(4, notesArea.getText());

            ps.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Record baru berhasil ditambahkan.");
            adminController.loadAllRoomsData();
            closeWindow();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Gagal", "Gagal menambahkan record: " + e.getMessage());
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
