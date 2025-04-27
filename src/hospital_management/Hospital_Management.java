/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hospital_management;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author ASUS
 */
public class Hospital_Management extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        // Initialize configuration first
        Configuration.reloadConfiguration();
        
        // Start the scheduler service
        SchedulerService.start();
        
        // Load and show the main window
        Parent root = FXMLLoader.load(getClass().getResource("FXMLDocument.fxml"));
        Scene scene = new Scene(root);
        stage.setTitle("Hospital Management System");
        stage.setScene(scene);
        stage.show();
        
        // Log application start
        Logger.logAudit("SYSTEM", "STARTUP", "Application started successfully");
    }
    
    @Override
    public void stop() {
        // Shutdown all services gracefully
        try {
            Logger.logAudit("SYSTEM", "SHUTDOWN", "Application shutdown initiated");
            
            // Stop the scheduler service
            SchedulerService.shutdown();
            
            // Stop the email service
            EmailService.shutdown();
            
            // Stop the notification service
            NotificationService.shutdown();
            
            // Close database connections
            Database.cleanupPool();
            
            // Final audit log
            Logger.logAudit("SYSTEM", "SHUTDOWN", "Application shutdown completed");
        } catch (Exception e) {
            System.err.println("Error during application shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
