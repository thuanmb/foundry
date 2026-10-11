package com.thuanbui.foundry.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EchoJobRequest(
        @NotNull @Size(min = 1, max = 500) String message,
        @Min(0) @Max(30) Integer delaySec) {
}
