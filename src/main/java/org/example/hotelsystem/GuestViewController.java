package org.example.hotelsystem;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.hotelsystem.model.Reservation;
import org.example.hotelsystem.model.Room;
import org.example.hotelsystem.util.DatabaseConnection;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class GuestViewController implements Initializable {

    @FXML private TilePane roomTilePane;
    @FXML private Button bookButton;
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private Button backButton;

    @FXML private TextField searchField;
    @FXML private Button findButton;
    @FXML private VBox searchResultContainer;
    @FXML private VBox verificationPane;
    @FXML private TextField verifyNameField;
    @FXML private TextField verifyContactField;
    @FXML private DatePicker verifyDateField;
    @FXML private Button verifyButton;
    @FXML private HBox actionPane;
    @FXML private Button guestCheckInButton;
    @FXML private Button guestCheckOutButton;

    private Timeline roomPollingTimeline;
    private Room selectedRoom = null;
    private Node selectedTile = null;
    private List<Reservation> foundReservations = new ArrayList<>();
    private Reservation selectedReservation = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        bookButton.setDisable(true);
        checkInPicker.valueProperty().addListener((obs, oldVal, newVal) -> attemptRoomSearch());
        checkOutPicker.valueProperty().addListener((obs, oldVal, newVal) -> attemptRoomSearch());

        searchResultContainer.setVisible(false);
        verificationPane.setVisible(false);
        actionPane.setVisible(false);

        this.roomPollingTimeline = new Timeline(new KeyFrame(Duration.seconds(7), event -> attemptRoomSearch()));
        this.roomPollingTimeline.setCycleCount(Timeline.INDEFINITE);
        this.roomPollingTimeline.play();
    }

    private void attemptRoomSearch() {
        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();
        if (checkIn != null && checkOut != null) {
            if (!checkOut.isAfter(checkIn)) {
                roomTilePane.getChildren().clear();
                return;
            }
            loadAvailableRooms(checkIn, checkOut);
        }
    }

    public void loadAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        int selectedRoomNumber = (selectedRoom != null) ? selectedRoom.getRoomNumber() : -1;
        roomTilePane.getChildren().clear();
        ObservableList<Room> roomList = FXCollections.observableArrayList();

        String sql = "SELECT * FROM public.room WHERE status = 'available' AND room_number NOT IN (" +
                "  SELECT room_number FROM public.reservation WHERE room_number IS NOT NULL AND " +
                "  reservation_status IN ('CONFIRMED', 'CHECKED_IN') AND " +
                "  (check_in_date < ? AND check_out_date > ?)" +
                ")";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(checkOut));
            ps.setDate(2, Date.valueOf(checkIn));
            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                Date lastModified = rs.getDate("last_modified_date");
                roomList.add(new Room(
                        rs.getInt("room_number"), rs.getString("status"), rs.getFloat("price"),
                        rs.getBoolean("ac"), rs.getBoolean("single_bed"), rs.getBoolean("twin_bed"),
                        rs.getBoolean("double_bed"), rs.getString("check_status"),
                        rs.getInt("last_change_by_admin_id"),
                        (lastModified != null) ? lastModified.toLocalDate() : null
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (Room room : roomList) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("RoomTile.fxml"));
                AnchorPane tile = loader.load();
                RoomTileController controller = loader.getController();
                controller.setData(room, this);
                roomTilePane.getChildren().add(tile);

                if(room.getRoomNumber() == selectedRoomNumber) {
                    selectRoom(room, tile);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void selectRoom(Room room, Node tileNode) {
        if (this.selectedTile != null) {
            this.selectedTile.setStyle("-fx-background-color: #f4f4f4; -fx-background-radius: 10;");
        }
        this.selectedRoom = room;
        this.selectedTile = tileNode;
        this.selectedTile.setStyle("-fx-background-color: #a7c7e7; -fx-background-radius: 10;");
        bookButton.setDisable(false);
    }

    @FXML
    void onBookButtonAction(ActionEvent event) {
        if (selectedRoom == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih kamar terlebih dahulu.");
            return;
        }

        try {
            if (roomPollingTimeline != null) roomPollingTimeline.stop();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("Booking.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Form Pemesanan Kamar " + selectedRoom.getRoomNumber());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(loader.load()));

            BookingController controller = loader.getController();
            controller.initData(selectedRoom, this, checkInPicker.getValue(), checkOutPicker.getValue());
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (roomPollingTimeline != null) roomPollingTimeline.play();
        }
    }

    @FXML
    void onFindReservationButtonAction(ActionEvent event) {
        String searchText = searchField.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Kosong",
                    "Masukkan ID Reservasi, Nama, atau Kontak Anda.");
            return;
        }

        foundReservations.clear();
        searchResultContainer.getChildren().clear();
        selectedReservation = null;

        String sql = "SELECT res.*, r.status AS room_status FROM public.reservation res " +
                "LEFT JOIN public.room r ON res.room_number = r.room_number " +
                "WHERE CAST(res.reservation_id AS TEXT) = ? OR " +
                "lower(res.guest_name) = lower(?) OR " +
                "res.guest_contact = ? " +
                "ORDER BY res.reservation_date DESC, res.check_in_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, searchText);
            ps.setString(2, searchText.toLowerCase());
            ps.setString(3, searchText);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Reservation reservation = new Reservation(
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
                );
                foundReservations.add(reservation);
            }

            if (foundReservations.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Tidak Ditemukan",
                        "Reservasi tidak ditemukan.");
                searchResultContainer.setVisible(false);
                verificationPane.setVisible(false);
                actionPane.setVisible(false);
            } else {
                displayAllFoundReservations();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Terjadi kesalahan saat mencari reservasi: " + e.getMessage());
        }
    }

    private void displayAllFoundReservations() {
        searchResultContainer.getChildren().clear();

        Label headerLabel = new Label("Hasil Pencarian:");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        searchResultContainer.getChildren().add(headerLabel);

        if (foundReservations.size() > 1) {
            Label countLabel = new Label("Ditemukan " + foundReservations.size() +
                    " reservasi. Klik salah satu untuk memilih:");
            countLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d; -fx-padding: 0 0 10 0;");
            searchResultContainer.getChildren().add(countLabel);
        }

        for (int i = 0; i < foundReservations.size(); i++) {
            Reservation reservation = foundReservations.get(i);
            VBox reservationCard = createReservationCard(reservation, i + 1);
            searchResultContainer.getChildren().add(reservationCard);
        }

        searchResultContainer.setVisible(true);
        searchResultContainer.setManaged(true);
        verificationPane.setVisible(false);
        actionPane.setVisible(false);
    }

    private VBox createReservationCard(Reservation reservation, int index) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #ffffff; -fx-padding: 15; -fx-border-color: #dee2e6; " +
                "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        Label titleLabel = new Label("Reservasi #" + reservation.getReservationId());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #495057;");

        Label roomLabel = new Label("Kamar: " + reservation.getRoomNumber());
        roomLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6c757d;");

        Label dateLabel = new Label("Check-in: " + reservation.getCheckInDate() +
                " | Check-out: " + reservation.getCheckOutDate());
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6c757d;");

        Label statusLabel = new Label("Status: " + reservation.getReservationStatus());
        String statusColor = getStatusColor(reservation.getReservationStatus());
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + statusColor + ";");

        card.getChildren().addAll(titleLabel, roomLabel, dateLabel, statusLabel);

        card.setOnMouseClicked(event -> selectReservation(reservation, card));

        card.setOnMouseEntered(event -> {
            if (!card.getStyle().contains("#e3f2fd")) {
                card.setStyle(card.getStyle().replace("#ffffff", "#f8f9fa"));
            }
        });
        card.setOnMouseExited(event -> {
            if (!card.getStyle().contains("#e3f2fd")) {
                card.setStyle(card.getStyle().replace("#f8f9fa", "#ffffff"));
            }
        });

        return card;
    }

    private String getStatusColor(String status) {
        switch (status.toUpperCase()) {
            case "CONFIRMED": return "#28a745";
            case "CHECKED_IN": return "#007bff";
            case "CHECKED_OUT": return "#6c757d";
            case "CANCELLED": return "#dc3545";
            default: return "#ffc107";
        }
    }

    private void selectReservation(Reservation reservation, VBox card) {
        searchResultContainer.getChildren().forEach(node -> {
            if (node instanceof VBox && node != searchResultContainer.getChildren().get(0) &&
                    node != searchResultContainer.getChildren().get(1)) {
                node.setStyle(node.getStyle().replace("#e3f2fd", "#ffffff"));
            }
        });

        card.setStyle(card.getStyle().replace("#ffffff", "#e3f2fd"));

        this.selectedReservation = reservation;

        String status = reservation.getReservationStatus().toUpperCase();
        if (status.equals("COMPLETED") || status.equals("CANCELLED") || status.equals("CHECKED_OUT")) {
            verificationPane.setVisible(false);
            actionPane.setVisible(false);

            showAlert(Alert.AlertType.INFORMATION, "Reservasi Selesai",
                    "Reservasi ID: " + reservation.getReservationId() +
                            " status '" + status + "'. Sampai Jumpa dan Hati-hati di Jalan <3");
            return;
        }
        verificationPane.setVisible(true);
        verificationPane.setManaged(true);

        verifyNameField.clear();
        verifyContactField.clear();
        verifyDateField.setValue(null);

        showAlert(Alert.AlertType.INFORMATION, "Reservasi Dipilih",
                "Anda memilih reservasi ID: " + reservation.getReservationId() +
                        " untuk kamar " + reservation.getRoomNumber() +
                        ". Silakan lakukan verifikasi.");
    }

    @FXML
    void onVerifyButtonAction(ActionEvent event) {
        if (selectedReservation == null) {
            showAlert(Alert.AlertType.WARNING, "Pilih Reservasi",
                    "Pilih reservasi terlebih dahulu sebelum verifikasi.");
            return;
        }

        String inputName = verifyNameField.getText();
        String inputContact = verifyContactField.getText();
        LocalDate inputDate = verifyDateField.getValue();

        if (inputName.trim().isEmpty() || inputContact.trim().isEmpty() || inputDate == null) {
            showAlert(Alert.AlertType.WARNING, "Input Kosong",
                    "Nama, Kontak, dan Tanggal Reservasi harus diisi.");
            return;
        }

        if (inputName.equalsIgnoreCase(selectedReservation.getGuestName()) &&
                inputContact.equals(selectedReservation.getGuestContact()) &&
                inputDate.equals(selectedReservation.getReservationDate())) {

            String latestStatus = getLatestReservationStatus(selectedReservation.getReservationId());
            if (latestStatus != null) {
                showAlert(Alert.AlertType.INFORMATION, "Verifikasi Berhasil",
                        "Data cocok untuk reservasi ID: " + selectedReservation.getReservationId() +
                                ". Silakan lanjutkan.");

                verificationPane.setVisible(false);
                verificationPane.setManaged(false);
                actionPane.setVisible(true);
                actionPane.setManaged(true);

                guestCheckInButton.setDisable(!"CONFIRMED".equalsIgnoreCase(latestStatus));
                guestCheckOutButton.setDisable(!"CHECKED_IN".equalsIgnoreCase(latestStatus));
            } else {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Gagal mendapatkan status reservasi terbaru.");
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Verifikasi Gagal",
                    "Kombinasi Nama, Kontak, atau Tanggal Reservasi tidak cocok.");
        }
    }

    private String getLatestReservationStatus(int reservationId) {
        String sql = "SELECT reservation_status FROM public.reservation WHERE reservation_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("reservation_status");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @FXML
    void onGuestCheckInButtonAction(ActionEvent event) {
        if (selectedReservation != null) {
            updateRoomCheckStatus("Waiting for Check-In",
                    "Pengajuan Check-In untuk kamar " + selectedReservation.getRoomNumber() +
                            " telah dikirim. Mohon tunggu konfirmasi dari resepsionis.");
        }
    }

    @FXML
    void onGuestCheckOutButtonAction(ActionEvent event) {
        if (selectedReservation != null) {
            updateRoomCheckStatus("Waiting for Check-Out",
                    "Pengajuan Check-Out untuk kamar " + selectedReservation.getRoomNumber() +
                            " telah dikirim. Mohon tunggu konfirmasi dari resepsionis.");
        }
    }

    private void updateRoomCheckStatus(String newRoomStatus, String successMessage) {
        if (selectedReservation == null) return;

        String updateRoomSql = "UPDATE public.room SET check_status = ? WHERE room_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement psRoom = conn.prepareStatement(updateRoomSql)) {

            psRoom.setString(1, newRoomStatus);
            psRoom.setInt(2, selectedReservation.getRoomNumber());

            int affectedRows = psRoom.executeUpdate();
            if (affectedRows > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Sukses", successMessage);
                actionPane.setVisible(false);
            } else {
                showAlert(Alert.AlertType.ERROR, "Gagal",
                        "Tidak dapat menemukan kamar untuk diperbarui.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Gagal",
                    "Gagal memperbarui status kamar: " + e.getMessage());
        }
    }

    @FXML
    void onBackButtonAction(ActionEvent event) {
        if (roomPollingTimeline != null) {
            roomPollingTimeline.stop();
        }

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

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
