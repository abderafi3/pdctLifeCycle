package com.checkmk.pdctLifeCycle.service;

import com.checkmk.pdctLifeCycle.model.HostUser;
import com.checkmk.pdctLifeCycle.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsersService {
    @Autowired
    private UsersRepository usersRepository;

    public List<HostUser> getAllUsers() {
        return usersRepository.findAll();
    }

    public HostUser getUserByEmail(String email) {
        return usersRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }
}
