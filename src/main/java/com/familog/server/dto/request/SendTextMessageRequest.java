package com.familog.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendTextMessageRequest(
        @NotNull(message = "보낸 사람은 필수입니다.")
        Long senderId,

        @NotBlank(message = "메시지 내용을 입력해 주세요.")
        @Size(max = 500, message = "메시지는 500자 이내로 입력해 주세요.")
        String text
) {}
