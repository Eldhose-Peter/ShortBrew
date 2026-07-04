package com.epproject.ShortBrew.controller;

import com.epproject.ShortBrew.controller.dto.AuthResponse;
import com.epproject.ShortBrew.controller.dto.LoginRequest;
import com.epproject.ShortBrew.controller.dto.SignupRequest;
import com.epproject.ShortBrew.controller.dto.UserProfileResponse;
import com.epproject.ShortBrew.model.User;
import com.epproject.ShortBrew.security.CurrentUser;
import com.epproject.ShortBrew.security.JwtService;
import com.epproject.ShortBrew.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        User user = userService.registerUser(request.email(), request.password(), request.fullName());
        String accessToken = jwtService.generateAccessToken(user.id());
        String refreshToken = jwtService.generateRefreshToken(user.id());
        AuthResponse response = new AuthResponse(accessToken, refreshToken, "bearer");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        User user = userService.authenticateUser(request.email(), request.password());
        String accessToken = jwtService.generateAccessToken(user.id());
        String refreshToken = jwtService.generateRefreshToken(user.id());
        AuthResponse response = new AuthResponse(accessToken, refreshToken, "bearer");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(@CurrentUser User currentUser) {
        UserProfileResponse response = new UserProfileResponse(
            currentUser.id(),
            currentUser.email(),
            currentUser.fullName(),
            currentUser.isActive(),
            currentUser.createdAt()
        );
        return ResponseEntity.ok(response);
    }
}
