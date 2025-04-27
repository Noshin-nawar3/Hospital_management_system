/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hospital_management;

/**
 *
 * @author ASUS
 */
public class Data {
    
    public static Integer admin_id;
    public static String admin_username;
    public static String path;
    
    public static String doctor_id;
    public static String doctor_name;
    
    public static Integer patient_id;
    
    public static String[] gender = {"Male", "Female", "Others"};
    public static String[] status = {"Active", "Inactive", "Confirm"};
      
    public static String[] specialization = {"Allergist", "Dermatologist", "Ophthalmologist", "Gynecologist", "Cardiologist"};
      
    public static Integer temp_PatientID;
    public static String temp_name;
    public static String temp_gender;
    public static Long temp_number;
    public static String temp_address;
    public static String temp_status;
    public static String temp_date;
    public static String temp_path;
    
    public static String temp_doctorID;
    public static String temp_doctorName;
    public static String temp_doctorEmail;
    public static String temp_doctorPassword;
    public static String temp_doctorSpecialized;
    public static String temp_doctorGender;
    public static String temp_doctorMobileNumber;
    public static String temp_doctorImagePath;
    public static String temp_doctorAddress;
    public static String temp_doctorStatus; 
    
    public static String temp_appID;
    public static String temp_appName;
    public static String temp_appGender;
    public static String temp_appMobileNumber;
    public static String temp_appAddress;
    public static String temp_appDescription;
    public static String temp_appDiagnosis;
    public static String temp_appTreatment;
    public static String temp_appDoctor;
    public static String temp_appSpecialized;
    public static String temp_appStatus;
    
    public static void clearDoctorData() {
        doctor_id = null;
        doctor_name = null;
        clearTempData();
    }

    public static void clearPatientData() {
        patient_id = null;
        clearTempData();
    }

    public static void clearAdminData() {
        admin_id = null;
        admin_username = null;
        path = null;
        clearTempData();
    }

    private static void clearTempData() {
        temp_PatientID = null;
        temp_name = null;
        temp_gender = null;
        temp_number = null;
        temp_address = null;
        temp_status = null;
        temp_date = null;
        temp_path = null;
        temp_doctorID = null;
        temp_doctorName = null;
        temp_doctorEmail = null;
        temp_doctorPassword = null;
        temp_doctorSpecialized = null;
        temp_doctorGender = null;
        temp_doctorMobileNumber = null;
        temp_doctorAddress = null;
        temp_doctorStatus = null;
        temp_doctorImagePath = null;
    }

    public static void setCurrentUser(String userType, String userId) {
        if ("doctor".equalsIgnoreCase(userType)) {
            doctor_id = userId;
        } else if ("patient".equalsIgnoreCase(userType)) {
            patient_id = Integer.parseInt(userId);
        } else if ("admin".equalsIgnoreCase(userType)) {
            admin_id = Integer.parseInt(userId);
        }
    }

    public static void validateStatus(String status) {
        boolean valid = false;
        for (String validStatus : status) {
            if (validStatus.equals(status)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    public static void validateGender(String gender) {
        boolean valid = false;
        for (String validGender : Data.gender) {
            if (validGender.equals(gender)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new IllegalArgumentException("Invalid gender: " + gender);
        }
    }

    public static void validateSpecialization(String spec) {
        boolean valid = false;
        for (String validSpec : specialization) {
            if (validSpec.equals(spec)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new IllegalArgumentException("Invalid specialization: " + spec);
        }
    }
}
