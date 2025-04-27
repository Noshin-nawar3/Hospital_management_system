package hospital_management;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class EmailService {
    private static final ExecutorService emailExecutor = Executors.newFixedThreadPool(5);
    private static final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(1);
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_SECONDS = 60;
    private static volatile Session session;
    private static final Object sessionLock = new Object();
    
    static {
        initializeSession();
    }
    
    private static void initializeSession() {
        synchronized (sessionLock) {
            Properties props = new Properties();
            props.put("mail.smtp.host", Configuration.getProperty("email.host"));
            props.put("mail.smtp.port", Configuration.getProperty("email.port"));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                        Configuration.getProperty("email.username"),
                        Configuration.getProperty("email.password")
                    );
                }
            });
        }
    }
    
    public static void sendEmailAsync(String to, String subject, String body) {
        sendEmailAsync(to, subject, body, null);
    }
    
    public static void sendEmailAsync(String to, String subject, String body, 
            Consumer<Exception> errorCallback) {
        emailExecutor.submit(() -> {
            try {
                sendEmailWithRetry(to, subject, body, 0);
            } catch (Exception e) {
                Logger.logError("Failed to send email to " + to, e);
                if (errorCallback != null) {
                    errorCallback.accept(e);
                }
            }
        });
    }
    
    private static void sendEmailWithRetry(String to, String subject, String body, int retryCount) 
            throws MessagingException {
        try {
            MimeMessage message = createMessage(to, subject, body);
            Transport.send(message);
            Logger.logInfo("Email sent successfully to: " + to);
        } catch (MessagingException e) {
            if (retryCount < MAX_RETRIES) {
                Logger.logWarning("Email send failed, scheduling retry " + (retryCount + 1) + 
                    " of " + MAX_RETRIES);
                scheduleRetry(to, subject, body, retryCount);
            } else {
                throw e;
            }
        }
    }
    
    private static void scheduleRetry(String to, String subject, String body, int retryCount) {
        retryExecutor.schedule(() -> {
            try {
                sendEmailWithRetry(to, subject, body, retryCount + 1);
            } catch (Exception e) {
                Logger.logError("Retry failed for email to " + to, e);
            }
        }, RETRY_DELAY_SECONDS * (retryCount + 1), TimeUnit.SECONDS);
    }
    
    private static MimeMessage createMessage(String to, String subject, String body) 
            throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(Configuration.getProperty("email.from")));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setSentDate(new Date());
        message.setText(body);
        return message;
    }
    
    public static void sendAppointmentConfirmation(String to, String patientName, 
            String doctorName, String date, String time) {
        String subject = "Appointment Confirmation";
        String body = String.format(
            "Dear %s,\n\n" +
            "Your appointment has been confirmed with Dr. %s on %s at %s.\n\n" +
            "Please arrive 15 minutes before your scheduled time.\n\n" +
            "If you need to reschedule or cancel, please contact us at least 24 hours in advance.\n\n" +
            "Best regards,\n" +
            Configuration.getProperty("app.name"),
            patientName, doctorName, date, time
        );
        
        sendEmailAsync(to, subject, body);
    }
    
    public static void sendDoctorAssignmentNotification(String to, String doctorName, 
            String patientName, String date, String time) {
        String subject = "New Patient Appointment";
        String body = String.format(
            "Dear Dr. %s,\n\n" +
            "A new appointment has been scheduled for you with patient %s on %s at %s.\n\n" +
            "Please review the patient's information in the system before the appointment.\n\n" +
            "Best regards,\n" +
            Configuration.getProperty("app.name"),
            doctorName, patientName, date, time
        );
        
        sendEmailAsync(to, subject, body);
    }
    
    public static void sendAppointmentReminder(String to, String name, String doctorName, 
            String date, String time) {
        String subject = "Appointment Reminder";
        String body = String.format(
            "Dear %s,\n\n" +
            "This is a reminder of your upcoming appointment with Dr. %s tomorrow at %s.\n\n" +
            "Please remember to:\n" +
            "- Arrive 15 minutes early\n" +
            "- Bring any relevant medical records\n" +
            "- Bring your ID and insurance card\n\n" +
            "If you need to reschedule, please contact us as soon as possible.\n\n" +
            "Best regards,\n" +
            Configuration.getProperty("app.name"),
            name, doctorName, time
        );
        
        sendEmailAsync(to, subject, body);
    }
    
    public static void sendPasswordResetEmail(String to, String name, String resetToken, 
            String resetUrl) {
        String subject = "Password Reset Request";
        String body = String.format(
            "Dear %s,\n\n" +
            "We received a request to reset your password. If you didn't make this request, " +
            "please ignore this email.\n\n" +
            "To reset your password, click the following link:\n" +
            "%s?token=%s\n\n" +
            "This link will expire in 1 hour.\n\n" +
            "Best regards,\n" +
            Configuration.getProperty("app.name"),
            name, resetUrl, resetToken
        );
        
        sendEmailAsync(to, subject, body);
    }
    
    public static void sendAccountLockoutNotification(String to, String name, int lockoutMinutes) {
        String subject = "Account Security Alert";
        String body = String.format(
            "Dear %s,\n\n" +
            "Your account has been temporarily locked due to multiple failed login attempts.\n\n" +
            "You can try logging in again after %d minutes.\n\n" +
            "If you did not attempt to log in, please contact support immediately.\n\n" +
            "Best regards,\n" +
            Configuration.getProperty("app.name"),
            name, lockoutMinutes
        );
        
        sendEmailAsync(to, subject, body);
    }
    
    public static void shutdown() {
        emailExecutor.shutdown();
        retryExecutor.shutdown();
        try {
            if (!emailExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                emailExecutor.shutdownNow();
            }
            if (!retryExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                retryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}