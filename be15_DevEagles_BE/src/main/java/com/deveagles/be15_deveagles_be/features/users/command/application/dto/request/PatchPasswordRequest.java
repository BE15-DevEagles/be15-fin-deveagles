package com.deveagles.be15_deveagles_be.features.users.command.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PatchPasswordRequest(
    @NotBlank(message = "이메일이 존재하지 않습니다.") String email,
    @NotBlank(message = "비밀번호가 존재하지 않습니다.")
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-={}:\";'<>?,./]).{8,}$",
            message = "비밀번호는 영문자, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.")
        String password) {}
