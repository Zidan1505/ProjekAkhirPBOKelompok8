package org.example.hotelsystem;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.hotelsystem.model.Employee;
import org.example.hotelsystem.model.HousekeepingRecord;
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
import java.util.Optional;
import java.util.ResourceBundle;

public class AdministratorController implements Initializable {

    private Timeline employeeRefreshTimeline;
    private Timeline roomRefreshTimeline;
    private Timeline reservationRefreshTimeline;
    private Timeline housekeepingRefreshTimeline;
    private Timeline forcedLogoutTimeline;
    private int loggedInAdminId;

    private final ObservableList<Employee> masterEmployeeData = FXCollections.observableArrayList();
    private final ObservableList<Room> masterRoomData = FXCollections.observableArrayList();
    private final ObservableList<Reservation> masterReservationData = FXCollections.observableArrayList();
    private final ObservableList<HousekeepingRecord> masterHousekeepingData = FXCollections.observableArrayList();

    @FXML private TextField searchEmployeeField;
    @FXML private TableView<Employee> employeeTable;
    @FXML private TableColumn<Employee, String> fullnameColumn;
    @FXML private TableColumn<Employee, String> statusEmployeeColumn;
    @FXML private TableView<Employee> detailTable;
    @FXML private TableColumn<Employee, Integer> idColumn;
    @FXML private TableColumn<Employee, String> usernameColumn;
    @FXML private TableColumn<Employee, String> emailColumn;
    @FXML private TableColumn<Employee, String> contactColumn;
    @FXML private Button addEmployeeButton;
    @FXML private Button editEmployeeButton;
    @FXML private Button deleteEmployeeButton;
    @FXML private Button forceLogoutButton;

    @FXML private TextField searchRoomField;
    @FXML private TableView<Room> roomTable;
    @FXML private TableColumn<Room, Integer> roomNumberColumn;
    @FXML private TableColumn<Room, String> statusRoomColumn;
    @FXML private TableColumn<Room, String> checkStatusColumn;
    @FXML private TableColumn<Room, Float> priceColumn;
    @FXML private TableColumn<Room, Boolean> acColumn;
    @FXML private TableColumn<Room, Boolean> singleBedColumn;
    @FXML private TableColumn<Room, Boolean> twinBedColumn;
    @FXML private TableColumn<Room, Boolean> doubleBedColumn;
    @FXML private Button editRoomButton;
    @FXML private Button addRoomButton;
    @FXML private Button deleteRoomButton;

    @FXML private TextField searchReservationField;
    @FXML private TableView<Reservation> reservationTable;
    @FXML private TableColumn<Reservation, Integer> roomNumberColumn1;
    @FXML private TableColumn<Reservation, Integer> statusRoomColumn1;
    @FXML private TableColumn<Reservation, Integer> checkStatusColumn1;
    @FXML private TableColumn<Reservation, String> priceColumn1;
    @FXML private TableColumn<Reservation, LocalDate> acColumn1;
    @FXML private TableColumn<Reservation, LocalDate> singleBedColumn1;
    @FXML private TableColumn<Reservation, LocalDate> twinBedColumn1;
    @FXML private TableColumn<Reservation, String> doubleBedColumn1;
    @FXML private Button editReservationButton;
    @FXML private Button deleteReservationButton;

    @FXML private TextField searchHousekeepingField;
    @FXML private TableView<HousekeepingRecord> housekeepingRecordTable;
    @FXML private TableColumn<HousekeepingRecord, Integer> idRecordColumn;
    @FXML private TableColumn<HousekeepingRecord, Integer> housekeepingIdColumn;
    @FXML private TableColumn<HousekeepingRecord, Integer> roomNumberHkColumn;
    @FXML private TableColumn<HousekeepingRecord, LocalDate> recordDateColumn;
    @FXML private TableColumn<HousekeepingRecord, String> descriptionColumn;
    @FXML private Button editHousekeepingRecordButton;
    @FXML private Button editHousekeepingButton1;
    @FXML private Button deleteHousekeepingButton11;

    public void initData(int adminId) {
        this.loggedInAdminId = adminId;
        Main.currentUserId = this.loggedInAdminId;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupEmployeeTab();
        setupRoomTab();
        setupReservationTab();
        setupHousekeepingTab();

        loadEmployeeData();
        loadAllRoomsData();
        loadReservationData();
        loadHousekeepingData();

        setupTimelines();
    }

