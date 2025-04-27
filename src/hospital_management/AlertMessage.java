/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hospital_management;

import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

/**
 *
 * @author ASUS
 */
public class AlertMessage {

    public boolean confirmationMessage(String message) {
        Alert alert = createBaseAlert(Alert.AlertType.CONFIRMATION, "Confirmation Message", message);
        
        Optional<ButtonType> option = alert.showAndWait();
        return option.get().equals(ButtonType.OK);
    }

    public void errorMessage(String message) {
        Alert alert = createBaseAlert(Alert.AlertType.ERROR, "Error Message", message);
        alert.showAndWait();
    }

    public void successMessage(String message) {
        Alert alert = createBaseAlert(Alert.AlertType.INFORMATION, "Information Message", message);
        alert.showAndWait();
    }

    private Alert createBaseAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Set custom styling
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/design/MainFormDesign.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-alert");
        
        return alert;
    }

    public void showDatabaseError(String operation, Exception e) {
        String message = String.format("Database error during %s: %s", operation, e.getMessage());
        errorMessage(message);
    }

    public void showValidationError(String field) {
        errorMessage("Please enter a valid " + field);
    }

    public void showRequiredFieldError(String field) {
        errorMessage(field + " is required");
    }

    public void showAccessDenied() {
        errorMessage("Access denied. Please check your credentials.");
    }

    public void showSessionExpired() {
        errorMessage("Your session has expired. Please log in again.");
    }

    public void showOperationSuccess(String operation) {
        successMessage(operation + " completed successfully!");
    }
}
