package hospital_management;

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupManager {
    private static final String BACKUP_DIR = "backups";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private static final AlertMessage alert = new AlertMessage();
    
    static {
        initializeBackupDirectory();
    }
    
    private static void initializeBackupDirectory() {
        File dir = new File(BACKUP_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    public static boolean performBackup() {
        String backupFile = BACKUP_DIR + File.separator + 
            "backup_" + DATE_FORMAT.format(new Date()) + ".sql";
        
        try {
            // Get database connection details
            String dbName = "hospital";
            String dbUser = "root";
            String dbPass = "";
            
            String executeCmd = String.format(
                "mysqldump -u%s -p%s --add-drop-database -B %s -r %s",
                dbUser, dbPass, dbName, backupFile
            );
            
            Process process = Runtime.getRuntime().exec(executeCmd);
            int processComplete = process.waitFor();
            
            if (processComplete == 0) {
                createZipBackup(backupFile);
                Logger.logAudit("SYSTEM", "BACKUP", "Database backup created successfully");
                alert.successMessage("Backup created successfully");
                return true;
            } else {
                Logger.logError("Backup failed with exit code: " + processComplete, null);
                alert.errorMessage("Could not create the backup");
                return false;
            }
            
        } catch (Exception e) {
            Logger.logError("Error creating backup", e);
            alert.errorMessage("Error creating backup: " + e.getMessage());
            return false;
        }
    }
    
    private static void createZipBackup(String sqlFile) throws IOException {
        String zipFile = sqlFile.replace(".sql", ".zip");
        
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos);
             FileInputStream fis = new FileInputStream(sqlFile)) {
            
            ZipEntry zipEntry = new ZipEntry(new File(sqlFile).getName());
            zos.putNextEntry(zipEntry);
            
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            
            zos.closeEntry();
        }
        
        // Delete the uncompressed SQL file
        new File(sqlFile).delete();
    }
    
    public static boolean restoreBackup(String backupFile) {
        if (!new File(backupFile).exists()) {
            alert.errorMessage("Backup file does not exist");
            return false;
        }
        
        try {
            String dbUser = "root";
            String dbPass = "";
            
            // If it's a zip file, extract it first
            if (backupFile.endsWith(".zip")) {
                backupFile = extractZipBackup(backupFile);
            }
            
            String[] executeCmd = new String[]{
                "mysql",
                "-u" + dbUser,
                "-p" + dbPass,
                "-e",
                "source " + backupFile
            };
            
            Process process = Runtime.getRuntime().exec(executeCmd);
            int processComplete = process.waitFor();
            
            if (processComplete == 0) {
                Logger.logAudit("SYSTEM", "RESTORE", "Database restored successfully");
                alert.successMessage("Database restored successfully");
                return true;
            } else {
                Logger.logError("Restore failed with exit code: " + processComplete, null);
                alert.errorMessage("Could not restore the backup");
                return false;
            }
            
        } catch (Exception e) {
            Logger.logError("Error restoring backup", e);
            alert.errorMessage("Error restoring backup: " + e.getMessage());
            return false;
        }
    }
    
    private static String extractZipBackup(String zipFile) throws IOException {
        String extractedFile = zipFile.replace(".zip", ".sql");
        
        try (FileInputStream fis = new FileInputStream(zipFile);
             java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(fis);
             FileOutputStream fos = new FileOutputStream(extractedFile)) {
            
            zis.getNextEntry();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            
            zis.closeEntry();
        }
        
        return extractedFile;
    }
    
    public static void cleanupOldBackups(int daysToKeep) {
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) return;
        
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);
        
        File[] files = backupDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        Logger.logAudit("SYSTEM", "CLEANUP", 
                            "Deleted old backup file: " + file.getName());
                    }
                }
            }
        }
    }
    
    public static File[] listBackups() {
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) {
            return new File[0];
        }
        return backupDir.listFiles((dir, name) -> 
            name.startsWith("backup_") && (name.endsWith(".sql") || name.endsWith(".zip")));
    }
}