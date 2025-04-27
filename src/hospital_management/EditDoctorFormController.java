package hospital_management;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class EditDoctorFormController implements Initializable {

    @FXML
    private Button edit_btn;

    @FXML
    private TextField edit_doctorID;

    @FXML
    private TextField edit_email;

    @FXML
    private TextField edit_fullName;

    @FXML
    private ComboBox<String> edit_gender;

    @FXML
    private Circle edit_imageCircle;

    @FXML
    private Button edit_importBtn;

    @FXML
    private TextField edit_mobileNumber;

    @FXML
    private TextField edit_password;

    @FXML
    private ComboBox<String> edit_specialized;

    @FXML
    private TextField edit_address;

    @FXML
    private AnchorPane main_form;

    private AlertMessage alert;
    private Image image;
    private String currentImagePath;

    public void initialize(URL location, ResourceBundle resources) {
        alert = new AlertMessage();
        displayFields();
        genderList();
        specializedList();
    }

    public void displayFields() {
        edit_doctorID.setText(Data.temp_doctorID);
        edit_fullName.setText(Data.temp_doctorName);
        edit_email.setText(Data.temp_doctorEmail);
        edit_password.setText(Data.temp_doctorPassword);
        edit_gender.getSelectionModel().select(Data.temp_doctorGender);
        edit_mobileNumber.setText(Data.temp_doctorMobileNumber);
        edit_specialized.getSelectionModel().select(Data.temp_doctorSpecialized);
        edit_address.setText(Data.temp_doctorAddress);

        if (Data.temp_doctorImage != null) {
            currentImagePath = Data.temp_doctorImage;
            image = new Image("File:" + Data.temp_doctorImage, 115, 115, false, true);
            edit_imageCircle.setFill(new ImagePattern(image));
        }
    }

    public void updateBtn() {
        if (!validateDoctorInput()) {
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = Database.getTransactionalConnection();
            Map<String, String> changes = new HashMap<>();

            String updateQuery = "UPDATE doctor SET full_name = ?, email = ?, "
                    + "password = ?, gender = ?, mobile_number = ?, specialized = ?, "
                    + "address = ?, modify_date = CURRENT_TIMESTAMP";

            if (Data.path != null && !Data.path.isEmpty()) {
                updateQuery += ", image = ?";
            }

            updateQuery += " WHERE doctor_id = ?";

            ps = conn.prepareStatement(updateQuery);

            int paramIndex = 1;
            ps.setString(paramIndex++, edit_fullName.getText());
            ps.setString(paramIndex++, edit_email.getText());
            ps.setString(paramIndex++, edit_password.getText());
            ps.setString(paramIndex++, edit_gender.getSelectionModel().getSelectedItem());
            ps.setString(paramIndex++, edit_mobileNumber.getText());
            ps.setString(paramIndex++, edit_specialized.getSelectionModel().getSelectedItem());
            ps.setString(paramIndex++, edit_address.getText());

            // Track changes for audit
            trackFieldChange(changes, "full_name", Data.temp_doctorName, edit_fullName.getText());
            trackFieldChange(changes, "email", Data.temp_doctorEmail, edit_email.getText());
            trackFieldChange(changes, "gender", Data.temp_doctorGender,
                    edit_gender.getSelectionModel().getSelectedItem());
            trackFieldChange(changes, "mobile_number", Data.temp_doctorMobileNumber,
                    edit_mobileNumber.getText());
            trackFieldChange(changes, "specialized", Data.temp_doctorSpecialized,
                    edit_specialized.getSelectionModel().getSelectedItem());
            trackFieldChange(changes, "address", Data.temp_doctorAddress, edit_address.getText());

            if (Data.path != null && !Data.path.isEmpty()) {
                Path source = Paths.get(Data.path);
                Path destination = Paths.get("Doctor_Directory",
                        edit_doctorID.getText() + getFileExtension(Data.path));

                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                ps.setString(paramIndex++, destination.toString());
                trackFieldChange(changes, "image", currentImagePath, destination.toString());
            }

            ps.setString(paramIndex, edit_doctorID.getText());

            if (alert.confirmationMessage("Are you sure you want to update this doctor's information?")) {
                ps.executeUpdate();

                // Log changes to audit trail
                if (!changes.isEmpty()) {
                    AuditTrail.trackDoctorChanges(edit_doctorID.getText(), changes,
                            String.valueOf(Data.admin_id));
                }

                // Send notification if email changed
                if (changes.containsKey("email")) {
                    EmailService.sendEmailAsync(edit_email.getText(),
                            "Doctor Profile Updated",
                            "Your profile information has been updated by the administrator.");
                }

                conn.commit();
                alert.successMessage("Updated Successfully!");
                closeStage();
            }

        } catch (Exception e) {
            Database.rollbackQuietly(conn);
            Logger.logError("Error updating doctor information", e);
            NotificationService.showDatabaseError("doctor update", e.getMessage());
        } finally {
            Database.closeResources(conn, ps, null);
        }
    }

    private void trackFieldChange(Map<String, String> changes, String field,
                                  String oldValue, String newValue) {
        if (oldValue == null) oldValue = "";
        if (newValue == null) newValue = "";
        if (!oldValue.equals(newValue)) {
            changes.put(field, oldValue + "->" + newValue);
        }
    }

    private boolean validateDoctorInput() {
        ValidationResult result = ValidationUtils.validateDoctorInput(
                edit_fullName.getText(),
                edit_email.getText(),
                edit_mobileNumber.getText(),
                edit_specialized.getSelectionModel().getSelectedItem(),
                edit_gender.getSelectionModel().getSelectedItem(),
                edit_address.getText()
        );

        if (result.hasErrors()) {
            NotificationService.showErrorNotification("Validation Error", result.getErrorMessage());
            return false;
        }
        return true;
    }

    public void importBtn() {
        FileChooser open = new FileChooser();
        open.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = open.showOpenDialog(edit_importBtn.getScene().getWindow());
        if (file != null) {
            Data.path = file.getAbsolutePath();
            image = new Image(file.toURI().toString(), 115, 115, false, true);
            edit_imageCircle.setFill(new ImagePattern(image));
        }
    }

    private String getFileExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(lastDot) : "";
    }

    public void genderList() {
        ObservableList<String> genderL = FXCollections.observableArrayList(Data.gender);
        edit_gender.setItems(genderL);
    }

    public void specializedList() {
        ObservableList<String> specializedL = FXCollections.observableArrayList(Data.specialized);
        edit_specialized.setItems(specializedL);
    }

    private void closeStage() {
        Stage stage = (Stage) main_form.getScene().getWindow();
        stage.close();
    }
}
