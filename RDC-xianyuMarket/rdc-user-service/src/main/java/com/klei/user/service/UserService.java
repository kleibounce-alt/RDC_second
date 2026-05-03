package com.klei.user.service;

import com.klei.user.dto.*;
import com.klei.user.vo.LoginVO;
import com.klei.user.vo.UserVO;
import java.util.List;

public interface UserService {

    UserVO findById(Long id);

    long register(RegisterDTO dto);

    long registerAdmin(RegisterDTO dto);

    LoginVO login(LoginDTO dto);

    LoginVO refreshToken(String refreshToken);

    void logout(Long userId, String accessToken);

    void updateProfile(Long userId, UpdateProfileDTO dto);

    void updatePassword(Long userId, UpdatePasswordDTO dto);

    void bindEmail(Long userId, String email);

    void sendResetCode(String username);

    void resetPassword(ResetPasswordDTO dto);

    List<UserVO> searchUsers(String keyword);
}