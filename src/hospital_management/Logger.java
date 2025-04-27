package hospital_management;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Logger {
    private static final String LOG_DIRECTORY = "logs";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final ExecutorService logExecutor = Executors.newSingleThreadExecutor();
    private static final Object FILE_LOCK = new Object();
    
    static {
        createLogDirectory();
    }
    
    private static void createLogDirectory() {
        File logDir = new File(LOG_DIRECTORY);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
    }
    
    public static void logError(String message, Throwable error) {
        String timestamp = DATE_FORMAT.format(new Date());
        String errorMessage = String.format("[%s] ERROR: %s%n%s", 
            timestamp, message, getStackTraceAsString(error));
            
        logExecutor.execute(() -> {
            synchronized (FILE_LOCK) {
                try (PrintWriter writer = new PrintWriter(
                        new FileWriter(LOG_DIRECTORY + "/error.log", true))) {
                    writer.println(errorMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        // Also log to database for persistence and querying
        logToDatabase("ERROR", message, error);
    }
    
    public static void logInfo(String message) {
        String timestamp = DATE_FORMAT.format(new Date());
        String logMessage = String.format("[%s] INFO: %s", timestamp, message);
        
        logExecutor.execute(() -> {
            synchronized (FILE_LOCK) {
                try (PrintWriter writer = new PrintWriter(
                        new FileWriter(LOG_DIRECTORY + "/info.log", true))) {
                    writer.println(logMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        // Also log to database
        logToDatabase("INFO", message, null);
    }
    
    public static void logWarning(String message) {
        String timestamp = DATE_FORMAT.format(new Date());
        String logMessage = String.format("[%s] WARNING: %s", timestamp, message);
        
        logExecutor.execute(() -> {
            synchronized (FILE_LOCK) {
                try (PrintWriter writer = new PrintWriter(
                        new FileWriter(LOG_DIRECTORY + "/warning.log", true))) {
                    writer.println(logMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        // Also log to database
        logToDatabase("WARNING", message, null);
    }
    
    public static void logAudit(String userId, String action, String details) {
        String timestamp = DATE_FORMAT.format(new Date());
        String logMessage = String.format("[%s] AUDIT: User %s performed %s: %s", 
            timestamp, userId, action, details);
        
        logExecutor.execute(() -> {
            synchronized (FILE_LOCK) {
                try (PrintWriter writer = new PrintWriter(
                        new FileWriter(LOG_DIRECTORY + "/audit.log", true))) {
                    writer.println(logMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        // Also log to audit_trail table
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = Database.connectDB();
            String sql = "INSERT INTO audit_trail (user_id, action, details, timestamp) " +
                        "VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
            ps = conn.prepareStatement(sql);
            ps.setString(1, userId);
            ps.setString(2, action);
            ps.setString(3, details);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Database.closeResources(conn, ps, null);
        }
    }
    
    public static void logSecurityEvent(String userId, String event, String details) {
        String timestamp = DATE_FORMAT.format(new Date());
        String logMessage = String.format("[%s] SECURITY: User %s - %s: %s", 
            timestamp, userId, event, details);
        
        logExecutor.execute(() -> {
            synchronized (FILE_LOCK) {
                try (PrintWriter writer = new PrintWriter(
                        new FileWriter(LOG_DIRECTORY + "/security.log", true))) {
                    writer.println(logMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        // Also log to database
        logToDatabase("SECURITY", event + ": " + details, null);
    }
    
    private static void logToDatabase(String level, String message, Throwable error) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = Database.connectDB();
            String sql = "INSERT INTO system_logs (level, message, stack_trace, timestamp) " +
                        "VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
            ps = conn.prepareStatement(sql);
            ps.setString(1, level);
            ps.setString(2, message);
            ps.setString(3, error != null ? getStackTraceAsString(error) : null);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Database.closeResources(conn, ps, null);
        }
    }
    
    private static String getStackTraceAsString(Throwable throwable) {
        if (throwable == null) return "";
        
        java.io.StringWriter sw = new java.io.StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
    
    public static void cleanupOldLogs(int retentionDays) {
        logExecutor.execute(() -> {
            try {
                File logDir = new File(LOG_DIRECTORY);
                if (!logDir.exists()) return;
                
                File[] logFiles = logDir.listFiles();
                if (logFiles == null) return;
                
                long cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60L * 60L * 1000L);
                
                for (File file : logFiles) {
                    if (file.lastModified() < cutoffTime) {
                        if (!file.delete()) {
                            System.err.println("Failed to delete old log file: " + file.getName());
                        }
                    }
                }
                
                // Clean up old database logs
                Connection conn = null;
                PreparedStatement ps = null;
                try {
                    conn = Database.connectDB();
                    String sql = "DELETE FROM system_logs WHERE timestamp < DATE_SUB(NOW(), INTERVAL ? DAY)";
                    ps = conn.prepareStatement(sql);
                    ps.setInt(1, retentionDays);
                    ps.executeUpdate();
                } finally {
                    Database.closeResources(conn, ps, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    public static void shutdown() {
        logExecutor.shutdown();
    }
}