/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hospital_management;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

/**
 *
 * @author ASUS
 */
public class DoctorMainFormController implements Initializable {

    @FXML
    private TextArea appointment_address;

    @FXML
    private TextField appointment_appointmentID;

    @FXML
    private Button appointment_btn;
    
    @FXML
    private Button logout_btn;

    @FXML
    private TextField appointment_description;

    @FXML
    private TextField appointment_diagnosis;

    @FXML
    private ComboBox<String> appointment_gender;

    @FXML
    private TextField appointment_mobileNumber;

    @FXML
    private TextField appointment_name;

    @FXML
    private ComboBox<String> appointment_status;

    @FXML
    private TextField appointment_treatment;

    @FXML
    private TableColumn<AppointmentData, String> appointments_col_action;

    @FXML
    private TableColumn<AppointmentData, String> appointments_col_appointmentID;

    @FXML
    private TableColumn<AppointmentData, String> appointments_col_contactNumber;

    @FXML
    private TableColumn<AppointmentData, String> appointments_col_date;

    @FXML
    private TableColumn<AppointmentData, String> appointments_col_dateDelete;

    @FXML
    private TableColumn<AppointmentData, String> appointments_col_dateModify;

    @FXML
    private TableColumn<AppointmentData, String> appointments_col_description;

    @FXML
    private TableColumn<AppointmentData, String> appointments_col_gender;

    @FXML
    private TableColumn<AppointmentData, String> appointments_col_name;

    @FXML
    private TableColumn<AppointmentData, String> appointments_col_status;

    @FXML
    private AnchorPane appointments_form;

    @FXML
    private TableView<AppointmentData> appointments_tableView;

    @FXML
    private Label current_form;

    @FXML
    private Label dashboard_IP;

    @FXML
    private Label dashboard_AP;

    @FXML
    private Label dashboard_TP;

    @FXML
    private Button dashboard_btn;

    @FXML
    private AreaChart<?, ?> dashboard_chart_DD;

    @FXML
    private AreaChart<?, ?> dashboard_chart_PD;

    @FXML
    private TableColumn<AppointmentData, String> dashboard_col_appointmentID;

    @FXML
    private TableColumn<AppointmentData, String> dashboard_col_name;

    @FXML
    private TableColumn<AppointmentData, String> dashboard_col_description;

    @FXML
    private TableColumn<AppointmentData, String> dashboard_col_appointmentDate;
    
    @FXML
    private TableColumn<AppointmentData, String> dashboard_col_status;

    @FXML
    private AnchorPane dashboard_form;

    @FXML
    private Label dashboard_tA;

    @FXML
    private TableView<AppointmentData> dashboard_tableView;

    @FXML
    private Label date_time;

    @FXML
    private AnchorPane main_form;

    @FXML
    private Label nav_adminID;

    @FXML
    private Label nav_username;

    @FXML
    private Button patient_btn;

    @FXML
    private Label patients_PA_dateCreated;

    @FXML
    private Label patients_PA_password;

    @FXML
    private Label patients_PA_patientID;

    @FXML
    private Button patients_PI_addBtn;

    @FXML
    private Label patients_PI_address;

    @FXML
    private Label patients_PI_gender;

    @FXML
    private Label patients_PI_mobileNumber;

    @FXML
    private Label patients_PI_patientName;

    @FXML
    private Button patients_PI_recordBtn;

    @FXML
    private TextArea patients_address;

    @FXML
    private Button patients_confirmBtn;

    @FXML
    private AnchorPane patients_form;

    @FXML
    private ComboBox<String> patients_gender;

    @FXML
    private TextField patients_mobileNumber;

    @FXML
    private TextField patients_password;

    @FXML
    private TextField patients_patientID;

    @FXML
    private TextField patients_patientName;

    @FXML
    private Button profile_btn;

    @FXML
    private Circle top_profile;

    @FXML
    private Label top_username;

    @FXML
    private DatePicker appointment_schedule;

    @FXML
    private Button appoinment_clearBtn;

    @FXML
    private Button appoinment_deleteBtn;

    @FXML
    private Button appoinment_insertBtn;

    @FXML
    private Button appoinment_updateBtn;
    
    @FXML
    private Circle profile_circleimage;

    @FXML
    private TextField profile_doctorID;

    @FXML
    private TextField profile_email;

    @FXML
    private AnchorPane profile_form;

    @FXML
    private ComboBox<String> profile_gender;

    @FXML
    private Button profile_importBtn;

    @FXML
    private Label profile_label_dateCreated;

    @FXML
    private Label profile_label_doctorID;

    @FXML
    private Label profile_label_email;

