package com.housedesign.Service.impl;

import org.springframework.stereotype.Service;

import com.housedesign.Service.SmsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MockSmsServiceImpl implements SmsService {

    @Override
    public void send(String phone, String code) {
        log.info("验证码：{}", code);
    }

}
