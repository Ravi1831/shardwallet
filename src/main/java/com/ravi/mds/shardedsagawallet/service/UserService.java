package com.ravi.mds.shardedsagawallet.service;

import com.ravi.mds.shardedsagawallet.entity.User;

public interface UserService {

    User createUser(User user);

    User findUserById(Long userId);

    User findUserByEmail(String userEmail);
}