    // --- LOGIKA UNTUK TAB EMPLOYEE ---
    private void setupEmployeeTab() {
        fullnameColumn.setCellValueFactory(new PropertyValueFactory<>("fullname"));
        statusEmployeeColumn.setCellValueFactory(new PropertyValueFactory<>("statusEmployee"));
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        contactColumn.setCellValueFactory(new PropertyValueFactory<>("contact"));

        FilteredList<Employee> filteredData = new FilteredList<>(masterEmployeeData, p -> true);

        searchEmployeeField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(employee -> {
                if (newValue == null || newValue.isBlank()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (employee.getFullname().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else return employee.getUsername().toLowerCase().contains(lowerCaseFilter);
            });
        });

        employeeTable.setItems(filteredData);

        employeeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                detailTable.setItems(FXCollections.observableArrayList(newVal));
            } else {
                detailTable.getItems().clear();
            }
        });
    }

    public void loadEmployeeData() {
        masterEmployeeData.clear();
        String sql = "SELECT * FROM public.employee WHERE CAST(id AS TEXT) NOT LIKE '1%' ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                masterEmployeeData.add(new Employee(rs.getInt("id"), rs.getString("username"), rs.getString("user_password"), rs.getString("fullname"), rs.getString("email"), rs.getString("contact"), rs.getString("status_employee")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- LOGIKA UNTUK TAB ROOM ---
    private void setupRoomTab() {
        roomNumberColumn.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        statusRoomColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        checkStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checkStatus"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        acColumn.setCellValueFactory(new PropertyValueFactory<>("ac"));
        singleBedColumn.setCellValueFactory(new PropertyValueFactory<>("singleBed"));
        twinBedColumn.setCellValueFactory(new PropertyValueFactory<>("twinBed"));
        doubleBedColumn.setCellValueFactory(new PropertyValueFactory<>("doubleBed"));

        FilteredList<Room> filteredData = new FilteredList<>(masterRoomData, p -> true);

        searchRoomField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(room -> {
                if (newValue == null || newValue.isBlank()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return String.valueOf(room.getRoomNumber()).contains(lowerCaseFilter);
            });
        });

        roomTable.setItems(filteredData);
    }

    public void loadAllRoomsData() {
        masterRoomData.clear();
        String sql = "SELECT * FROM public.room ORDER BY room_number ASC";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while(rs.next()) {
                Date lastModified = rs.getDate("last_modified_date");
                masterRoomData.add(new Room(
                        rs.getInt("room_number"), rs.getString("status"), rs.getFloat("price"),
                        rs.getBoolean("ac"), rs.getBoolean("single_bed"), rs.getBoolean("twin_bed"),
                        rs.getBoolean("double_bed"), rs.getString("check_status"),
                        rs.getInt("last_change_by_admin_id"), (lastModified != null) ? lastModified.toLocalDate() : null
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- LOGIKA UNTUK TAB RESERVATION ---
    private void setupReservationTab() {
        roomNumberColumn1.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
        statusRoomColumn1.setCellValueFactory(new PropertyValueFactory<>("receptionistId"));
        checkStatusColumn1.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        priceColumn1.setCellValueFactory(new PropertyValueFactory<>("guestName"));
        acColumn1.setCellValueFactory(new PropertyValueFactory<>("reservationDate"));
        singleBedColumn1.setCellValueFactory(new PropertyValueFactory<>("checkInDate"));
        twinBedColumn1.setCellValueFactory(new PropertyValueFactory<>("checkOutDate"));
        doubleBedColumn1.setCellValueFactory(new PropertyValueFactory<>("reservationStatus"));

        FilteredList<Reservation> filteredData = new FilteredList<>(masterReservationData, p -> true);

        searchReservationField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(reservation -> {
                if (newValue == null || newValue.isBlank()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (reservation.getGuestName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else return String.valueOf(reservation.getRoomNumber()).contains(lowerCaseFilter);
            });
        });

        reservationTable.setItems(filteredData);
    }

    private void loadReservationData() {
        masterReservationData.clear();
        String sql = "SELECT res.*, r.status AS room_status " +
                "FROM public.reservation res " +
                "LEFT JOIN public.room r ON res.room_number = r.room_number " +
                "ORDER BY res.reservation_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                masterReservationData.add(new Reservation(
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
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Gagal memuat data reservasi.");
        }
    }

    // --- LOGIKA UNTUK TAB HOUSEKEEPING ---
    private void setupHousekeepingTab() {
        idRecordColumn.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        housekeepingIdColumn.setCellValueFactory(new PropertyValueFactory<>("housekeepingId"));
        roomNumberHkColumn.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        recordDateColumn.setCellValueFactory(new PropertyValueFactory<>("recordDate"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));

        FilteredList<HousekeepingRecord> filteredData = new FilteredList<>(masterHousekeepingData, p -> true);

        searchHousekeepingField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(record -> {
                if (newValue == null || newValue.isBlank()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return String.valueOf(record.getRoomNumber()).contains(lowerCaseFilter);
            });
        });

        housekeepingRecordTable.setItems(filteredData);
    }

    private void loadHousekeepingData() {
        masterHousekeepingData.clear();
        String sql = "SELECT * FROM public.housekeeping_record ORDER BY record_id DESC";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                masterHousekeepingData.add(new HousekeepingRecord(
                        rs.getInt("record_id"),
                        rs.getInt("housekeeping_id"),
                        rs.getInt("room_number"),
                        rs.getDate("record_date").toLocalDate(),
                        rs.getString("notes")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Gagal memuat data housekeeping.");
        }
    }

    // --- TIMELINES & OTHER METHODS ---
    @FXML void onForceLogoutButtonAction(ActionEvent event) {
        Employee selectedEmployee = employeeTable.getSelectionModel().getSelectedItem();
        if (selectedEmployee == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih karyawan yang ingin di-logout.");
            return;
        }
        if ("Logged-Out".equalsIgnoreCase(selectedEmployee.getStatusEmployee())) {
            showAlert(Alert.AlertType.INFORMATION, "Informasi", "Karyawan " + selectedEmployee.getFullname() + " sudah dalam status Logged-Out.");
            return;
        }
        String sql = "UPDATE public.employee SET status_employee = 'Logged-Out' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, selectedEmployee.getId());
            ps.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Karyawan " + selectedEmployee.getFullname() + " berhasil di-logout paksa.");
            loadEmployeeData();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Gagal", "Gagal mengubah status karyawan.");
        }
    }

    private void setupTimelines() {
        this.employeeRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> loadEmployeeData()));
        this.employeeRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        this.employeeRefreshTimeline.play();

        this.roomRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> loadAllRoomsData()));
        this.roomRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        this.roomRefreshTimeline.play();

        this.reservationRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(7), event -> loadReservationData()));
        this.reservationRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        this.reservationRefreshTimeline.play();

        this.housekeepingRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(7), event -> loadHousekeepingData()));
        this.housekeepingRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        this.housekeepingRefreshTimeline.play();

        this.forcedLogoutTimeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> checkMyStatus()));
        this.forcedLogoutTimeline.setCycleCount(Timeline.INDEFINITE);
        this.forcedLogoutTimeline.play();
    }

    private void checkMyStatus() {
        if (this.loggedInAdminId <= 0) return;
        String sql = "SELECT status_employee FROM public.employee WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, this.loggedInAdminId);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && "Logged-Out".equals(rs.getString("status_employee"))) {
                Platform.runLater(this::forceLogout);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void forceLogout() {
        if (employeeRefreshTimeline != null) employeeRefreshTimeline.stop();
        if (roomRefreshTimeline != null) roomRefreshTimeline.stop();
        if (reservationRefreshTimeline != null) reservationRefreshTimeline.stop();
        if (housekeepingRefreshTimeline != null) housekeepingRefreshTimeline.stop();
        if (forcedLogoutTimeline != null) forcedLogoutTimeline.stop();
        try {
            Main.currentUserId = -1;
            Stage stage = (Stage) addRoomButton.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Login.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            stage.setScene(scene);
            stage.setTitle("Login Staf");
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML void onAddEmployeeButton(ActionEvent event) {
        openUpdateEmployeeWindow(null);
    }

    @FXML void onEditEmployeeButton(ActionEvent event) {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih karyawan yang akan diedit.");
            return;
        }
        openUpdateEmployeeWindow(selected);
    }

    @FXML void onDeleteEmployeeButton(ActionEvent event) {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih karyawan yang akan dihapus.");
            return;
        }

        if (selected.getId() == this.loggedInAdminId) {
            showAlert(Alert.AlertType.ERROR, "Gagal", "Anda tidak dapat menghapus akun Anda sendiri.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Hapus");
        alert.setHeaderText("Hapus Karyawan: " + selected.getFullname());
        alert.setContentText("Apakah Anda yakin? Tindakan ini tidak dapat dibatalkan.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String sql = "DELETE FROM public.employee WHERE id = ?";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, selected.getId());
                    ps.executeUpdate();
                    showAlert(Alert.AlertType.INFORMATION, "Sukses", "Karyawan berhasil dihapus.");
                    loadEmployeeData(); // Refresh tabel
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Gagal", "Gagal menghapus karyawan: " + e.getMessage());
                }
            }
        });
    }

    @FXML void onAddRoomButtonAction(ActionEvent event) { openUpdateRoomWindow(null); }

    @FXML void onEditRoomButtonAction(ActionEvent event) {
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        if(selected == null) { showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih kamar yang akan diedit."); return; }
        openUpdateRoomWindow(selected);
    }

    @FXML
    void onDeleteRoomButtonAction(ActionEvent event) {
        Room selectedRoom = roomTable.getSelectionModel().getSelectedItem();
        if (selectedRoom == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih kamar yang ingin dihapus.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Hapus");
        alert.setHeaderText("Hapus Kamar No: " + selectedRoom.getRoomNumber());
        alert.setContentText("Apakah Anda yakin ingin menghapus kamar ini?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM public.room WHERE room_number = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, selectedRoom.getRoomNumber());
                int affectedRows = ps.executeUpdate();
                if (affectedRows > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Sukses", "Kamar " + selectedRoom.getRoomNumber() + " berhasil dihapus.");
                    loadAllRoomsData();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Gagal", "Kamar tidak ditemukan di database.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error Database", "Gagal menghapus kamar: " + e.getMessage());
            }
        }
    }

    @FXML void onEditReservationButton(ActionEvent event) {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if(selected == null) { showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih reservasi yang akan diedit."); return; }
        openUpdateReservationWindow(selected);
    }

    @FXML
    void onDeleteReservationButton(ActionEvent event) {
        Reservation selectedReservation = reservationTable.getSelectionModel().getSelectedItem();
        if (selectedReservation == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data reservasi yang ingin dihapus.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Hapus");
        alert.setHeaderText("Hapus Reservasi ID: " + selectedReservation.getReservationId() + " (Tamu: " + selectedReservation.getGuestName() + ")");
        alert.setContentText("Apakah Anda yakin ingin menghapus reservasi ini? Tindakan ini tidak dapat dibatalkan.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM public.reservation WHERE reservation_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, selectedReservation.getReservationId());
                int affectedRows = ps.executeUpdate();
                if (affectedRows > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Sukses", "Reservasi berhasil dihapus.");
                    loadReservationData();
                    loadAllRoomsData();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Gagal", "Reservasi tidak ditemukan di database.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error Database", "Gagal menghapus reservasi: " + e.getMessage());
            }
        }
    }

    @FXML void onHousekeepingRecordTable(SortEvent<TableView<HousekeepingRecord>> event) {}

    @FXML void onEditHousekeepingButton(ActionEvent event) {
        openUpdateHousekeepingWindow(null);
    }

    @FXML void onEditHousekeepingRecordButton(ActionEvent event) {
        HousekeepingRecord selected = housekeepingRecordTable.getSelectionModel().getSelectedItem();
        if(selected == null) { showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih record yang akan diedit."); return; }
        openUpdateHousekeepingWindow(selected);
    }

    @FXML
    void onDeleteHousekeepingButton(ActionEvent event) {
        HousekeepingRecord selectedRecord = housekeepingRecordTable.getSelectionModel().getSelectedItem();
        if (selectedRecord == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih record housekeeping yang ingin dihapus.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Hapus");
        alert.setHeaderText("Hapus Record ID: " + selectedRecord.getRecordId() + " (Kamar No: " + selectedRecord.getRoomNumber() + ")");
        alert.setContentText("Apakah Anda yakin ingin menghapus record ini? Tindakan ini tidak dapat dibatalkan.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM public.housekeeping_record WHERE record_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, selectedRecord.getRecordId());
                int affectedRows = ps.executeUpdate();
                if (affectedRows > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Sukses", "Record housekeeping berhasil dihapus.");
                    loadHousekeepingData();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Gagal", "Record tidak ditemukan di database.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error Database", "Gagal menghapus record: " + e.getMessage());
            }
        }
    }

    private void openUpdateEmployeeWindow(Employee employee) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UpdateEmployee.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(employee != null ? "Edit Karyawan" : "Tambah Karyawan Baru");
            stage.setScene(new Scene(loader.load()));
            UpdateEmployeeController controller = loader.getController();
            controller.initData(employee, this);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal membuka jendela editor karyawan.");
        }
    }

    private void openUpdateRoomWindow(Room room) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UpdateRoom.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(room != null ? "Edit Kamar " + room.getRoomNumber() : "Tambah Kamar Baru");
            stage.setScene(new Scene(loader.load()));
            UpdateRoomController controller = loader.getController();
            controller.initData(room, this, this.loggedInAdminId);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal membuka jendela editor kamar.");
        }
    }

    private void openUpdateReservationWindow(Reservation reservation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UpdateReservation.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(reservation != null ? "Edit Reservasi" : "Tambah Reservasi Baru");
            stage.setScene(new Scene(loader.load()));
            UpdateReservationController controller = loader.getController();
            controller.initData(reservation, this, this.loggedInAdminId);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal membuka jendela editor reservasi.");
        }
    }

    private void openUpdateHousekeepingWindow(HousekeepingRecord record) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UpdateHousekeeping.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(record != null ? "Edit Record Housekeeping" : "Tambah Record Baru");
            stage.setScene(new Scene(loader.load()));
            UpdateHousekeepingController controller = loader.getController();
            controller.initData(record, this);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal membuka jendela editor housekeeping.");
        }
    }
}