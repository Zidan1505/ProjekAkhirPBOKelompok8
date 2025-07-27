module org.example.hotelsystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.sql;
    requires org.kordamp.ikonli.fontawesome5;
    requires java.desktop;

    // Izinkan FXML mengakses controller Anda
    opens org.example.hotelsystem to javafx.fxml;
    // Izinkan JavaFX TableView mengakses kelas model Anda
    opens org.example.hotelsystem.model to javafx.base;

    exports org.example.hotelsystem;
}