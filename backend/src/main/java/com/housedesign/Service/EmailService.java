package com.housedesign.Service;

// 邮件发送通道
public interface EmailService {
    // 向目标邮箱发送验证码
    void send(String target, String code);
}
