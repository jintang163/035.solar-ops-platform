package com.solar.ops.analysis.service;

import com.solar.ops.admin.entity.SysUser;

import java.util.List;
import java.util.Map;

public interface AppPushService {

    boolean pushToUser(Long userId, String title, String content, Map<String, String> extras);

    boolean pushToUsers(List<SysUser> users, String title, String content, Map<String, String> extras);

    boolean pushToPhone(String phone, String title, String content, Map<String, String> extras);

    boolean pushToPhones(List<String> phones, String title, String content, Map<String, String> extras);
}
