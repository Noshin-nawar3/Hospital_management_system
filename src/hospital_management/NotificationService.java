package hospital_management;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NotificationService {
    private static final ScheduledExecutorService scheduler = 
        java.util.concurrent.Executors.newScheduledThreadPool(2);
    private static final java.util.Map<String, ScheduledFuture<?>> activeNotifications = 
        new java.util.concurrent.ConcurrentHashMap<>();
    private static final Image SUCCESS_ICON = new Image(NotificationService.class.getResourceAsStream("/images/success.png"));
    private static final Image WARNING_ICON = new Image(NotificationService.class.getResourceAsStream("/images/warning.png"));
    private static final Image ERROR_ICON = new Image(NotificationService.class.getResourceAsStream("/images/error.png"));

    public static void showInfoNotification(String title, String message) {
        Platform.runLater(() -> {
            Notifications.create()
                .title(title)
                .text(message)
                .position(Pos.TOP_RIGHT)
                .hideAfter(Duration.seconds(5))
                .showInformation();
        });
    }

    public static void showSuccessNotification(String title, String message) {
        Platform.runLater(() -> {
            Notifications.create()
                .title(title)
                .text(message)
                .position(Pos.TOP_RIGHT)
                .hideAfter(Duration.seconds(5))
                .showConfirm();
        });
    }

    public static void showWarningNotification(String title, String message) {
        Platform.runLater(() -> {
            Notifications.create()
                .title(title)
                .text(message)
                .position(Pos.TOP_RIGHT)
                .hideAfter(Duration.seconds(7))
                .showWarning();
        });
    }

    public static void showErrorNotification(String title, String message) {
        Platform.runLater(() -> {
            Notifications.create()
                .title(title)
                .text(message)
                .position(Pos.TOP_RIGHT)
                .hideAfter(Duration.seconds(10))
                .showError();
        });
    }

    public static void showPersistentNotification(String id, String title, String message) {
        Platform.runLater(() -> {
            Stage notificationStage = new Stage(StageStyle.UNDECORATED);
            notificationStage.initModality(Modality.NONE);

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            Label messageLabel = new Label(message);
            messageLabel.setWrapText(true);

            VBox content = new VBox(10, titleLabel, messageLabel);
            content.setAlignment(Pos.CENTER);
            content.setStyle("-fx-padding: 20px; -fx-background-color: white; " +
                "-fx-border-color: #cccccc; -fx-border-width: 1px;");

            Scene scene = new Scene(content);
            notificationStage.setScene(scene);
            notificationStage.show();

            // Store reference to allow later removal
            activeNotifications.put(id, null);
        });
    }

    public static void removePersistentNotification(String id) {
        ScheduledFuture<?> future = activeNotifications.remove(id);
        if (future != null) {
            future.cancel(true);
        }
    }

    public static void showDatabaseError(String operation, String errorMessage) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Error during " + operation);
            alert.setContentText("A database error occurred: " + errorMessage + 
                "\n\nPlease try again or contact support if the problem persists.");
            alert.showAndWait();

            Logger.logError("Database error during " + operation, 
                new Exception(errorMessage));
        });
    }

    public static void showSessionExpiryWarning(int minutesRemaining) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Session Expiring");
            alert.setHeaderText("Your session will expire soon");
            alert.setContentText("Your session will expire in " + minutesRemaining + 
                " minutes.\nWould you like to extend your session?");

            ButtonType extendButton = new ButtonType("Extend Session");
            ButtonType logoutButton = new ButtonType("Logout");
            alert.getButtonTypes().setAll(extendButton, logoutButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == extendButton) {
                    SecurityManager.extendSession();
                } else {
                    SecurityManager.logout();
                }
            }
        });
    }

    public static CompletableFuture<Boolean> showConfirmDialog(
            String title, String header, String content) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);

            Optional<ButtonType> result = alert.showAndWait();
            future.complete(result.isPresent() && result.get() == ButtonType.OK);
        });

        return future;
    }

    public static void scheduleAppointmentReminder(
            String appointmentId, String patientEmail, String patientName, 
            String doctorName, String appointmentDate, String appointmentTime, 
            long delayHours) {
        scheduler.schedule(() -> {
            EmailService.sendAppointmentReminder(
                patientEmail, patientName, doctorName, appointmentDate, appointmentTime);
            showInfoNotification("Appointment Reminder", 
                "Upcoming appointment tomorrow with Dr. " + doctorName);
        }, delayHours, TimeUnit.HOURS);
    }

    public static void showMaintenanceNotification(String message, long durationMinutes) {
        showPersistentNotification("maintenance", "System Maintenance", 
            message + "\nEstimated duration: " + durationMinutes + " minutes");

        scheduler.schedule(() -> 
            removePersistentNotification("maintenance"), 
            durationMinutes, TimeUnit.MINUTES);
    }

    public static void showBackupProgress(String status, double progress) {
        Platform.runLater(() -> {
            Notifications.create()
                .title("Backup Progress")
                .text(status + String.format(" (%.1f%%)", progress * 100))
                .position(Pos.BOTTOM_RIGHT)
                .hideAfter(Duration.seconds(3))
                .showInformation();
        });
    }

    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}