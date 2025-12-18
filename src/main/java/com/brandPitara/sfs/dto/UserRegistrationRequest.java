package com.brandPitara.sfs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegistrationRequest {
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    @Size(min = 6, max = 20)
    private String password;
    
    private String phoneNumber;
}