/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hospital_management;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 *
 * @author ASUS
 */
public class FXMLDocumentController implements Initializable {
    
    
    @FXML
    private CheckBox login_checkBox;

    @FXML
    private AnchorPane login_form;

    @FXML
    private Button login_loginBtn;

    @FXML
    private PasswordField login_password;

    @FXML
    private Hyperlink login_registerHere;

    @FXML
    private TextField login_showPassword;

    @FXML
    private ComboBox<?> login_user;

    @FXML
    private TextField login_username;

    @FXML
    private AnchorPane main_form;

    @FXML
    private CheckBox register_checkBox;

    @FXML
    private TextField register_email;

    @FXML
    private AnchorPane register_form;

    @FXML
    private Hyperlink register_loginHere;

    @FXML
    private PasswordField register_password;

    @FXML
    private TextField register_showPassword;

    @FXML
    private Button register_signupbtn;

    @FXML
    private TextField register_username;
    
    //Database roots
    private Connection connect;
    private PreparedStatement prepare;
    private ResultSet result;
    
    private AlertMessage alert = new AlertMessage();
    
    
     public void loginAccount() {
         
         if (login_username.getText().isEmpty()
                || login_password.getText().isEmpty()) {
            alert.errorMessage("Incorrect Username/Password");
        } else{
             
             String sql = "SELECT * FROM admin WHERE username = ? AND password = ?";

            connect = Database.connectDB();
            
            try {
              
                 if (!login_showPassword.isVisible()) {
                    if (!login_showPassword.getText().equals(login_password.getText())) {
                        login_showPassword.setText(login_password.getText());
                    }
                } else {
                    if (!login_showPassword.getText().equals(login_password.getText())) {
                        login_password.setText(login_showPassword.getText());
                    }
                }
                
                
               prepare = connect.prepareStatement(sql);
                prepare.setString(1, login_username.getText());
                prepare.setString(2, login_password.getText());
                result = prepare.executeQuery();
                
                if (result.next()) {
                    
                     Data.admin_username = login_username.getText();
                     Data.admin_id = Integer.parseInt(result.getString("admin_id"));
                    
                     alert.successMessage("Login Successfully!");
                     Parent root = FXMLLoader.load(getClass().getResource("AdminMainForm.fxml"));
                    Stage stage = new Stage();
                    stage.setTitle("Hospital Management System | Admin Portal");
                    stage.setScene(new Scene(root));
                    stage.show();
                    
                    // TO HIDE MY ADMIN PAGE (LOGIN FORM)
                    login_loginBtn.getScene().getWindow().hide();
                    
                }else{
                     alert.errorMessage("Incorrect Username/Password");
                }
                
            }catch (Exception e) {
                e.printStackTrace();
            }
             
         }
         
     }
     
