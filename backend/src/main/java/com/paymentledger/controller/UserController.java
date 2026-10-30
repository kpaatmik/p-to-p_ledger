package com.paymentledger.controller;

import com.paymentledger.dto.response.UserSearchResponse;
import com.paymentledger.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * User endpoints for Phase 5: search for recipients (T-050).
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET /users/search — T-050: search by email or phone</li>
 * </ul>
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * T-050: Search for a user by email or phone.
     * Returns HTTP 200 with user info (masked contact) or empty response.
     * Never exposes passwords, PINs, or credentials.
     *
     * @param q email or phone number to search for
     * @return HTTP 200 with UserSearchResponse or empty
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchUser(@RequestParam(name = "q") String q) {
        Optional<UserSearchResponse> result = userService.searchUserByEmailOrPhone(q);
        
        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        } else {
            return ResponseEntity.ok(new Object() {
                public String message = "No user found";
            });
        }
    }
}
