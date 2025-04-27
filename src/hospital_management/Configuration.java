package hospital_management;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Configuration {
    private static final String CONFIG_FILE = "config/application.properties";
    private static final Properties properties = new Properties();
    private static final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    static {
        ensureConfigFileExists();
        loadConfiguration();
    }
    
    private static void ensureConfigFileExists() {
        try {
            Path configPath = Paths.get(CONFIG_FILE);
            if (!Files.exists(configPath.getParent())) {
                Files.createDirectories(configPath.getParent());
            }
            if (!Files.exists(configPath)) {
                setDefaultConfiguration();
            }
        } catch (IOException e) {
            Logger.logError("Failed to create configuration file", e);
        }
    }
    
    private static void setDefaultConfiguration() {
        // Database settings
        setProperty("db.url", "jdbc:mysql://localhost:3306/hospital_management");
        setProperty("db.username", "root");
        setProperty("db.password", "");
        setProperty("db.pool.size", "10");
        
        // Email settings
        setProperty("email.host", "smtp.gmail.com");
        setProperty("email.port", "587");
        setProperty("email.username", "your-email@gmail.com");
        setProperty("email.password", "your-app-password");
        setProperty("email.from", "hospital-management@yourdomain.com");
        
        // Security settings
        setProperty("security.session.timeout", "30"); // minutes
        setProperty("security.max.login.attempts", "3");
        setProperty("security.lockout.duration", "15"); // minutes
        setProperty("security.password.expiry", "90"); // days
        
        // Backup settings
        setProperty("backup.directory", "backups");
        setProperty("backup.retention.days", "30");
        setProperty("backup.schedule.cron", "0 0 1 * * ?"); // 1 AM daily
        
        // Notification settings
        setProperty("notification.appointment.reminder", "24"); // hours before
        setProperty("notification.session.expiry.warning", "5"); // minutes before
        
        // Audit settings
        setProperty("audit.enabled", "true");
        setProperty("audit.retention.days", "365");
        
        // Application settings
        setProperty("app.name", "Hospital Management System");
        setProperty("app.version", "1.0.0");
        setProperty("app.locale", "en");
        setProperty("app.timezone", "UTC");
        
        saveConfiguration();
    }
    
    public static void reloadConfiguration() {
        lock.writeLock().lock();
        try {
            cache.clear();
            loadConfiguration();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private static void loadConfiguration() {
        lock.writeLock().lock();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
            cache.clear();
            properties.forEach((k, v) -> cache.put(k.toString(), v.toString()));
        } catch (IOException e) {
            Logger.logError("Failed to load configuration", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public static void saveConfiguration() {
        lock.writeLock().lock();
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "Hospital Management System Configuration");
        } catch (IOException e) {
            Logger.logError("Failed to save configuration", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public static String getProperty(String key) {
        String cachedValue = cache.get(key);
        if (cachedValue != null) {
            return cachedValue;
        }
        
        lock.readLock().lock();
        try {
            String value = properties.getProperty(key);
            if (value != null) {
                cache.put(key, value);
            }
            return value;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public static String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }
    
    public static void setProperty(String key, String value) {
        ValidationResult result = ValidationUtils.validateConfigurationValue(key, value);
        if (result.hasErrors()) {
            throw new IllegalArgumentException("Invalid configuration: " + result.getErrorMessage());
        }
        
        lock.writeLock().lock();
        try {
            properties.setProperty(key, value);
            cache.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public static int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            Logger.logWarning("Invalid integer property: " + key + "=" + value);
            return defaultValue;
        }
    }
    
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    public static long getLongProperty(String key, long defaultValue) {
        String value = getProperty(key);
        try {
            return value != null ? Long.parseLong(value) : defaultValue;
        } catch (NumberFormatException e) {
            Logger.logWarning("Invalid long property: " + key + "=" + value);
            return defaultValue;
        }
    }
    
    public static double getDoubleProperty(String key, double defaultValue) {
        String value = getProperty(key);
        try {
            return value != null ? Double.parseDouble(value) : defaultValue;
        } catch (NumberFormatException e) {
            Logger.logWarning("Invalid double property: " + key + "=" + value);
            return defaultValue;
        }
    }
    
    public static void removeProperty(String key) {
        lock.writeLock().lock();
        try {
            properties.remove(key);
            cache.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public static boolean hasProperty(String key) {
        return getProperty(key) != null;
    }
    
    public static void clearCache() {
        cache.clear();
    }
}