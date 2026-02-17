package com.ravi.mds.shardedsagawallet.service;

import com.ravi.mds.shardedsagawallet.entity.User;
import com.ravi.mds.shardedsagawallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public User createUser(User user) {
        log.info("creating user with email {}",user.getEmail());
        userRepository.findByEmail(user.getEmail())
                .ifPresent(exisitngUser -> {
                    throw  new RuntimeException("User already present with the current email " + user.getEmail());
                });
        User createdUser = userRepository.save(user);
        log.info("user created with id in partition {},{}",createdUser.getId(),(createdUser.getId()%2+1));
        return createdUser;
    }

    @Override
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with user id " + userId));
    }

    @Override
    public User findUserByEmail(String userEmail) {
       return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with user email " + userEmail));
    }
}
