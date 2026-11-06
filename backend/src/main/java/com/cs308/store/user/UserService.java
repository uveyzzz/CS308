package com.cs308.store.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cs308.store.common.NotFoundException;

@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    public User getById(long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    /** Inside a transaction, changes to the entity are saved automatically when the method ends. */
    @Transactional
    public User updateProfile(long id, UpdateProfileRequest request) {
        User user = getById(id);
        if (request.fullName() != null) {
            user.setFullName(request.fullName().trim());
        }
        if (request.homeAddress() != null) {
            user.setHomeAddress(request.homeAddress());
        }
        return user;
    }
}
