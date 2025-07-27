package org.example.hotelsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.example.hotelsystem.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Main extends Application {

    public static int currentUserId = -1;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("General.fxml"));
        AnchorPane root = fxmlLoader.load();
        Scene scene = new Scene(root);
        stage.setTitle("Hotel System");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (currentUserId != -1) {
            System.out.println("Aplikasi ditutup, mengubah status employee " + currentUserId + " menjadi Logged-Out.");
            String sql = "UPDATE public.employee SET status_employee = 'Logged-Out' WHERE id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, currentUserId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
