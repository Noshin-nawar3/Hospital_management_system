package hospital_management;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AuditTrail {
    private static final AlertMessage alert = new AlertMessage();

    public static void logDataChange(String tableName, String recordId, String action, 
            String changedBy, Map<String, String> changes) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = Database.getTransactionalConnection();
            
            String sql = "INSERT INTO audit_trail (table_name, record_id, action, changed_by, " +
                        "field_name, old_value, new_value, change_date) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
            
            ps = conn.prepareStatement(sql);
            
            for (Map.Entry<String, String> change : changes.entrySet()) {
                String[] values = change.getValue().split("->", 2);
                String oldValue = values[0];
                String newValue = values.length > 1 ? values[1] : null;
                
                ps.setString(1, tableName);
                ps.setString(2, recordId);
                ps.setString(3, action);
                ps.setString(4, changedBy);
                ps.setString(5, change.getKey());
                ps.setString(6, oldValue);
                ps.setString(7, newValue);
                
                ps.addBatch();
            }
            
            ps.executeBatch();
            conn.commit();
            
            Logger.logAudit(changedBy, action, 
                String.format("Changes made to %s (ID: %s)", tableName, recordId));
            
        } catch (SQLException e) {
            Database.rollbackQuietly(conn);
            Logger.logError("Error logging audit trail", e);
        } finally {
            Database.closeResources(conn, ps, null);
        }
    }
    
    public static List<Map<String, Object>> getAuditHistory(String tableName, String recordId) {
        List<Map<String, Object>> history = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = Database.getConnection();
            
            String sql = "SELECT * FROM audit_trail WHERE table_name = ? AND record_id = ? " +
                        "ORDER BY change_date DESC";
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, tableName);
            ps.setString(2, recordId);
            
            rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("action", rs.getString("action"));
                entry.put("changedBy", rs.getString("changed_by"));
                entry.put("fieldName", rs.getString("field_name"));
                entry.put("oldValue", rs.getString("old_value"));
                entry.put("newValue", rs.getString("new_value"));
                entry.put("changeDate", rs.getTimestamp("change_date"));
                
                history.add(entry);
            }
            
        } catch (SQLException e) {
            Logger.logError("Error retrieving audit history", e);
        } finally {
            Database.closeResources(conn, ps, rs);
        }
        
        return history;
    }
    
    public static void trackPatientChanges(int patientId, Map<String, String> changes, String changedBy) {
        logDataChange("patient", String.valueOf(patientId), "UPDATE", changedBy, changes);
    }
    
    public static void trackDoctorChanges(String doctorId, Map<String, String> changes, String changedBy) {
        logDataChange("doctor", doctorId, "UPDATE", changedBy, changes);
    }
    
    public static void trackAppointmentChanges(int appointmentId, Map<String, String> changes, String changedBy) {
        logDataChange("appointment", String.valueOf(appointmentId), "UPDATE", changedBy, changes);
    }
    
    public static void trackDeletion(String tableName, String recordId, String deletedBy) {
        Map<String, String> changes = new HashMap<>();
        changes.put("status", "active->deleted");
        logDataChange(tableName, recordId, "DELETE", deletedBy, changes);
    }
    
    public static void createAuditTables() {
        Connection conn = null;
        Statement stmt = null;
        
        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();
            
            // Create audit_trail table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS audit_trail (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "table_name VARCHAR(50) NOT NULL, " +
                "record_id VARCHAR(50) NOT NULL, " +
                "action VARCHAR(20) NOT NULL, " +
                "changed_by VARCHAR(50) NOT NULL, " +
                "field_name VARCHAR(50) NOT NULL, " +
                "old_value TEXT, " +
                "new_value TEXT, " +
                "change_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX idx_table_record (table_name, record_id), " +
                "INDEX idx_change_date (change_date)" +
                ")"
            );
            
        } catch (SQLException e) {
            Logger.logError("Error creating audit tables", e);
            alert.errorMessage("Failed to create audit tables");
        } finally {
            Database.closeResources(conn, stmt, null);
        }
    }
    
    public static void purgeOldRecords(int daysToKeep) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = Database.getTransactionalConnection();
            
            String sql = "DELETE FROM audit_trail WHERE change_date < DATE_SUB(NOW(), INTERVAL ? DAY)";
            
            ps = conn.prepareStatement(sql);
            ps.setInt(1, daysToKeep);
            
            int deleted = ps.executeUpdate();
            conn.commit();
            
            Logger.logAudit("SYSTEM", "PURGE", 
                String.format("Purged %d audit trail records older than %d days", deleted, daysToKeep));
            
        } catch (SQLException e) {
            Database.rollbackQuietly(conn);
            Logger.logError("Error purging audit trail", e);
        } finally {
            Database.closeResources(conn, ps, null);
        }
    }
}