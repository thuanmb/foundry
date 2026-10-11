package com.thuanbui.foundry.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendOtpRequest(
        @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "{validation.phone.invalid}") String phoneNumber) {
}
