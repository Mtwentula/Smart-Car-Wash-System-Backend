package za.co.int216d.carwash.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.int216d.carwash.auth.config.AuthAppProperties;
import za.co.int216d.carwash.auth.domain.User;
import za.co.int216d.carwash.auth.dto.LoginRequest;
import za.co.int216d.carwash.auth.dto.LoginResponse;
import za.co.int216d.carwash.auth.dto.RegisterRequest;
import za.co.int216d.carwash.auth.dto.RegisterResponse;
import za.co.int216d.carwash.auth.dto.VerifyEmailRequest;
import za.co.int216d.carwash.auth.repository.UserRepository;
import za.co.int216d.carwash.common.exception.ApiException;
import za.co.int216d.carwash.common.security.JwtService;
import za.co.int216d.carwash.common.security.Role;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthAppProperties props;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User user = users.save(User.builder()
                .email(email)
                .passwordHash(encoder.encode(request.password()))
                .role(Role.CLIENT)
                .active(true)
                .emailVerified(false)
                .build());

        emailService.sendAccountCreated(email);
        String otp = otpService.issue(user.getId());
        emailService.sendOtp(email, otp, props.getOtp().getTtlMinutes());
        log.info("Registered user {} — OTP dispatched", user.getId());
        return new RegisterResponse(user.getId(), "Account created. Please verify your email.");
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim().toLowerCase())
                .orElseThrow(() -> ApiException.badRequest("Invalid email or code"));

        if (user.isEmailVerified()) return;

        if (!otpService.verify(user.getId(), request.otp())) {
            throw ApiException.badRequest("Invalid or expired verification code");
        }
        user.setEmailVerified(true);
        users.save(user);
        emailService.sendWelcome(user.getEmail());
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim().toLowerCase())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        if (!user.isActive()) throw ApiException.forbidden("Account is disabled");
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        String access = jwtService.generateAccessToken(user.getId(), user.getRole());
        String refresh = refreshTokenService.issue(user.getId());
        return new LoginResult(
                new LoginResponse(access, user.getId(), user.getRole(), jwtService.getAccessExpiryMs()),
                refresh);
    }

    @Transactional
    public LoginResult refresh(String refreshCookie) {
        UUID userId = refreshTokenService.rotate(refreshCookie);
        User user = users.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
        if (!user.isActive()) throw ApiException.forbidden("Account is disabled");
        String access = jwtService.generateAccessToken(user.getId(), user.getRole());
        String newRefresh = refreshTokenService.issue(user.getId());
        return new LoginResult(
                new LoginResponse(access, user.getId(), user.getRole(), jwtService.getAccessExpiryMs()),
                newRefresh);
    }

    public record LoginResult(LoginResponse response, String refreshToken) {}
}
