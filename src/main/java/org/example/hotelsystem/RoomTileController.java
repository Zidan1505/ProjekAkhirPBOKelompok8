package org.example.hotelsystem;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import org.example.hotelsystem.model.Room;

public class RoomTileController {

    @FXML
    private Label roomNumberLabel;

    @FXML
    private Label priceLabel;

    @FXML
    private Label facilityLabel;

    @FXML
    private AnchorPane rootPane;

    private Room room;
    private GuestViewController parentController;

    public void setData(Room room, GuestViewController parentController) {
        this.room = room;
        this.parentController = parentController;

        roomNumberLabel.setText(String.valueOf(room.getRoomNumber()));

        priceLabel.setText(String.format("Rp %,.0f / malam", room.getPrice()));

        StringBuilder facilities = new StringBuilder("Fasilitas: ");

        if (room.isAc()) {
            facilities.append("AC, ");
        }

        if (room.isSingleBed()) {
            facilities.append("Single Bed");
        } else if (room.isTwinBed()) {
            facilities.append("Twin Bed");
        } else if (room.isDoubleBed()) {
            facilities.append("Double Bed");
        } else {
            facilities.append("Tanpa Tempat Tidur");
        }

        String facilitiesText = facilities.toString().replaceAll(",\\s*$", "");

        facilityLabel.setText(facilitiesText);
    }

    public Room getRoom() {
        return room;
    }

    public AnchorPane getRootPane() {
        return rootPane;
    }

    @FXML
    void onTileClicked(MouseEvent event) {
        parentController.selectRoom(this.room, (Node) event.getSource());
    }
}
