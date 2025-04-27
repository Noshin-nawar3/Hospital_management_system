package hospital_management;

import java.security.*;
import java.util.*;
import java.time.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SecurityManager {
    private static final Map<String, Session> activeSessions = new ConcurrentHashMap<>();
    private static final int SESSION_TIMEOUT_MINUTES = 30;
    private static final String SALT = Configuration.getProperty("security.salt", "HospitalMgmtSalt");
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256;
    
    public static class Session {
        private final String sessionId;
        private final String username;
        private final String userRole;
        private LocalDateTime lastAccess;
        private final Map<String, Object> attributes;
        
        public Session(String username, String userRole) {
            this.sessionId = generateSessionId();
            this.username = username;
            this.userRole = userRole;
            this.lastAccess = LocalDateTime.now();
            this.attributes = new ConcurrentHashMap<>();
        }
        
        public void updateLastAccess() {
            this.lastAccess = LocalDateTime.now();
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(
                lastAccess.plusMinutes(SESSION_TIMEOUT_MINUTES));
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getUsername() { return username; }
        public String getUserRole() { return userRole; }
        public LocalDateTime getLastAccess() { return lastAccess; }
        
        public void setAttribute(String key, Object value) {
            attributes.put(key, value);
        }
        
        public Object getAttribute(String key) {
            return attributes.get(key);
        }
        
        public void removeAttribute(String key) {
            attributes.remove(key);
        }
    }
    
    public static String login(String username, String password) throws SecurityException {
        try {
            // Verify credentials against database
            if (verifyCredentials(username, password)) {
                String userRole = getUserRole(username);
                Session session = new Session(username, userRole);
                activeSessions.put(session.getSessionId(), session);
                
                // Log successful login
                Logger.logInfo("User logged in successfully: " + username);
                AuditTrail.log("LOGIN", username, "Successful login");
                
                return session.getSessionId();
            } else {
                // Log failed login attempt
                Logger.logWarning("Failed login attempt for user: " + username);
                AuditTrail.log("LOGIN_FAILED", username, "Invalid credentials");
                throw new SecurityException("Invalid credentials");
            }
        } catch (Exception e) {
            Logger.logError("Error during login", e);
            throw new SecurityException("Login failed: " + e.getMessage());
        }
    }
    
    public static void logout(String sessionId) {
        Session session = activeSessions.remove(sessionId);
        if (session != null) {
            AuditTrail.log("LOGOUT", session.getUsername(), "User logged out");
        }
    }
    
    public static Session validateSession(String sessionId) throws SecurityException {
        Session session = activeSessions.get(sessionId);
        if (session == null) {
            throw new SecurityException("Invalid session");
        }
        
        if (session.isExpired()) {
            activeSessions.remove(sessionId);
            throw new SecurityException("Session expired");
        }
        
        session.updateLastAccess();
        return session;
    }
    
    public static void cleanupExpiredSessions() {
        List<String> expiredSessions = activeSessions.entrySet().stream()
            .filter(entry -> entry.getValue().isExpired())
            .map(Map.Entry::getKey)
            .toList();
        
        expiredSessions.forEach(sessionId -> {
            Session session = activeSessions.remove(sessionId);
            if (session != null) {
                AuditTrail.log("SESSION_EXPIRED", session.getUsername(), 
                    "Session expired and cleaned up");
            }
        });
    }
    
    private static boolean verifyCredentials(String username, String password) {
        try {
            // Get stored hash from database
            String storedHash = getStoredHash(username);
            if (storedHash == null) {
                return false;
            }
            
            // Hash the provided password and compare
            String hashedPassword = hashPassword(password);
            return storedHash.equals(hashedPassword);
        } catch (Exception e) {
            Logger.logError("Error verifying credentials", e);
            return false;
        }
    }
    
    public static String hashPassword(String password) {
        try {
            byte[] salt = SALT.getBytes(StandardCharsets.UTF_8);
            
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATION_COUNT,
                KEY_LENGTH
            );
            
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            Logger.logError("Error hashing password", e);
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    private static String getStoredHash(String username) {
        try {
            // Query database for stored hash
            // Implementation depends on your database structure
            return Database.getUserPasswordHash(username);
        } catch (Exception e) {
            Logger.logError("Error retrieving stored hash", e);
            return null;
        }
    }
    
    private static String getUserRole(String username) {
        try {
            // Query database for user role
            // Implementation depends on your database structure
            return Database.getUserRole(username);
        } catch (Exception e) {
            Logger.logError("Error retrieving user role", e);
            return "GUEST";
        }
    }
    
    private static String generateSessionId() {
        return UUID.randomUUID().toString();
    }
    
    public static boolean hasPermission(String sessionId, String permission) {
        try {
            Session session = validateSession(sessionId);
            return checkPermission(session.getUserRole(), permission);
        } catch (SecurityException e) {
            return false;
        }
    }
    
    private static boolean checkPermission(String userRole, String permission) {
        // Implement role-based permission checking
        // This could be expanded based on your requirements
        switch (userRole.toUpperCase()) {
            case "ADMIN":
                return true;
            case "DOCTOR":
                return permission.startsWith("DOCTOR_") || 
                       permission.startsWith("VIEW_");
            case "PATIENT":
                return permission.startsWith("PATIENT_") || 
                       permission.startsWith("VIEW_PERSONAL_");
            default:
                return false;
        }
    }
    
    public static String getCurrentUser(String sessionId) {
        try {
            Session session = validateSession(sessionId);
            return session.getUsername();
        } catch (SecurityException e) {
            return null;
        }
    }
    
    public static String getCurrentUserRole(String sessionId) {
        try {
            Session session = validateSession(sessionId);
            return session.getUserRole();
        } catch (SecurityException e) {
            return null;
        }
    }
}