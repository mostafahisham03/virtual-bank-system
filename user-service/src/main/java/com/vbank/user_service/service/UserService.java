package com.vbank.user_service.service;

import com.vbank.user_service.dto.UserLoginResponse;
import com.vbank.user_service.dto.UserProfileResponse;
import com.vbank.user_service.dto.UserRegisterRequest;
import com.vbank.user_service.dto.UserRegisterResponse;
import com.vbank.user_service.model.User;

import java.util.UUID;

public interface UserService {

    UserRegisterResponse registerUser(UserRegisterRequest request);

    UserLoginResponse loginUser(String username, String password);

    UserProfileResponse getUserProfile(UUID userId);
}
