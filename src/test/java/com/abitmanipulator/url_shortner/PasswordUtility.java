package com.abitmanipulator.url_shortner;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordUtility {
    static void main() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        System.out.println(passwordEncoder.encode("secret")); // siva@gmail.com
        System.out.println(passwordEncoder.encode("admin")); // admin@gmail.com
        //VALUES ('admin@gmail.com', 'admin', 'Administrator', 'ROLE_ADMIN'),
        //       ('siva@gmail.com', 'secret', 'Siva', 'ROLE_USER');
    }
}
