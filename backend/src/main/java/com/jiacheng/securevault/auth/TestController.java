package com.jiacheng.securevault.auth;

import com.jiacheng.securevault.common.ApiResponse;
import com.jiacheng.securevault.user.User;
import com.jiacheng.securevault.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final UserRepository userRepository;

    public TestController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        return ApiResponse.success(Map.of(
                "userId", user != null ? user.getId() : null,
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities()
        ));
    }
}
