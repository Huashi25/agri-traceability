package com.agri.traceability.config;

import com.agri.traceability.entity.SysUser;
import com.agri.traceability.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    @Autowired
    private SysUserService userService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        List<SysUser> users = userService.list();
        for (SysUser user : users) {
            if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
                String encoded = encoder.encode(user.getPassword());
                user.setPassword(encoded);
                userService.updateById(user);
            }
        }
    }
}
