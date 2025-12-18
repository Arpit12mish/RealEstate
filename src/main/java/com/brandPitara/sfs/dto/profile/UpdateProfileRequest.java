package com.brandPitara.sfs.dto.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 80, message = "Name must be 2-80 chars")
    private String name;

    @Email(message = "Invalid email")
    @Size(max = 120)
    private String email;
}