     public void loginShowPassword() {

        if (login_checkBox.isSelected()) {
            login_showPassword.setText(login_password.getText());
            login_showPassword.setVisible(true);
            login_password.setVisible(false);
        } else {
            login_password.setText(login_showPassword.getText());
            login_showPassword.setVisible(false);
            login_password.setVisible(true);
        }

    }
    
    
    public void registerAccount() {
        if(register_email.getText().isEmpty()
                || register_username.getText().isEmpty()
                || register_password.getText().isEmpty()) {
            alert.errorMessage("Please fill all the blank fields");
            return;
        }
        
        connect = Database.connectDB();
        if(connect == null) {
            alert.errorMessage("Database connection failed");
            return;
        }
        
        try {
            // First sync password fields
            if (!register_showPassword.isVisible()) {
                if (!register_showPassword.getText().equals(register_password.getText())) {
                    register_showPassword.setText(register_password.getText());
                }
            } else {
                if (!register_showPassword.getText().equals(register_password.getText())) {
                    register_password.setText(register_showPassword.getText());
                }
            }

            // Check if username already exists
            String checkUsername = "SELECT * FROM admin WHERE username = ?";
            prepare = connect.prepareStatement(checkUsername);
            prepare.setString(1, register_username.getText());
            result = prepare.executeQuery();
            
            if (result.next()) {
                alert.errorMessage(register_username.getText() + " is already taken!");
                return;
            } 
            
            if (register_password.getText().length() < 8) {
                alert.errorMessage("Invalid Password, at least 8 characters needed");
                return;
            }

            // Insert new admin
            String insertData = "INSERT INTO admin (email, username, password, date, gender) VALUES(?,?,?,?,?)";
            prepare = connect.prepareStatement(insertData);
            
            Date date = new Date();
            java.sql.Date sqlDate = new java.sql.Date(date.getTime());
            
            prepare.setString(1, register_email.getText());
            prepare.setString(2, register_username.getText());
            prepare.setString(3, register_password.getText());
            prepare.setString(4, String.valueOf(sqlDate));
            prepare.setString(5, "Male"); // Default gender

            int rowsAffected = prepare.executeUpdate();
            
            if(rowsAffected > 0) {
                alert.successMessage("Registered Successfully!");
                registerClear();
                login_form.setVisible(true);
                register_form.setVisible(false);
            } else {
                alert.errorMessage("Registration failed");
            }

        } catch (SQLException e) {
            alert.errorMessage("Database error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            alert.errorMessage("An error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if(result != null) result.close();
                if(prepare != null) prepare.close();
                if(connect != null) connect.close();
            } catch(SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void registerClear() {
        register_email.clear();
        register_username.clear();
        register_password.clear();
        register_showPassword.clear();
    }
    
    public void registerShowPassword() {

        if (register_checkBox.isSelected()) {
            register_showPassword.setText(register_password.getText());
            register_showPassword.setVisible(true);
            register_password.setVisible(false);
        } else {
            register_password.setText(register_showPassword.getText());
            register_showPassword.setVisible(false);
            register_password.setVisible(true);
        }

    }
    
    
    
    public void userList() {

        List<String> listU = new ArrayList<>();

        for (String data : Users.user) {
            listU.add(data);
        }

        ObservableList listData = FXCollections.observableList(listU);
        login_user.setItems(listData);
    }
    
    public void switchPage() {
        
       if (login_user.getSelectionModel().getSelectedItem() == "Admin Portal") {
        
           try{
             
               Parent root = FXMLLoader.load(getClass().getResource("FXMLDocument.fxml"));
                Stage stage = new Stage();

                stage.setTitle("Hospital Management System");

                stage.setMinWidth(340);
                stage.setMinHeight(580);

                stage.setScene(new Scene(root));
                stage.show();
               
           }catch (Exception e) {
                e.printStackTrace();
            }
           
       } else if (login_user.getSelectionModel().getSelectedItem() == "Doctor Portal") {
           
           try{
             
               Parent root = FXMLLoader.load(getClass().getResource("DoctorPage.fxml"));
                Stage stage = new Stage();

                stage.setTitle("Hospital Management System");

                stage.setMinWidth(340);
                stage.setMinHeight(580);

                stage.setScene(new Scene(root));
                stage.show();
               
           }catch (Exception e) {
                e.printStackTrace();
            }
           
       } else if (login_user.getSelectionModel().getSelectedItem() == "Patient Portal") {
          try{
             
               Parent root = FXMLLoader.load(getClass().getResource("PatientPage.fxml"));
                Stage stage = new Stage();

                stage.setTitle("Hospital Management System");

                stage.setMinWidth(340);
                stage.setMinHeight(580);

                stage.setScene(new Scene(root));
                stage.show();
               
           }catch (Exception e) {
                e.printStackTrace();
            }
            
       }
        login_user.getScene().getWindow().hide();

    }
    
    
    public void switchForm(ActionEvent event){
        
        if(event.getSource() == login_registerHere){
            login_form.setVisible(false);
            register_form.setVisible(true);
        }else if(event.getSource() == register_loginHere){
             login_form.setVisible(true);
            register_form.setVisible(false);
        }
        
    }
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
         userList();
    }    
    
}
