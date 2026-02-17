package com.ravi.mds.shardedsagawallet.controller;

import com.ravi.mds.shardedsagawallet.entity.User;
import com.ravi.mds.shardedsagawallet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user){
        User createdUser = userService.createUser(user);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> findUserByUserId(@PathVariable Long id){
        User userById = userService.findUserById(id);
        return new ResponseEntity<>(userById, HttpStatus.OK);
    }

    @GetMapping("em/{email}")
    public ResponseEntity<User> findUserByUserEmail(@PathVariable String email){
        User userById = userService.findUserByEmail(email);
        return new ResponseEntity<>(userById, HttpStatus.OK);
    }
}
