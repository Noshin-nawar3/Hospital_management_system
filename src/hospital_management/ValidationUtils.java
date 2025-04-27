package hospital_management;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class ValidationUtils {
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^\\d{10}$");
    private static final Pattern NAME_PATTERN = 
        Pattern.compile("^[\\p{L} .'-]+$");
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$");
    
    public static ValidationResult validateLogin(String username, String password) {
        ValidationResult result = new ValidationResult();
        
        if (username == null || username.trim().isEmpty()) {
            result.addError("Username cannot be empty");
        }
        
        if (password == null || password.trim().isEmpty()) {
            result.addError("Password cannot be empty");
        }
        
        return result;
    }
    
    public static ValidationResult validatePatientInput(
            String fullName, String email, String mobileNumber, 
            String gender, String address) {
        ValidationResult result = new ValidationResult();
        
        if (fullName == null || fullName.trim().isEmpty()) {
            result.addError("Full name is required");
        } else if (!NAME_PATTERN.matcher(fullName).matches()) {
            result.addError("Invalid name format");
        }
        
        if (email == null || email.trim().isEmpty()) {
            result.addError("Email is required");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            result.addError("Invalid email format");
        }
        
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            result.addError("Mobile number is required");
        } else if (!PHONE_PATTERN.matcher(mobileNumber).matches()) {
            result.addError("Invalid mobile number format (10 digits required)");
        }
        
        if (gender == null || gender.trim().isEmpty()) {
            result.addError("Gender must be selected");
        }
        
        if (address == null || address.trim().isEmpty()) {
            result.addError("Address is required");
        }
        
        return result;
    }
    
    public static ValidationResult validateDoctorInput(
            String fullName, String email, String mobileNumber, 
            String specialized, String gender, String address) {
        ValidationResult result = validatePatientInput(fullName, email, mobileNumber, gender, address);
        
        if (specialized == null || specialized.trim().isEmpty()) {
            result.addError("Specialization must be selected");
        }
        
        return result;
    }
    
    public static ValidationResult validatePassword(String password, String confirmPassword) {
        ValidationResult result = new ValidationResult();
        
        if (password == null || password.trim().isEmpty()) {
            result.addError("Password is required");
        } else if (!PASSWORD_PATTERN.matcher(password).matches()) {
            result.addError("Password must contain at least 8 characters, including uppercase, " +
                "lowercase, numbers and special characters");
        }
        
        if (confirmPassword == null || !confirmPassword.equals(password)) {
            result.addError("Passwords do not match");
        }
        
        return result;
    }
    
    public static ValidationResult validateAppointmentInput(
            String description, String scheduleDate, String doctorId, String patientId) {
        ValidationResult result = new ValidationResult();
        
        if (description == null || description.trim().isEmpty()) {
            result.addError("Description is required");
        }
        
        if (scheduleDate == null || scheduleDate.trim().isEmpty()) {
            result.addError("Schedule date is required");
        } else {
            try {
                LocalDate date = LocalDate.parse(scheduleDate);
                if (date.isBefore(LocalDate.now())) {
                    result.addError("Schedule date cannot be in the past");
                }
            } catch (Exception e) {
                result.addError("Invalid date format");
            }
        }
        
        if (doctorId == null || doctorId.trim().isEmpty()) {
            result.addError("Doctor must be selected");
        }
        
        if (patientId == null || patientId.trim().isEmpty()) {
            result.addError("Patient ID is required");
        }
        
        return result;
    }
    
    public static ValidationResult validatePayment(
            String amount, String paymentMethod, String description) {
        ValidationResult result = new ValidationResult();
        
        if (amount == null || amount.trim().isEmpty()) {
            result.addError("Amount is required");
        } else {
            try {
                double value = Double.parseDouble(amount);
                if (value <= 0) {
                    result.addError("Amount must be greater than zero");
                }
            } catch (NumberFormatException e) {
                result.addError("Invalid amount format");
            }
        }
        
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            result.addError("Payment method must be selected");
        }
        
        if (description == null || description.trim().isEmpty()) {
            result.addError("Description is required");
        }
        
        return result;
    }
    
    public static ValidationResult validateMedicalRecord(
            String diagnosis, String treatment, String notes) {
        ValidationResult result = new ValidationResult();
        
        if (diagnosis == null || diagnosis.trim().isEmpty()) {
            result.addError("Diagnosis is required");
        }
        
        if (treatment == null || treatment.trim().isEmpty()) {
            result.addError("Treatment is required");
        }
        
        if (notes == null || notes.trim().isEmpty()) {
            result.addError("Notes are required");
        }
        
        return result;
    }
    
    public static ValidationResult validateDateRange(String startDate, String endDate) {
        ValidationResult result = new ValidationResult();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        
        try {
            if (startDate == null || startDate.trim().isEmpty()) {
                result.addError("Start date is required");
            } else {
                LocalDate start = LocalDate.parse(startDate, formatter);
            }
            
            if (endDate == null || endDate.trim().isEmpty()) {
                result.addError("End date is required");
            } else {
                LocalDate end = LocalDate.parse(endDate, formatter);
                LocalDate start = LocalDate.parse(startDate, formatter);
                
                if (end.isBefore(start)) {
                    result.addError("End date cannot be before start date");
                }
            }
        } catch (Exception e) {
            result.addError("Invalid date format");
        }
        
        return result;
    }
    
    public static ValidationResult validateBackupPath(String path) {
        ValidationResult result = new ValidationResult();
        
        if (path == null || path.trim().isEmpty()) {
            result.addError("Backup path is required");
        } else {
            try {
                java.nio.file.Path backupPath = java.nio.file.Paths.get(path);
                if (!java.nio.file.Files.exists(backupPath.getParent())) {
                    result.addError("Backup directory does not exist");
                }
                if (!java.nio.file.Files.isWritable(backupPath.getParent())) {
                    result.addError("Backup directory is not writable");
                }
            } catch (Exception e) {
                result.addError("Invalid backup path");
            }
        }
        
        return result;
    }
    
    public static ValidationResult validateConfigurationValue(String key, String value) {
        ValidationResult result = new ValidationResult();
        
        if (key == null || key.trim().isEmpty()) {
            result.addError("Configuration key is required");
        }
        
        if (value == null || value.trim().isEmpty()) {
            result.addError("Configuration value is required");
        }
        
        // Validate specific configuration types
        if (key != null) {
            switch (key) {
                case "email.port":
                    try {
                        int port = Integer.parseInt(value);
                        if (port <= 0 || port > 65535) {
                            result.addError("Port must be between 1 and 65535");
                        }
                    } catch (NumberFormatException e) {
                        result.addError("Port must be a number");
                    }
                    break;
                    
                case "backup.retention.days":
                case "audit.retention.days":
                    try {
                        int days = Integer.parseInt(value);
                        if (days <= 0) {
                            result.addError("Retention days must be positive");
                        }
                    } catch (NumberFormatException e) {
                        result.addError("Retention days must be a number");
                    }
                    break;
                    
                case "email.host":
                    if (!value.matches("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                        result.addError("Invalid email host format");
                    }
                    break;
                    
                case "email.from":
                    if (!EMAIL_PATTERN.matcher(value).matches()) {
                        result.addError("Invalid email address format");
                    }
                    break;
            }
        }
        
        return result;
    }
    
    public static class ValidationResult {
        private final java.util.Map<String, String> errors = new java.util.HashMap<>();
        
        public void addError(String message) {
            errors.put(String.valueOf(errors.size() + 1), message);
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public java.util.Map<String, String> getErrors() {
            return java.util.Collections.unmodifiableMap(errors);
        }
        
        public String getErrorMessage() {
            return String.join("\n", errors.values());
        }
    }
}