// package com.brandPitara.sfs.service;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.mail.SimpleMailMessage;
// import org.springframework.mail.javamail.JavaMailSender;
// import org.springframework.stereotype.Service;

// import lombok.RequiredArgsConstructor;

// @Service
// @RequiredArgsConstructor
// public class EmailService {

//     private final JavaMailSender mailSender;

//     @Value("${spring.mail.username}")
//     private String fromEmail;

//     public void sendOtpEmail(String toEmail, String otpCode) {
//         SimpleMailMessage message = new SimpleMailMessage();
//         message.setFrom(fromEmail);
//         message.setTo(toEmail);
//         message.setSubject("Your OTP Code");
//         message.setText("Your OTP code is: " + otpCode + "\nThis code will expire in 5 minutes.");

//         mailSender.send(message);
//     }
// }
