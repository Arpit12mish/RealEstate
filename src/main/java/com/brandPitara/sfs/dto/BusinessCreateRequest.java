// package com.brandPitara.sfs.dto;

// import jakarta.validation.constraints.Email;
// import jakarta.validation.constraints.NotBlank;
// import jakarta.validation.constraints.NotNull;
// import jakarta.validation.constraints.Size;
// import lombok.Data;

// @Data
// public class BusinessCreateRequest {

//     @NotBlank
//     @Size(max = 255)
//     private String name;

//     @Size(max = 20)
//     private String primaryPhone;

//     @Size(max = 20)
//     private String secondaryPhone;

//     @Email
//     @Size(max = 255)
//     private String email;

//     @Size(max = 255)
//     private String website;

//     @Size(max = 255)
//     private String addressLine1;

//     @Size(max = 255)
//     private String addressLine2;

//     @Size(max = 255)
//     private String landmark;

//     @Size(max = 10)
//     private String pincode;

//     @NotNull
//     private Long cityId;

//     private Double latitude;
//     private Double longitude;

//     @NotNull
//     private Long categoryId;

//     private Boolean active = true;
// }
package com.brandPitara.sfs.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
public class BusinessCreateRequest {

    // existing fields...
    private String name;
    private String primaryPhone;
    private String secondaryPhone;
    private String whatsappPhone;
    private String email;
    private String website;
    private String addressLine1;
    private String addressLine2;
    private String landmark;
    private String pincode;
    private Long cityId;
    private Double latitude;
    private Double longitude;
    private Long categoryId;
    private Boolean active;

    // NEW
    private LocalTime openTime;
    private LocalTime closeTime;
    private Integer establishedYear;
    private String highlightBadge;
    private Boolean topRated;
    private Boolean nearAndFast;
}
