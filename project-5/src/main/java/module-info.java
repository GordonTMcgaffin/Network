module com.p2p.project5 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens com.p2p to javafx.fxml;
    exports com.p2p;
}