module org.example.osmaintenance {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;

    requires java.desktop;
    requires com.sun.jna;
    requires java.sql;
    requires com.github.oshi;
    requires java.management;
    requires jdk.management;

    opens org.osmaintenance to javafx.fxml;
    exports org.osmaintenance;
    exports org.osmaintenance.ui;
    opens org.osmaintenance.ui to javafx.fxml;
}