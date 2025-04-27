package hospital_management;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Users {
    private static final AlertMessage alert = new AlertMessage();
    public static String[] user = {"Admin Portal", "Doctor Portal", "Patient Portal"};

    public static boolean loginUser(String username, String password, String userType) {
        if (!SecurityManager.checkLoginAttempt(username)) {
            long remainingMinutes = SecurityManager.getRemainingLockoutMinutes(username);
            alert.errorMessage("Account is locked. Try again in " + remainingMinutes + " minutes.");
            return false;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = Database.getConnection();
            String table = userType.toLowerCase();
            String idColumn = table + "_id";
            
            String query = "SELECT * FROM " + table + " WHERE " + 
                (table.equals("doctor") ? "doctor_id" : "username") + " = ? AND password = ?";
            
            ps = conn.prepareStatement(query);
            ps.setString(1, username);
            ps.setString(2, Database.hashPassword(password));
            
            rs = ps.executeQuery();
            
            if (rs.next()) {
                SecurityManager.resetAttempts(username);
                SecurityManager.createSession(username);
                Logger.logAccess(username, "LOGIN", true);
                
                // Set user data based on type
                switch (userType.toLowerCase()) {
                    case "admin":
                        Data.admin_id = rs.getInt(idColumn);
                        Data.admin_username = username;
                        break;
                    case "doctor":
                        Data.doctor_id = rs.getString(idColumn);
                        Data.doctor_name = rs.getString("full_name");
                        break;
                    case "patient":
                        Data.patient_id = rs.getInt(idColumn);
                        break;
                }
                return true;
            } else {
                SecurityManager.recordFailedAttempt(username);
                Logger.logAccess(username, "LOGIN_FAILED", false);
                alert.errorMessage("Invalid username/password");
                return false;
            }
        } catch (SQLException e) {
            Logger.logError("Login error for user: " + username, e);
            alert.showDatabaseError("login", e);
            return false;
        } finally {
            Database.closeResources(conn, ps, rs);
        }
    }

    public static boolean changePassword(String username, String oldPassword, String newPassword, String userType) {
        if (!ValidationUtils.isValidPassword(newPassword)) {
            alert.errorMessage(ValidationUtils.getPasswordRequirements());
            return false;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = Database.getTransactionalConnection();
            String table = userType.toLowerCase();
            String idColumn = table + "_id";
            
            String query = "UPDATE " + table + " SET password = ? WHERE " + 
                (table.equals("doctor") ? "doctor_id" : "username") + " = ? AND password = ?";
            
            ps = conn.prepareStatement(query);
            ps.setString(1, Database.hashPassword(newPassword));
            ps.setString(2, username);
            ps.setString(3, Database.hashPassword(oldPassword));
            
            int updated = ps.executeUpdate();
            if (updated > 0) {
                conn.commit();
                Logger.logSecurityEvent(username, "PASSWORD_CHANGE", "Password changed successfully");
                alert.successMessage("Password changed successfully");
                return true;
            } else {
                conn.rollback();
                alert.errorMessage("Current password is incorrect");
                return false;
            }
        } catch (SQLException e) {
            Database.rollbackQuietly(conn);
            Logger.logError("Password change error for user: " + username, e);
            alert.showDatabaseError("password change", e);
            return false;
        } finally {
            Database.closeResources(conn, ps, null);
        }
    }

    public static void logoutUser(String username) {
        SecurityManager.invalidateSession(username);
        Logger.logAccess(username, "LOGOUT", true);
        
        // Clear user data
        Data.clearAdminData();
        Data.clearDoctorData();
        Data.clearPatientData();
    }

    public static boolean isSessionValid(String username) {
        if (!SecurityManager.isSessionValid(username)) {
            alert.showSessionExpired();
            return false;
        }
        return true;
    }

    public static boolean registerPatient(String patientId, String password, String fullName,
            String mobileNumber, String gender, String address) {
        
        ValidationResult validation = ValidationUtils.validatePatientInput(fullName, mobileNumber, gender, address);
        if (validation.hasErrors()) {
            alert.errorMessage(validation.getErrorMessage());
            return false;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            alert.errorMessage(ValidationUtils.getPasswordRequirements());
            return false;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = Database.getTransactionalConnection();
            
            String query = "INSERT INTO patient (patient_id, password, full_name, mobile_number, " +
                          "gender, address, date, status) VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE, 'Active')";
            
            ps = conn.prepareStatement(query);
            ps.setString(1, patientId);
            ps.setString(2, Database.hashPassword(password));
            ps.setString(3, ValidationUtils.sanitizeInput(fullName));
            ps.setString(4, mobileNumber);
            ps.setString(5, gender);
            ps.setString(6, ValidationUtils.sanitizeInput(address));
            
            ps.executeUpdate();
            conn.commit();
            
            Logger.logAudit("SYSTEM", "PATIENT_REGISTRATION", 
                "New patient registered: " + patientId);
            alert.successMessage("Patient registered successfully");
            return true;
            
        } catch (SQLException e) {
            Database.rollbackQuietly(conn);
            Logger.logError("Error registering patient: " + patientId, e);
            alert.showDatabaseError("patient registration", e);
            return false;
        } finally {
            Database.closeResources(conn, ps, null);
        }
    }

    public static boolean registerDoctor(String doctorId, String password, String fullName,
            String email, String gender, String mobileNumber, String specialization, String address) {
        
        ValidationResult validation = ValidationUtils.validateDoctorInput(fullName, email, 
            mobileNumber, specialization, gender, address);
        if (validation.hasErrors()) {
            alert.errorMessage(validation.getErrorMessage());
            return false;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            alert.errorMessage(ValidationUtils.getPasswordRequirements());
            return false;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = Database.getTransactionalConnection();
            
            String query = "INSERT INTO doctor (doctor_id, password, full_name, email, gender, " +
                          "mobile_number, specialized, address, date, status) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE, 'Active')";
            
            ps = conn.prepareStatement(query);
            ps.setString(1, doctorId);
            ps.setString(2, Database.hashPassword(password));
            ps.setString(3, ValidationUtils.sanitizeInput(fullName));
            ps.setString(4, email);
            ps.setString(5, gender);
            ps.setString(6, mobileNumber);
            ps.setString(7, specialization);
            ps.setString(8, ValidationUtils.sanitizeInput(address));
            
            ps.executeUpdate();
            conn.commit();
            
            Logger.logAudit("SYSTEM", "DOCTOR_REGISTRATION", 
                "New doctor registered: " + doctorId);
            alert.successMessage("Doctor registered successfully");
            return true;
            
        } catch (SQLException e) {
            Database.rollbackQuietly(conn);
            Logger.logError("Error registering doctor: " + doctorId, e);
            alert.showDatabaseError("doctor registration", e);
            return false;
        } finally {
            Database.closeResources(conn, ps, null);
        }
    }

    public static boolean updateUserProfile(String userId, String userType, Map<String, String> updates) {
        if (updates == null || updates.isEmpty()) {
            return false;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = Database.getTransactionalConnection();
            
            StringBuilder query = new StringBuilder("UPDATE " + userType.toLowerCase() + " SET ");
            List<String> setColumns = new ArrayList<>();
            List<String> values = new ArrayList<>();
            
            for (Map.Entry<String, String> entry : updates.entrySet()) {
                String value = ValidationUtils.sanitizeInput(entry.getValue());
                if (value != null && !value.isEmpty()) {
                    setColumns.add(entry.getKey() + " = ?");
                    values.add(value);
                }
            }
            
            if (setColumns.isEmpty()) {
                return false;
            }
            
            query.append(String.join(", ", setColumns));
            query.append(" WHERE ").append(userType.toLowerCase())
                 .append("_id = ?");
            
            ps = conn.prepareStatement(query.toString());
            
            int paramIndex = 1;
            for (String value : values) {
                ps.setString(paramIndex++, value);
            }
            ps.setString(paramIndex, userId);
            
            int updated = ps.executeUpdate();
            if (updated > 0) {
                conn.commit();
                Logger.logAudit(userId, "PROFILE_UPDATE", 
                    "Profile updated for " + userType + ": " + userId);
                alert.successMessage("Profile updated successfully");
                return true;
            } else {
                conn.rollback();
                alert.errorMessage("Profile update failed");
                return false;
            }
            
        } catch (SQLException e) {
            Database.rollbackQuietly(conn);
            Logger.logError("Error updating profile for " + userType + ": " + userId, e);
            alert.showDatabaseError("profile update", e);
            return false;
        } finally {
            Database.closeResources(conn, ps, null);
        }
    }
}
