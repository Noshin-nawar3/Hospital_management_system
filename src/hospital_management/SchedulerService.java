package hospital_management;

import java.time.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;

public class SchedulerService {
    private static final ScheduledExecutorService scheduler = 
        Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    private static final Map<String, ScheduledFuture<?>> scheduledTasks = 
        new ConcurrentHashMap<>();
    private static final Set<String> runningTasks = new HashSet<>();
    
    static {
        initializeScheduler();
    }
    
    private static void initializeScheduler() {
        // Schedule daily backup
        scheduleBackup();
        
        // Schedule session cleanup
        scheduleSessionCleanup();
        
        // Schedule appointment reminders check
        scheduleAppointmentReminders();
        
        // Schedule audit log cleanup
        scheduleAuditLogCleanup();
    }
    
    private static void scheduleBackup() {
        String cronExpression = Configuration.getProperty("backup.schedule.cron", "0 0 1 * * ?");
        scheduleTask("daily-backup", () -> {
            if (!isTaskRunning("backup")) {
                markTaskAsRunning("backup");
                try {
                    BackupManager.performBackup();
                } finally {
                    markTaskAsCompleted("backup");
                }
            }
        }, cronExpression);
    }
    
    private static void scheduleSessionCleanup() {
        // Run every hour
        scheduleRecurringTask("session-cleanup", () -> {
            if (!isTaskRunning("session-cleanup")) {
                markTaskAsRunning("session-cleanup");
                try {
                    SecurityManager.cleanupExpiredSessions();
                } finally {
                    markTaskAsCompleted("session-cleanup");
                }
            }
        }, 1, TimeUnit.HOURS);
    }
    
    private static void scheduleAppointmentReminders() {
        // Run every 15 minutes
        scheduleRecurringTask("appointment-reminders", () -> {
            if (!isTaskRunning("appointment-reminders")) {
                markTaskAsRunning("appointment-reminders");
                try {
                    checkAndSendAppointmentReminders();
                } finally {
                    markTaskAsCompleted("appointment-reminders");
                }
            }
        }, 15, TimeUnit.MINUTES);
    }
    
    private static void scheduleAuditLogCleanup() {
        // Run daily at 2 AM
        String cronExpression = "0 0 2 * * ?";
        scheduleTask("audit-cleanup", () -> {
            if (!isTaskRunning("audit-cleanup")) {
                markTaskAsRunning("audit-cleanup");
                try {
                    int retentionDays = Configuration.getIntProperty("audit.retention.days", 365);
                    cleanupAuditLogs(retentionDays);
                } finally {
                    markTaskAsCompleted("audit-cleanup");
                }
            }
        }, cronExpression);
    }
    
    private static void checkAndSendAppointmentReminders() {
        try {
            // Get appointments in the next 24 hours
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime tomorrow = now.plusDays(1);
            
            // Query and process appointments
            // This would typically involve database queries and sending notifications
            // Implementation details would depend on your data access layer
        } catch (Exception e) {
            Logger.logError("Error checking appointment reminders", e);
        }
    }
    
    private static void cleanupAuditLogs(int retentionDays) {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            // Implementation would depend on your audit log storage mechanism
            Logger.logInfo("Cleaning up audit logs older than " + cutoffDate);
        } catch (Exception e) {
            Logger.logError("Error cleaning up audit logs", e);
        }
    }
    
    public static void scheduleTask(String taskId, Runnable task, String cronExpression) {
        try {
            CronTrigger trigger = new CronTrigger(cronExpression);
            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                wrapTask(task), 
                trigger.getNextFireTime(),
                trigger.getPeriod(),
                TimeUnit.MILLISECONDS
            );
            scheduledTasks.put(taskId, future);
        } catch (Exception e) {
            Logger.logError("Error scheduling task: " + taskId, e);
        }
    }
    
    public static void scheduleRecurringTask(String taskId, Runnable task, 
            long interval, TimeUnit unit) {
        try {
            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                wrapTask(task),
                0,
                interval,
                unit
            );
            scheduledTasks.put(taskId, future);
        } catch (Exception e) {
            Logger.logError("Error scheduling recurring task: " + taskId, e);
        }
    }
    
    public static void scheduleOneTimeTask(String taskId, Runnable task, 
            long delay, TimeUnit unit) {
        try {
            ScheduledFuture<?> future = scheduler.schedule(
                wrapTask(task),
                delay,
                unit
            );
            scheduledTasks.put(taskId, future);
        } catch (Exception e) {
            Logger.logError("Error scheduling one-time task: " + taskId, e);
        }
    }
    
    private static Runnable wrapTask(Runnable task) {
        return () -> {
            Thread.currentThread().setName("Scheduler-" + task.hashCode());
            try {
                task.run();
            } catch (Exception e) {
                Logger.logError("Error executing scheduled task", e);
            }
        };
    }
    
    public static void cancelTask(String taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
    }
    
    private static synchronized boolean isTaskRunning(String taskId) {
        return runningTasks.contains(taskId);
    }
    
    private static synchronized void markTaskAsRunning(String taskId) {
        runningTasks.add(taskId);
    }
    
    private static synchronized void markTaskAsCompleted(String taskId) {
        runningTasks.remove(taskId);
    }
    
    private static class CronTrigger {
        private final String expression;
        private final long period;
        
        public CronTrigger(String expression) {
            this.expression = expression;
            this.period = calculatePeriod();
        }
        
        public long getNextFireTime() {
            // Simple implementation - assuming daily schedule at specific hour
            // A full implementation would parse and handle all cron expression components
            try {
                String[] parts = expression.split(" ");
                int hour = Integer.parseInt(parts[2]);
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime nextRun = now.withHour(hour).withMinute(0)
                    .withSecond(0).withNano(0);
                if (nextRun.isBefore(now)) {
                    nextRun = nextRun.plusDays(1);
                }
                return Duration.between(now, nextRun).toMillis();
            } catch (Exception e) {
                Logger.logError("Error calculating next fire time", e);
                return TimeUnit.HOURS.toMillis(1); // Default to 1 hour if parsing fails
            }
        }
        
        public long getPeriod() {
            return period;
        }
        
        private long calculatePeriod() {
            // Simple implementation - assuming daily schedule
            // A full implementation would calculate based on cron expression
            return TimeUnit.DAYS.toMillis(1);
        }
    }
    
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}