    @FXML
    private Label profile_label_name;

    @FXML
    private TextField profile_mobileNumber;

    @FXML
    private TextField profile_name;

    @FXML
    private ComboBox<String> profile_specialized;

    @FXML
    private ComboBox<String> profile_status;

    @FXML
    private Button profile_updateBtn;
    
    @FXML
    private TextArea profile_address;

    private Connection connect;
    private PreparedStatement prepare;
    private Statement statement;
    private ResultSet result;
    
    private Image image;
    private final AlertMessage alert = new AlertMessage();
    
    private Thread timeThread;
    private volatile boolean timeThreadRunning = true;

    public void runTime() {
        timeThread = new Thread(() -> {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
            while (timeThreadRunning) {
                try {
                    Thread.sleep(1000);
                    Platform.runLater(() -> {
                        date_time.setText(format.format(new Date()));
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        timeThread.setDaemon(true);
        timeThread.start();
    }

    public void appointmentUpdateBtn() {
        Connection conn = null;
        CallableStatement callable = null;
        
        try {
            if (validateAppointmentFields()) {
                conn = Database.getTransactionalConnection();
                
                if (alert.confirmationMessage("Are you sure you want to UPDATE Appointment ID: " + 
                    appointment_appointmentID.getText() + "?")) {
                    
                    String procedureCall = "{CALL update_appointment(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";
                    callable = conn.prepareCall(procedureCall);
                    
                    setAppointmentParameters(callable);
                    callable.execute();
                    conn.commit();
                    
                    appointmentShowData();
                    appointmentAppointmentID();
                    appointmentClearBtn();
                    alert.successMessage("Successfully Updated!");
                }
            }
        } catch (SQLException e) {
            Database.rollbackQuietly(conn);
            alert.errorMessage("An error occurred while updating the appointment: " + e.getMessage());
            e.printStackTrace();
        } finally {
            Database.closeResources(conn, callable, null);
        }
    }

    private boolean validateAppointmentFields() {
        if (appointment_appointmentID.getText().isEmpty()
                || appointment_name.getText().isEmpty()
                || appointment_gender.getSelectionModel().getSelectedItem() == null
                || appointment_mobileNumber.getText().isEmpty()
                || appointment_description.getText().isEmpty()
                || appointment_address.getText().isEmpty()
                || appointment_status.getSelectionModel().getSelectedItem() == null
                || appointment_schedule.getValue() == null
                || appointment_diagnosis.getText().isEmpty()
                || appointment_treatment.getText().isEmpty()) {
            alert.errorMessage("Please fill all the required fields, including diagnosis and treatment");
            return false;
        }
        return true;
    }

    private void setAppointmentParameters(CallableStatement callable) throws SQLException {
        callable.setString(1, Database.sanitizeInput(appointment_appointmentID.getText()));
        callable.setString(2, Database.sanitizeInput(appointment_name.getText()));
        callable.setString(3, appointment_gender.getSelectionModel().getSelectedItem());
        callable.setString(4, Database.sanitizeInput(appointment_mobileNumber.getText()));
        callable.setString(5, Database.sanitizeInput(appointment_description.getText()));
        callable.setString(6, Database.sanitizeInput(appointment_address.getText()));
        callable.setString(7, appointment_status.getSelectionModel().getSelectedItem());
        callable.setDate(8, java.sql.Date.valueOf(appointment_schedule.getValue()));
        callable.setString(9, Database.sanitizeInput(appointment_treatment.getText()));
        callable.setString(10, Database.sanitizeInput(appointment_diagnosis.getText()));
    }

    public void logoutBtn() {
        try {
            if (alert.confirmationMessage("Are you sure you want to logout?")) {
                timeThreadRunning = false;
                if (timeThread != null) {
                    timeThread.interrupt();
                }
                
                Data.clearDoctorData(); // This should be implemented in Data class
                Parent root = FXMLLoader.load(getClass().getResource("DoctorPage.fxml"));
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.show();

                logout_btn.getScene().getWindow().hide();
            }
        } catch (Exception e) {
            alert.errorMessage("Error during logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        displayAdminIDNumberName();
        runTime();
        
        dashbboardDisplayIP();
        dashbboardDisplayTP();
        dashbboardDisplayAP(); 
        dashbboardDisplayTA();
        dashboardDisplayData();
        dashboardNOP();
        dashboardNOA();

        appointmentShowData();
        appointmentGenderList();
        appointmentStatusList();
        appointmentAppointmentID();
        
        patientGenderList();
        
        profileLabels();
        profileFields();
        profileGenderList();
        profileSpecializedList();
        profileStatusList();
        profileDisplayImages(); 
    }
}
