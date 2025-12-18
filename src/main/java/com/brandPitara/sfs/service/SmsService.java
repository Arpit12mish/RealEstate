// package com.brandPitara.sfs.service;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;

// import com.twilio.Twilio;
// import com.twilio.rest.api.v2010.account.Message;
// import com.twilio.type.PhoneNumber;

// @Service
// public class SmsService {

//     @Value("${twilio.account.sid}")
//     private String accountSid;

//     @Value("${twilio.auth.token}")
//     private String authToken;

//     @Value("${twilio.phone.number}")
//     private String twilioNumber;

//     public void sendOtpSms(String phoneNumber, String otpCode) {

//         Twilio.init(accountSid, authToken);

//         Message.creator(
//                 new PhoneNumber(phoneNumber),
//                 new PhoneNumber(twilioNumber),
//                 "Your OTP code is: " + otpCode + ". This code will expire in 5 minutes."
//         ).create();
//     }
// }
