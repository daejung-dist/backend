package shop.daejung.daejungdistbackend.modules.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.daejung.daejungdistbackend.modules.user.service.UserService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;

    @GetMapping("/users")
    public List<Map<String, Object>> getUsers() {
        List<Map<String, Object>> users = new ArrayList<>();
        userService.findAll().forEach(user -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", user.getId());
            dto.put("email", user.getEmail());
            dto.put("name", user.getName());
            dto.put("role", user.getRole().name());
            users.add(dto);
        });
        return users;
    }
}
