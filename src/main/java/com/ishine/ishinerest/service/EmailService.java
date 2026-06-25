package com.ishine.ishinerest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Service for sending emails
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    @Value("${spring.mail.username:noreply@istudy.com}")
    private String fromEmail;
    
    /**
     * Send password reset email asynchronously
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String userName, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset Your iStudy Password");
            
            String resetLink = frontendUrl + "/reset-password?token=" + token;
            String htmlContent = buildPasswordResetEmailHtml(userName, resetLink);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            // Log error but don't throw - email failure shouldn't break the flow
            log.error("Failed to send password reset email to: {}", toEmail, e);
        }
    }
    
    /**
     * Send email verification email asynchronously
     */
    @Async
    public void sendVerificationEmail(String toEmail, String userName, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your iStudy Email Address");
            
            String verificationLink = frontendUrl + "/verify-email?token=" + token;
            String htmlContent = buildVerificationEmailHtml(userName, verificationLink);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            // Log error but don't throw - email failure shouldn't break the flow
            log.error("Failed to send verification email to: {}", toEmail, e);
        }
    }
    
    /**
     * Build HTML content for email verification
     */
    private String buildVerificationEmailHtml(String userName, String verificationLink) {
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "<meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
               "</head>" +
               "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;'>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f4f4f4; padding: 20px;'>" +
               "<tr>" +
               "<td align='center'>" +
               "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>" +
               
               "<!-- Header -->" +
               "<tr>" +
               "<td style='background-color: #0D47A1; padding: 30px; text-align: center;'>" +
               "<h1 style='color: #ffffff; margin: 0; font-size: 28px;'>iStudy</h1>" +
               "</td>" +
               "</tr>" +
               
               "<!-- Content -->" +
               "<tr>" +
               "<td style='padding: 40px 30px;'>" +
               "<h2 style='color: #333333; margin-top: 0;'>Welcome to iStudy!</h2>" +
               "<p style='color: #666666; font-size: 16px; line-height: 1.6;'>" +
               "Hi " + userName + "," +
               "</p>" +
               "<p style='color: #666666; font-size: 16px; line-height: 1.6;'>" +
               "Thank you for signing up! Please verify your email address to complete your registration and access all features:" +
               "</p>" +
               
               "<!-- CTA Button -->" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
               "<tr>" +
               "<td align='center'>" +
               "<a href='" + verificationLink + "' style='display: inline-block; padding: 15px 40px; background-color: #4CAF50; color: #ffffff; text-decoration: none; border-radius: 5px; font-size: 16px; font-weight: bold;'>" +
               "Verify Email Address" +
               "</a>" +
               "</td>" +
               "</tr>" +
               "</table>" +
               
               "<p style='color: #666666; font-size: 14px; line-height: 1.6;'>" +
               "Or copy and paste this link into your browser:" +
               "</p>" +
               "<p style='color: #0D47A1; font-size: 14px; word-break: break-all;'>" +
               verificationLink +
               "</p>" +
               
               "<p style='color: #999999; font-size: 13px; line-height: 1.6; margin-top: 30px;'>" +
               "This link will expire in 24 hours. If you didn't create an account, you can safely ignore this email." +
               "</p>" +
               "</td>" +
               "</tr>" +
               
               "<!-- Footer -->" +
               "<tr>" +
               "<td style='background-color: #f8f8f8; padding: 20px 30px; text-align: center; border-top: 1px solid #eeeeee;'>" +
               "<p style='color: #999999; font-size: 12px; margin: 0;'>" +
               "© 2024 iStudy. All rights reserved." +
               "</p>" +
               "<p style='color: #999999; font-size: 12px; margin: 10px 0 0 0;'>" +
               "Need help? Contact us at support@istudy.com" +
               "</p>" +
               "</td>" +
               "</tr>" +
               
               "</table>" +
               "</td>" +
               "</tr>" +
               "</table>" +
               "</body>" +
               "</html>";
    }
    
    /**
     * Build HTML content for password reset email
     */
    private String buildPasswordResetEmailHtml(String userName, String resetLink) {
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "<meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
               "</head>" +
               "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;'>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f4f4f4; padding: 20px;'>" +
               "<tr>" +
               "<td align='center'>" +
               "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>" +
               
               "<!-- Header -->" +
               "<tr>" +
               "<td style='background-color: #0D47A1; padding: 30px; text-align: center;'>" +
               "<h1 style='color: #ffffff; margin: 0; font-size: 28px;'>iStudy</h1>" +
               "</td>" +
               "</tr>" +
               
               "<!-- Content -->" +
               "<tr>" +
               "<td style='padding: 40px 30px;'>" +
               "<h2 style='color: #333333; margin-top: 0;'>Reset Your Password</h2>" +
               "<p style='color: #666666; font-size: 16px; line-height: 1.6;'>" +
               "Hi " + userName + "," +
               "</p>" +
               "<p style='color: #666666; font-size: 16px; line-height: 1.6;'>" +
               "We received a request to reset your password. Click the button below to create a new password:" +
               "</p>" +
               
               "<!-- CTA Button -->" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
               "<tr>" +
               "<td align='center'>" +
               "<a href='" + resetLink + "' style='display: inline-block; padding: 15px 40px; background-color: #0D47A1; color: #ffffff; text-decoration: none; border-radius: 5px; font-size: 16px; font-weight: bold;'>" +
               "Reset Password" +
               "</a>" +
               "</td>" +
               "</tr>" +
               "</table>" +
               
               "<p style='color: #666666; font-size: 14px; line-height: 1.6;'>" +
               "Or copy and paste this link into your browser:" +
               "</p>" +
               "<p style='color: #0D47A1; font-size: 14px; word-break: break-all;'>" +
               resetLink +
               "</p>" +
               
               "<p style='color: #999999; font-size: 13px; line-height: 1.6; margin-top: 30px;'>" +
               "This link will expire in 1 hour. If you didn't request a password reset, you can safely ignore this email." +
               "</p>" +
               "</td>" +
               "</tr>" +
               
               "<!-- Footer -->" +
               "<tr>" +
               "<td style='background-color: #f8f8f8; padding: 20px 30px; text-align: center; border-top: 1px solid #eeeeee;'>" +
               "<p style='color: #999999; font-size: 12px; margin: 0;'>" +
               "© 2024 iStudy. All rights reserved." +
               "</p>" +
               "<p style='color: #999999; font-size: 12px; margin: 10px 0 0 0;'>" +
               "Need help? Contact us at support@istudy.com" +
               "</p>" +
               "</td>" +
               "</tr>" +
               
               "</table>" +
               "</td>" +
               "</tr>" +
               "</table>" +
               "</body>" +
               "</html>";
    }
}

// Made with Bob
