package com.housedesign.Service;

// 短信发送通道
public interface SmsService {
    // 向指定手机号发送验证码
    void send(String phone, String code);
}
