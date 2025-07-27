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
import org.example.hotelsystem.model.Reservation;
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

public class ReceptionistController implements Initializable {

    private Timeline tableRefreshTimeline;
    private Timeline forcedLogoutTimeline;
    private int loggedInReceptionistId;

    @FXML private TableView<Reservation> reservationTable;
    @FXML private TableColumn<Reservation, Integer> reservationIdColumn;
    @FXML private TableColumn<Reservation, Integer> roomNumberResColumn;
    @FXML private TableColumn<Reservation, String> guestNameColumn;
    @FXML private TableColumn<Reservation, String> guestContactColumn;
    @FXML private TableColumn<Reservation, LocalDate> reservationDateColumn;
    @FXML private TableColumn<Reservation, LocalDate> checkInDateColumn;
    @FXML private TableColumn<Reservation, LocalDate> checkOutDateColumn;
    @FXML private TableView<Room> roomTable;
    @FXML private TableColumn<Room, Integer> roomNumberColumn;
    @FXML private TableColumn<Room, String> statusColumn;
    @FXML private TableColumn<Room, Float> priceColumn;
    @FXML private TableColumn<Room, Boolean> acColumn;
    @FXML private TableColumn<Room, Boolean> singleBedColumn;
    @FXML private TableColumn<Room, Boolean> twinBedColumn;
    @FXML private TableColumn<Room, Boolean> doubleBedColumn;
    @FXML private TableColumn<Room, String> checkStatusColumn;
    @FXML private TableColumn<Room, Integer> adminIdColumn;
    @FXML private Button checkInButton;
    @FXML private Button checkOutButton;

