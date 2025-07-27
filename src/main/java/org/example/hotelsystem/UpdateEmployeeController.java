package org.example.hotelsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.hotelsystem.model.Employee;
import org.example.hotelsystem.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UpdateEmployeeController {

    @FXML private Label titleLabel;
    @FXML private TextField idField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField fullnameField;
    @FXML private TextField emailField;
    @FXML private TextField contactField;
    @FXML private Button saveButton;

    private Employee employeeToEdit;
    private AdministratorController adminController;

    public void initData(Employee employee, AdministratorController adminController) {
        this.employeeToEdit = employee;
        this.adminController = adminController;

        if (employeeToEdit != null) {
            titleLabel.setText("Edit Karyawan: " + employee.getFullname());
            saveButton.setText("Simpan Perubahan");
            idField.setText(String.valueOf(employeeToEdit.getId()));
            idField.setDisable(true);
            populateForm();
        } else {
            titleLabel.setText("Tambah Karyawan Baru");
            saveButton.setText("Tambah Karyawan");
            idField.setDisable(false);
        }
    }

    private void populateForm() {
        usernameField.setText(employeeToEdit.getUsername());
        fullnameField.setText(employeeToEdit.getFullname());
        emailField.setText(employeeToEdit.getEmail());
        contactField.setText(employeeToEdit.getContact());
    }

    @FXML
    void onSaveButtonAction(ActionEvent event) {
        if (idField.getText().isEmpty() || usernameField.getText().isEmpty() || fullnameField.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Tidak Valid", "ID, Username, dan Nama Lengkap tidak boleh kosong.");
            return;
        }
        if (employeeToEdit == null && passwordField.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Tidak Valid", "Password harus diisi untuk karyawan baru.");
            return;
        }

        if (employeeToEdit != null) {
            updateEmployee();
        } else {
            insertNewEmployee();
        }
    }

    private void updateEmployee() {
        boolean updatePassword = !passwordField.getText().isEmpty();
        String sql = updatePassword ?
                "UPDATE public.employee SET username = ?, user_password = ?, fullname = ?, email = ?, contact = ? WHERE id = ?" :
                "UPDATE public.employee SET username = ?, fullname = ?, email = ?, contact = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usernameField.getText());
            int parameterIndex = 2;
            if (updatePassword) {
                ps.setString(parameterIndex++, passwordField.getText());
            }
            ps.setString(parameterIndex++, fullnameField.getText());
            ps.setString(parameterIndex++, emailField.getText());
            ps.setString(parameterIndex++, contactField.getText());
            ps.setInt(parameterIndex, employeeToEdit.getId());

            ps.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data karyawan berhasil diperbarui.");
            adminController.loadAllRoomsData();
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Gagal", "Gagal memperbarui data: " + e.getMessage());
        }
    }

    private void insertNewEmployee() {
        int newId;
        try {
            newId = Integer.parseInt(idField.getText());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Input Tidak Valid", "ID harus berupa angka.");
            return;
        }

        if (isIdExists(newId)) {
            showAlert(Alert.AlertType.ERROR, "Gagal", "ID " + newId + " sudah digunakan. Harap gunakan ID lain.");
            return;
        }

        String sql = "INSERT INTO public.employee (id, username, user_password, fullname, email, contact, status_employee) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newId);
            ps.setString(2, usernameField.getText());
            ps.setString(3, passwordField.getText());
            ps.setString(4, fullnameField.getText());
            ps.setString(5, emailField.getText());
            ps.setString(6, contactField.getText());
            ps.setString(7, "Logged-Out");

            ps.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Karyawan baru dengan ID " + newId + " berhasil ditambahkan.");
            adminController.loadAllRoomsData();
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Gagal", "Gagal menambahkan karyawan: " + e.getMessage());
        }
    }

    private boolean isIdExists(int id) {
        String sql = "SELECT COUNT(*) FROM public.employee WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
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
