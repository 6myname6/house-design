package com.housedesign.Service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.housedesign.Service.SmsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sms.provider", matchIfMissing = true, havingValue = "mock")
public class MockSmsServiceImpl implements SmsService {
    // 生成手机号掩码
    private String makeMaskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "**";
        }
        String maskPhone = phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
        return maskPhone;
    }

    @Override
    public void send(String phone, String code) {
        log.info("已向手机号{}发送验证码", makeMaskPhone(phone));
    }

}
