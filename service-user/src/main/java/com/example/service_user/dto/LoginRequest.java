package com.example.service_user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    @Schema(description = "사용자 ID", example = "user1")
    private String id;

    @Schema(description = "비밀번호", example = "1234")
    private String pwd;
}
