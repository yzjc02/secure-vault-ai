package com.jiacheng.securevault.auth;

import com.jiacheng.securevault.audit.enums.AuditAction;
import com.jiacheng.securevault.audit.enums.AuditResourceType;
import com.jiacheng.securevault.audit.service.AuditLogService;
import com.jiacheng.securevault.exception.BusinessException;
import com.jiacheng.securevault.security.JwtService;
import com.jiacheng.securevault.user.User;
import com.jiacheng.securevault.user.UserRepository;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(400, "用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(400, "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_USER");
        user.setEnabled(true);
        try {
            User savedUser = userRepository.save(user);
            auditLogService.recordForUser(savedUser.getId(), AuditAction.REGISTER_SUCCESS,
                    AuditResourceType.AUTH, savedUser.getId(), true, "User registered");
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(400, "用户名或邮箱已存在");
        }
    }

    public TokenResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            auditLogService.recordAnonymous(AuditAction.LOGIN_FAILURE, AuditResourceType.AUTH,
                    null, false, "Login failed");
            throw ex;
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        userRepository.findByUsername(userDetails.getUsername())
                .ifPresent(user -> auditLogService.recordForUser(user.getId(), AuditAction.LOGIN_SUCCESS,
                        AuditResourceType.AUTH, user.getId(), true, "Login succeeded"));
        String token = jwtService.generateToken(userDetails);
        return new TokenResponse(token);
    }
}
