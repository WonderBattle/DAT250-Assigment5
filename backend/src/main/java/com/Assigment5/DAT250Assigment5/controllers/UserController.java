package com.Assigment5.DAT250Assigment5.controllers;

import com.Assigment5.DAT250Assigment5.PollManager;
import com.Assigment5.DAT250Assigment5.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/users")
@CrossOrigin
@Tag(name = "Users", description = "User management APIs") // Step 6: API Documentation
public class UserController {

    @Autowired
    private PollManager pollManager;

    @Operation(summary = "Get all users", description = "Returns a list of all registered users") // Step 6: API Documentation
    @GetMapping
    public List<User> getAllUsers() {
        return pollManager.getAllUsers();
    }

    @Operation(summary = "Create a new user", description = "Creates a new user account and returns the created user") // Step 6: API Documentation
    @PostMapping
    public User createUser(@RequestBody User user) {
        return pollManager.createUser(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        pollManager.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
