package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.Feedback;
import com.ishine.ishinerest.entity.User;
import com.ishine.ishinerest.pojo.SubmitFeedbackRequest;
import com.ishine.ishinerest.repository.FeedbackRepository;
import com.ishine.ishinerest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;
import java.util.Optional;

/**
 * Service for handling feedback and contact form submissions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@istudy.ie}")
    private String fromEmail;

    @Value("${app.support.email:support@istudy.ie}")
    private String supportEmail;

    /**
     * Submit new feedback
     */
    @Transactional
    public Feedback submitFeedback(SubmitFeedbackRequest request, Long userId) {
        Feedback feedback = new Feedback();
        feedback.setName(request.name());
        feedback.setEmail(request.email());
        feedback.setType(Feedback.FeedbackType.valueOf(request.type().toUpperCase()));
        feedback.setSubject(request.subject());
        feedback.setMessage(request.message());
        feedback.setStatus(Feedback.FeedbackStatus.NEW);

        // Link to user if authenticated
        if (userId != null) {
            Optional<User> user = userRepository.findById(userId);
            user.ifPresent(feedback::setUser);
        }

        Feedback savedFeedback = feedbackRepository.save(feedback);
        log.info("Feedback submitted successfully. ID: {}, Type: {}, Email: {}", 
                savedFeedback.getId(), savedFeedback.getType(), savedFeedback.getEmail());

        // Send email notifications asynchronously
        sendAdminNotification(savedFeedback);
        sendUserConfirmation(savedFeedback);

        return savedFeedback;
    }

    /**
     * Send notification to admin about new feedback
     */
    @Async
    public void sendAdminNotification(Feedback feedback) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(supportEmail);
            helper.setSubject("New " + feedback.getType() + " from " + feedback.getName() + " - " + feedback.getSubject());

            String htmlContent = buildAdminNotificationHtml(feedback);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Admin notification email sent for feedback ID: {}", feedback.getId());

        } catch (Exception e) {
            log.error("Failed to send admin notification email for feedback ID: {}", feedback.getId(), e);
        }
    }

    /**
     * Send confirmation email to user
     */
    @Async
    public void sendUserConfirmation(Feedback feedback) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(feedback.getEmail());
            helper.setSubject("We received your message - iStudy");

            String htmlContent = buildUserConfirmationHtml(feedback);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("User confirmation email sent for feedback ID: {}", feedback.getId());

        } catch (Exception e) {
            log.error("Failed to send user confirmation email for feedback ID: {}", feedback.getId(), e);
        }
    }

    /**
     * Build HTML content for admin notification email
     */
    private String buildAdminNotificationHtml(Feedback feedback) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9;'>" +
                "<div style='background-color: #fff; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>" +
                
                "<h2 style='color: #0D47A1; margin-top: 0;'>New " + feedback.getType() + " Received</h2>" +
                
                "<div style='background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<p style='margin: 5px 0;'><strong>Feedback ID:</strong> " + feedback.getId() + "</p>" +
                "<p style='margin: 5px 0;'><strong>Type:</strong> " + feedback.getType() + "</p>" +
                "<p style='margin: 5px 0;'><strong>Status:</strong> " + feedback.getStatus() + "</p>" +
                "<p style='margin: 5px 0;'><strong>Submitted:</strong> " + feedback.getCreatedAt() + "</p>" +
                "</div>" +
                
                "<h3 style='color: #333; margin-top: 25px;'>Contact Information</h3>" +
                "<p style='margin: 5px 0;'><strong>Name:</strong> " + feedback.getName() + "</p>" +
                "<p style='margin: 5px 0;'><strong>Email:</strong> <a href='mailto:" + feedback.getEmail() + "'>" + feedback.getEmail() + "</a></p>" +
                
                "<h3 style='color: #333; margin-top: 25px;'>Subject</h3>" +
                "<p style='background-color: #f5f5f5; padding: 10px; border-radius: 5px;'>" + feedback.getSubject() + "</p>" +
                
                "<h3 style='color: #333; margin-top: 25px;'>Message</h3>" +
                "<div style='background-color: #f5f5f5; padding: 15px; border-radius: 5px; white-space: pre-wrap;'>" +
                feedback.getMessage() +
                "</div>" +
                
                "<div style='margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; text-align: center; color: #999; font-size: 12px;'>" +
                "<p>This is an automated notification from iStudy</p>" +
                "</div>" +
                
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Build HTML content for user confirmation email
     */
    private String buildUserConfirmationHtml(Feedback feedback) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;'>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f4f4f4; padding: 20px;'>" +
                "<tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>" +
                
                "<!-- Header -->" +
                "<tr><td style='background-color: #0D47A1; padding: 30px; text-align: center;'>" +
                "<h1 style='color: #ffffff; margin: 0; font-size: 28px;'>iStudy</h1>" +
                "</td></tr>" +
                
                "<!-- Content -->" +
                "<tr><td style='padding: 40px 30px;'>" +
                "<h2 style='color: #333333; margin-top: 0;'>Thank You for Contacting Us!</h2>" +
                "<p style='color: #666666; font-size: 16px; line-height: 1.6;'>Hi " + feedback.getName() + ",</p>" +
                "<p style='color: #666666; font-size: 16px; line-height: 1.6;'>" +
                "We've received your message and wanted to let you know that we're on it! Our team will review your " + 
                feedback.getType().toString().toLowerCase() + " and get back to you within 24-48 hours." +
                "</p>" +
                
                "<div style='background-color: #f5f5f5; padding: 20px; border-radius: 5px; margin: 25px 0;'>" +
                "<h3 style='color: #333; margin-top: 0; font-size: 16px;'>Your Message Summary</h3>" +
                "<p style='margin: 5px 0; color: #666;'><strong>Subject:</strong> " + feedback.getSubject() + "</p>" +
                "<p style='margin: 5px 0; color: #666;'><strong>Type:</strong> " + feedback.getType() + "</p>" +
                "<p style='margin: 5px 0; color: #666;'><strong>Reference ID:</strong> #" + feedback.getId() + "</p>" +
                "</div>" +
                
                "<p style='color: #666666; font-size: 16px; line-height: 1.6;'>" +
                "If you have any additional information to add, feel free to reply to this email." +
                "</p>" +
                
                "<p style='color: #999999; font-size: 13px; line-height: 1.6; margin-top: 30px;'>" +
                "Best regards,<br>The iStudy Team" +
                "</p>" +
                "</td></tr>" +
                
                "<!-- Footer -->" +
                "<tr><td style='background-color: #f8f8f8; padding: 20px 30px; text-align: center; border-top: 1px solid #eeeeee;'>" +
                "<p style='color: #999999; font-size: 12px; margin: 0;'>© 2024 iStudy. All rights reserved.</p>" +
                "<p style='color: #999999; font-size: 12px; margin: 10px 0 0 0;'>" +
                "Need help? Contact us at " + supportEmail + "</p>" +
                "</td></tr>" +
                
                "</table>" +
                "</td></tr>" +
                "</table>" +
                "</body>" +
                "</html>";
    }
}

// Made with Bob