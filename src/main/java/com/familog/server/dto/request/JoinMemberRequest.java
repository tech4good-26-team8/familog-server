package com.familog.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinMemberRequest(
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 30, message = "이름은 30자 이내로 입력해 주세요.")
        String name,

        @NotBlank(message = "초대코드를 입력해 주세요.")
        String inviteCode
) {}
