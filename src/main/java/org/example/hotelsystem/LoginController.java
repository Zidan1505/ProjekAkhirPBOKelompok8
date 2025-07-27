package org.example.hotelsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.hotelsystem.util.DatabaseConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private Button loginButton;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField usernameTextField;

    @FXML
    void onLoginButtonAction(ActionEvent event) {
        String username = usernameTextField.getText();
        String plainPassword = passwordField.getText();

        if (username.isEmpty() || plainPassword.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Form Error!", "Silakan masukkan username dan password");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT id FROM public.employee WHERE username = ? AND user_password = md5(?)";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, plainPassword);

            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                Main.currentUserId = id;

                String role = "";
                int roleDigit = id / 100;

                if (roleDigit == 1) role = "Administrator";
                else if (roleDigit == 2) role = "Resepsionis";
                else if (roleDigit == 3) role = "Petugas Kebersihan";
                else {
                    showAlert(Alert.AlertType.ERROR, "Role Error", "Format ID pengguna tidak valid.");
                    return;
                }

                String updateSql = "UPDATE public.employee SET status_employee = 'Logged-In' WHERE id = ?";
                try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                    updatePs.setInt(1, id);
                    updatePs.executeUpdate();
                }

                System.out.println("Login berhasil! Peran: " + role);
                navigateToDashboard(role, event, id);

            } else {
                showAlert(Alert.AlertType.ERROR, "Login Gagal", "Username atau Password salah.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Terjadi kesalahan saat menghubungi database.");
        }
    }

    private void navigateToDashboard(String role, ActionEvent event, int employeeId) {
        String fxmlFile = "";
        switch (role) {
            case "Administrator":
                fxmlFile = "Administrator.fxml";
                break;
            case "Resepsionis":
                fxmlFile = "Receptionist.fxml";
                break;
            case "Petugas Kebersihan":
                fxmlFile = "Housekeeping.fxml";
                break;
            default:
                showAlert(Alert.AlertType.ERROR, "Navigate Error", "Peran tidak dikenal.");
                return;
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFile));
            Scene scene = new Scene(fxmlLoader.load());

            if (role.equals("Administrator")) {
                AdministratorController controller = fxmlLoader.getController();
                controller.initData(employeeId);
            } else if (role.equals("Resepsionis")) {
                ReceptionistController controller = fxmlLoader.getController();
                controller.initData(employeeId);
            } else if (role.equals("Petugas Kebersihan")) {
                HousekeepingController controller = fxmlLoader.getController();
                controller.initData(employeeId);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Dashboard " + role);
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Gagal memuat halaman dashboard: " + fxmlFile);
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private Button backButton;

    @FXML
    void onBackButtonAction(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("General.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(fxmlLoader.load());
            stage.setTitle("Hotel System");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
