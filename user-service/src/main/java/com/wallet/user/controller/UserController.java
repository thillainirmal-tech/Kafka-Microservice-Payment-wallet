package com.wallet.user.controller;

import com.wallet.user.dto.UserDto;
import com.wallet.user.dto.UserProfileDto;
import com.wallet.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/user-service")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/user")
    public Long createUser(@RequestBody @Valid UserDto userDto) throws ExecutionException, InterruptedException {
        return userService.createuser(userDto);
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<UserProfileDto> getUserProfile(@PathVariable Long  id) {
        UserProfileDto userProfileDto = userService.getUserProfile(id);
        return ResponseEntity.ok(userProfileDto);
    }
}
