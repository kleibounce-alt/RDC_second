package com.klei.user.service;

import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.annotation.Transactional;
import com.klei.common.exception.AuthException;
import com.klei.common.exception.BusinessException;
import com.klei.common.utils.JwtUtil;
import com.klei.common.utils.MailUtil;
import com.klei.common.utils.PasswordUtil;
import com.klei.common.utils.RedisUtil;
import com.klei.user.dto.*;
import com.klei.user.entity.Role;
import com.klei.user.entity.User;
import com.klei.user.entity.enums.UserStatus;
import com.klei.user.mapper.RoleMapper;
import com.klei.user.mapper.RolePermissionMapper;
import com.klei.user.mapper.UserMapper;
import com.klei.user.mapper.UserRoleMapper;
import com.klei.user.vo.LoginVO;
import com.klei.user.vo.UserVO;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class UserService {

    private static final String ADMIN_INVITE_CODE = "RDC-ADMIN-2026";
    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 20;
    private static final int PASSWORD_MIN = 6;
    private static final int PASSWORD_MAX = 32;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    public UserVO findById(Long id) {
        User user = userMapper.findById(id);
        return user == null ? null : toUserVO(user);
    }

    @Transactional
    public long register(RegisterDTO dto) {
        validateUsername(dto.getUsername());
        validatePassword(dto.getPassword());
        checkUsernameNotExists(dto.getUsername());

        long userId = userMapper.insert(
                dto.getUsername(),
                PasswordUtil.hash(dto.getPassword()),
                dto.getNickname(),
                null,
                dto.getEmail(),
                dto.getPhone(),
                UserStatus.NORMAL
        );
        bindRole(userId, "ROLE_USER");
        return userId;
    }

    @Transactional
    public long registerAdmin(RegisterDTO dto) {
        validateUsername(dto.getUsername());
        validatePassword(dto.getPassword());
        if (!ADMIN_INVITE_CODE.equals(dto.getInviteCode())) {
            throw new BusinessException("邀请码错误");
        }
        checkUsernameNotExists(dto.getUsername());

        long userId = userMapper.insert(
                dto.getUsername(),
                PasswordUtil.hash(dto.getPassword()),
                dto.getNickname(),
                null,
                dto.getEmail(),
                dto.getPhone(),
                UserStatus.NORMAL
        );
        bindRole(userId, "ROLE_ADMIN");
        return userId;
    }

    public LoginVO login(LoginDTO dto) {
        if (dto.getCaptcha() == null || dto.getCaptchaUuid() == null) {
            throw new BusinessException("请输入验证码");
        }
        String redisCode = RedisUtil.get("captcha:" + dto.getCaptchaUuid());
        if (redisCode == null || !redisCode.equalsIgnoreCase(dto.getCaptcha())) {
            throw new BusinessException("验证码错误或已过期");
        }
        RedisUtil.del("captcha:" + dto.getCaptchaUuid());

        User user = userMapper.findByUsername(dto.getUsername());
        if (user == null || !PasswordUtil.check(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (UserStatus.BANNED.equals(user.getStatus())) {
            throw new BusinessException("账号已被封禁");
        }

        List<String> roles = getRoles(user.getId());
        List<String> permissions = getPermissions(user.getId());

        Date banEndTime = user.getBanEndTime() == null ? null : Timestamp.valueOf(user.getBanEndTime());
        String accessToken = JwtUtil.createAccessToken(user.getId(), permissions, roles, user.getVipLevel(), banEndTime);

        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setUser(toUserVO(user, roles));

        if (Boolean.TRUE.equals(dto.getRememberMe())) {
            String refreshToken = JwtUtil.createRefreshToken(user.getId());
            RedisUtil.setex("refresh:" + user.getId(), 7 * 24 * 60 * 60, refreshToken);
            vo.setRefreshToken(refreshToken);
        } else {
            vo.setRefreshToken(null);
        }

        return vo;
    }

    public LoginVO refreshToken(String refreshToken) {
        var jwt = JwtUtil.verify(refreshToken);
        Long userId = jwt.getClaim("userId").asLong();

        String stored = RedisUtil.get("refresh:" + userId);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new AuthException(401, "登录已过期，请重新登录");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        List<String> roles = getRoles(userId);
        List<String> permissions = getPermissions(userId);
        Date banEndTime = user.getBanEndTime() == null ? null : Timestamp.valueOf(user.getBanEndTime());
        String newAccessToken = JwtUtil.createAccessToken(userId, permissions, roles, user.getVipLevel(), banEndTime);

        LoginVO vo = new LoginVO();
        vo.setAccessToken(newAccessToken);
        vo.setRefreshToken(refreshToken);
        vo.setUser(toUserVO(user, roles));
        return vo;
    }

    public void logout(Long userId, String accessToken) {
        try {
            var jwt = JwtUtil.verify(accessToken);
            long remain = jwt.getExpiresAt().getTime() - System.currentTimeMillis();
            int ttl = (int) (remain / 1000);
            if (ttl > 0) {
                RedisUtil.setex("blacklist:access:" + userId, ttl, accessToken);
            }
        } catch (Exception ignored) {}
        RedisUtil.del("refresh:" + userId);
    }

    @Transactional
    public void updateProfile(Long userId, UpdateProfileDTO dto) {
        userMapper.updateProfile(dto.getNickname(), dto.getAvatar(), dto.getEmail(), dto.getPhone(), userId);
    }

    @Transactional
    public void updatePassword(Long userId, UpdatePasswordDTO dto) {
        validatePassword(dto.getNewPassword());
        User user = userMapper.findById(userId);
        if (!PasswordUtil.check(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        userMapper.updatePassword(PasswordUtil.hash(dto.getNewPassword()), userId);
    }

    @Transactional
    public void bindEmail(Long userId, String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new BusinessException("邮箱格式不正确");
        }
        User exist = userMapper.findByEmail(email);
        if (exist != null && !exist.getId().equals(userId)) {
            throw new BusinessException("该邮箱已被其他用户绑定");
        }
        userMapper.updateEmail(email, userId);
    }

    public void sendResetCode(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new BusinessException("未绑定邮箱，请先前往个人中心绑定");
        }

        String code = String.valueOf(new Random().nextInt(899999) + 100000);
        RedisUtil.setex("reset:code:" + user.getId(), 600, code);

        MailUtil.sendResetCode(user.getEmail(), code);
    }

    @Transactional
    public void resetPassword(ResetPasswordDTO dto) {
        validatePassword(dto.getNewPassword());
        User user = userMapper.findByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        String redisCode = RedisUtil.get("reset:code:" + user.getId());
        if (redisCode == null || !redisCode.equals(dto.getCode())) {
            throw new BusinessException("验证码错误或已过期");
        }
        userMapper.updatePassword(PasswordUtil.hash(dto.getNewPassword()), user.getId());
        RedisUtil.del("reset:code:" + user.getId());
    }

    private void validateUsername(String username) {
        if (username == null || username.length() < USERNAME_MIN || username.length() > USERNAME_MAX) {
            throw new BusinessException("用户名长度需在 " + USERNAME_MIN + "-" + USERNAME_MAX + " 位");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            throw new BusinessException("密码长度需在 " + PASSWORD_MIN + "-" + PASSWORD_MAX + " 位");
        }
    }

    private void checkUsernameNotExists(String username) {
        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException("用户名已存在");
        }
    }

    private void bindRole(long userId, String roleCode) {
        Role role = roleMapper.findByCode(roleCode);
        if (role != null) {
            userRoleMapper.insert(userId, role.getId());
        }
    }

    private List<String> getRoles(Long userId) {
        return userRoleMapper.findRolesByUserId(userId)
                .stream().map(Role::getCode).collect(Collectors.toList());
    }

    private List<String> getPermissions(Long userId) {
        List<String> list = new ArrayList<>();
        for (Role role : userRoleMapper.findRolesByUserId(userId)) {
            rolePermissionMapper.findPermissionsByRoleId(role.getId())
                    .forEach(p -> list.add(p.getCode()));
        }
        return list;
    }

    private UserVO toUserVO(User user) {
        return toUserVO(user, getRoles(user.getId()));
    }

    private UserVO toUserVO(User user, List<String> roles) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setVipLevel(user.getVipLevel());
        vo.setVipExpireTime(user.getVipExpireTime());
        vo.setStatus(user.getStatus());
        vo.setRoles(roles);
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}