    public void initData(int receptionistId) {
        this.loggedInReceptionistId = receptionistId;
        Main.currentUserId = this.loggedInReceptionistId;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        checkInButton.setDisable(true);
        checkOutButton.setDisable(true);

        reservationTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Reservation item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setDisable(false); setStyle("");
                } else {
                    boolean isMaintenance = "maintenance".equalsIgnoreCase(item.getRoomStatus());
                    setDisable(isMaintenance);
                    setStyle(isMaintenance ? "-fx-background-color: #f2f2f2; -fx-text-fill: #b0b0b0;" : "");
                }
            }
        });

        reservationTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadRoomDetails(newValue.getRoomNumber());
                Room selectedRoomDetails = roomTable.getItems().isEmpty() ? null : roomTable.getItems().get(0);
                if (selectedRoomDetails != null) {
                    String roomCheckStatus = selectedRoomDetails.getCheckStatus();
                    checkInButton.setDisable(!"Waiting for Check-In".equalsIgnoreCase(roomCheckStatus));
                    checkOutButton.setDisable(!"Waiting for Check-Out".equalsIgnoreCase(roomCheckStatus));
                } else {
                    checkInButton.setDisable(true);
                    checkOutButton.setDisable(true);
                }
            } else {
                roomTable.getItems().clear();
                checkInButton.setDisable(true);
                checkOutButton.setDisable(true);
            }
        });

        loadReservationData();

        this.tableRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
            int selectedId = (selected != null) ? selected.getReservationId() : -1;
            loadReservationData();
            if (selectedId != -1) {
                for (Reservation item : reservationTable.getItems()) {
                    if (item.getReservationId() == selectedId) {
                        reservationTable.getSelectionModel().select(item);
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

    private void setupTableColumns() {
        reservationIdColumn.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
        roomNumberResColumn.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        guestNameColumn.setCellValueFactory(new PropertyValueFactory<>("guestName"));
        guestContactColumn.setCellValueFactory(new PropertyValueFactory<>("guestContact"));
        reservationDateColumn.setCellValueFactory(new PropertyValueFactory<>("reservationDate"));
        checkInDateColumn.setCellValueFactory(new PropertyValueFactory<>("checkInDate"));
        checkOutDateColumn.setCellValueFactory(new PropertyValueFactory<>("checkOutDate"));
        roomNumberColumn.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        acColumn.setCellValueFactory(new PropertyValueFactory<>("ac"));
        singleBedColumn.setCellValueFactory(new PropertyValueFactory<>("singleBed"));
        twinBedColumn.setCellValueFactory(new PropertyValueFactory<>("twinBed"));
        doubleBedColumn.setCellValueFactory(new PropertyValueFactory<>("doubleBed"));
        checkStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checkStatus"));
        adminIdColumn.setCellValueFactory(new PropertyValueFactory<>("adminId"));
    }

    private void loadReservationData() {
        ObservableList<Reservation> reservationList = FXCollections.observableArrayList();
        String sql = "SELECT res.*, rm.status as room_status FROM public.reservation res " +
                "LEFT JOIN public.room rm ON res.room_number = rm.room_number " +
                "WHERE res.reservation_status != 'COMPLETED' AND res.reservation_status != 'CANCELLED' " +
                "ORDER BY res.check_in_date ASC";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reservationList.add(new Reservation(
                        rs.getInt("reservation_id"),
                        rs.getInt("receptionist_id"),
                        rs.getInt("room_number"),
                        rs.getString("guest_name"),
                        rs.getString("guest_contact"),
                        rs.getDate("reservation_date").toLocalDate(),
                        rs.getDate("check_in_date").toLocalDate(),
                        rs.getDate("check_out_date").toLocalDate(),
                        rs.getString("room_status"),
                        rs.getString("reservation_status")
                ));
            }
            reservationTable.setItems(reservationList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadRoomDetails(int roomNumber) {
        ObservableList<Room> roomList = FXCollections.observableArrayList();
        String sql = "SELECT * FROM public.room WHERE room_number = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
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
            roomTable.setItems(roomList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onCheckInButtonAction(ActionEvent event) {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data reservasi dari tabel atas.");
            return;
        }
        String sqlRoom = "UPDATE public.room SET status = 'not available', check_status = 'Check-In' WHERE room_number = ?";
        String sqlRes = "UPDATE public.reservation SET receptionist_id = ?, reservation_status = 'CHECKED_IN' WHERE reservation_id = ?";
        runTransaction(sqlRoom, sqlRes, selected, "Check-In");
    }

    @FXML
    void onCheckOutButtonAction(ActionEvent event) {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data reservasi dari tabel atas.");
            return;
        }
        String sqlRoom = "UPDATE public.room SET status = 'maintenance', check_status = 'Check-Out' WHERE room_number = ?";
        String sqlRes = "UPDATE public.reservation SET receptionist_id = ?, reservation_status = 'COMPLETED' WHERE reservation_id = ?";
        runTransaction(sqlRoom, sqlRes, selected, "Check-Out");
    }

    private void runTransaction(String sqlRoom, String sqlReservation, Reservation reservation, String actionType) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psRoom = conn.prepareStatement(sqlRoom); PreparedStatement psRes = conn.prepareStatement(sqlReservation)) {
                psRoom.setInt(1, reservation.getRoomNumber());
                psRoom.executeUpdate();

                psRes.setInt(1, this.loggedInReceptionistId);
                psRes.setInt(2, reservation.getReservationId());
                psRes.executeUpdate();

                conn.commit();
                showAlert(Alert.AlertType.INFORMATION, "Sukses", actionType + " untuk tamu '" + reservation.getGuestName() + "' berhasil.");
            } catch (SQLException e) {
                conn.rollback();
                showAlert(Alert.AlertType.ERROR, "Gagal", actionType + " gagal. Perubahan dibatalkan.");
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            loadReservationData();
            roomTable.getItems().clear();
        }
    }

    private void checkMyStatus() {
        if (this.loggedInReceptionistId <= 0) return;
        String sql = "SELECT status_employee FROM public.employee WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, this.loggedInReceptionistId);
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
            Stage stage = (Stage) checkInButton.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Login.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            stage.setScene(scene);
            stage.setTitle("Login Staf");
            stage.show();
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
