package com.example.service_user.controller;

import com.example.service_user.dto.ApiResponse;
import com.example.service_user.dto.LoginRequest;
import com.example.service_user.dto.RegisterRequest;
import com.example.service_user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody RegisterRequest request) {
        userService.register(request);
        return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다."));
    }

    @Operation(summary = "로그인", description = "사용자 인증 후 JWT를 HttpOnly Cookie로 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        String token = userService.login(request);

        Cookie cookie = new Cookie("accessToken", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(cookie);

        return ResponseEntity.ok(ApiResponse.success("로그인이 완료되었습니다."));
    }
}
