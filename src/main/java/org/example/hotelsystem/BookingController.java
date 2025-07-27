package org.example.hotelsystem;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.hotelsystem.model.Room;
import org.example.hotelsystem.util.DatabaseConnection;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class BookingController implements Initializable {

    @FXML private Label roomNumberLabel;
    @FXML private TextField guestNameField;
    @FXML private TextField guestContactField;
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private TextField paymentField;
    @FXML private ComboBox<String> paymentMethodComboBox;
    @FXML private TextField accountNumberField;
    @FXML private Button payButton;
    @FXML private Button downloadReceiptButton;

    private Room roomToBook;
    private GuestViewController parentController;
    private int uniqueCode;
    private double totalPayment;
    private int newReservationId;
    private Map<String, String> paymentOptionsMap;

    public void initData(Room room, GuestViewController parentController, LocalDate checkIn, LocalDate checkOut) {
        this.roomToBook = room;
        this.parentController = parentController;

        roomNumberLabel.setText("No: " + room.getRoomNumber());
        checkInPicker.setValue(checkIn);
        checkOutPicker.setValue(checkOut);
        checkInPicker.setDisable(true);
        checkOutPicker.setDisable(true);

        long duration = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (duration <= 0) duration = 1;
        this.uniqueCode = getNextUniqueCode();
        this.totalPayment = (room.getPrice() * duration) + this.uniqueCode;
        paymentField.setText(String.format("Rp %,.0f", totalPayment));
        downloadReceiptButton.setDisable(true);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        paymentOptionsMap = new HashMap<>();
        paymentOptionsMap.put("Mandiri", "1060938030");
        paymentOptionsMap.put("BCA", "3297490028");
        paymentOptionsMap.put("BRI", "6930083002829");
        paymentOptionsMap.put("Gopay", "0812934953274");

        paymentMethodComboBox.setItems(FXCollections.observableArrayList(paymentOptionsMap.keySet()));

        paymentMethodComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                accountNumberField.setText(paymentOptionsMap.get(newVal));
            } else {
                accountNumberField.clear();
            }
        });
    }

    @FXML
    void onPayButtonAction(ActionEvent event) {
        String guestName = guestNameField.getText();
        String guestContact = guestContactField.getText();
        LocalDate checkInDate = checkInPicker.getValue();
        LocalDate checkOutDate = checkOutPicker.getValue();

        if (guestName.trim().isEmpty() || guestContact.trim().isEmpty() || paymentMethodComboBox.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Semua field harus diisi.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            String checkSql = "SELECT r.status, (SELECT COUNT(*) FROM public.reservation " +
                    "WHERE room_number = ? AND check_in_date < ? AND check_out_date > ?) as booking_count " +
                    "FROM public.room r WHERE r.room_number = ? FOR UPDATE";

            try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                psCheck.setInt(1, roomToBook.getRoomNumber());
                psCheck.setDate(2, Date.valueOf(checkOutDate));
                psCheck.setDate(3, Date.valueOf(checkInDate));
                psCheck.setInt(4, roomToBook.getRoomNumber());

                ResultSet rsCheck = psCheck.executeQuery();

                if (rsCheck.next()) {
                    String roomStatus = rsCheck.getString("status");
                    int bookingCount = rsCheck.getInt("booking_count");

                    if (!"Available".equalsIgnoreCase(roomStatus) || bookingCount > 0) {
                        showAlert(Alert.AlertType.WARNING, "Pemesanan gagal", "Maaf, kamar ini telah dipesan oleh orang lain, tolong pilih kamar atau tanggal lain.");
                        conn.rollback();
                        parentController.loadAvailableRooms(checkInDate, checkOutDate);
                        closeWindow();
                        return;
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Pemesanan gagal", "Maaf, Kamar tidak tersedia.");
                    conn.rollback();
                    return;
                }
            }

            String insertSql = "INSERT INTO public.reservation (room_number, guest_name, guest_contact, reservation_date, check_in_date, check_out_date, reservation_status) VALUES (?, ?, ?, ?, ?, ?, 'CONFIRMED') RETURNING reservation_id";

            try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                psInsert.setInt(1, roomToBook.getRoomNumber());
                psInsert.setString(2, guestName);
                psInsert.setString(3, guestContact);
                psInsert.setDate(4, Date.valueOf(LocalDate.now()));
                psInsert.setDate(5, Date.valueOf(checkInDate));
                psInsert.setDate(6, Date.valueOf(checkOutDate));

                ResultSet generatedKeys = psInsert.executeQuery();
                if (generatedKeys.next()) {
                    this.newReservationId = generatedKeys.getInt(1);

                    String updateRoomSql = "UPDATE public.room SET status = 'not available' WHERE room_number = ?";
                    try (PreparedStatement psUpdateRoom = conn.prepareStatement(updateRoomSql)) {
                        psUpdateRoom.setInt(1, roomToBook.getRoomNumber());
                        psUpdateRoom.executeUpdate();
                    }

                    conn.commit();

                    showAlert(Alert.AlertType.INFORMATION, "Sukses", "Pemesanan kamar berhasil! Kode reservasi kamu adalah: " + this.newReservationId);
                    payButton.setDisable(true);
                    downloadReceiptButton.setDisable(false);
                    parentController.loadAvailableRooms(checkInDate, checkOutDate);
                } else {
                    conn.rollback();
                    throw new SQLException("Gagal membuat reservasi, no ID tidak ada.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Terjadi kesalahan pada: " + e.getMessage());
        }
    }

    @FXML
    void onDownloadReceiptButtonAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan Struk Reservasi");
        fileChooser.setInitialFileName("struk_reservasi_" + this.newReservationId + ".txt");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = fileChooser.showSaveDialog(new Stage());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("--- STRUK RESERVASI HOTEL ---\n");
                writer.write("ID Reservasi: " + this.newReservationId + "\n\n");
                writer.write("Nama Tamu\t\t: " + guestNameField.getText() + "\n");
                writer.write("Kontak\t\t\t: " + guestContactField.getText() + "\n\n");
                writer.write("Tanggal Check-In\t: " + checkInPicker.getValue() + "\n");
                writer.write("Tanggal Check-Out\t: " + checkOutPicker.getValue() + "\n\n");
                writer.write("--- DETAIL KAMAR & PEMBAYARAN ---\n");
                writer.write("Kamar No.\t\t: " + roomToBook.getRoomNumber() + "\n");
                writer.write("Harga per Malam\t: Rp " + String.format("%,.0f", roomToBook.getPrice()) + "\n");
                writer.write("Kode Unik\t\t: " + this.uniqueCode + "\n");
                writer.write("TOTAL PEMBAYARAN\t: Rp " + String.format("%,.0f", totalPayment) + "\n\n");
                writer.write("--- Terima kasih ---\n");
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Struk berhasil disimpan di: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Gagal", "Gagal menyimpan file struk.");
            }
        }
    }

    private int getNextUniqueCode() {
        String selectSql = "SELECT current_value FROM public.app_counters WHERE counter_name = 'unique_payment_code' FOR UPDATE";
        String updateSql = "UPDATE public.app_counters SET current_value = ? WHERE counter_name = 'unique_payment_code'";
        int nextCode = 1;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                 PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {

                ResultSet rs = stmt.executeQuery(selectSql);
                int currentValue = 0;
                if (rs.next()) {
                    currentValue = rs.getInt("current_value");
                }

                nextCode = (currentValue >= 100) ? 1 : currentValue + 1;

                psUpdate.setInt(1, nextCode);
                psUpdate.executeUpdate();
                conn.commit();

                return nextCode;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    private void closeWindow() {
        Stage stage = (Stage) payButton.getScene().getWindow();
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
