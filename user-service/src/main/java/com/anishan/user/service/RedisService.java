package com.anishan.user.service;


import com.anishan.api.entity.LoginUser;

public interface RedisService {

    void cacheUser(LoginUser user);

    LoginUser getUser(Long userId);

    void removeUser(Long userId);

    void cacheEmailCode(String key, String value);

    void getEmailCode(String key);

    void cacheCaptcha(String key, String value);

    String getCaptcha(String key);

}
