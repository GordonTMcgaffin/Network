/**
 * @file    Alert.java
 * @brief   Show an {@code Alert}.
 * @author  J. P. Visser (21553416@sun.ac.za)
 * @date    2022-08-16
 */

import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.layout.*;

/**
 * The {@code Alerter} class contains a single function for showing
 * {@code Alerts}.
 */
public final class Alerter {

    /**
     * Shows an {@code Alert} to inform the user.
     *
     * @param type  type of {@code Alert}
     * @param titleAndHeader  the alert's title and header text
     * @param msg  message to display
     */
    public static void showAlert(AlertType type, String titleAndHeader, String msg)
    {
        Alert alert = new Alert(type);
        alert.setResizable(true);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setTitle(titleAndHeader);
        alert.setHeaderText(titleAndHeader);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
