package com.ecommerce;

import com.ecommerce.domain.UserEntity;
import com.ecommerce.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class EcommerceBffApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceBffApplication.class, args);
    }

    /**
     * Garante que os usuários de demonstração semeados pelo Flyway possuam hash BCrypt válido para "Senha@123".
     */
    @Bean
    CommandLineRunner syncDemoPasswords(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            for (UserEntity user : userRepository.findAll()) {
                if (!passwordEncoder.matches("Senha@123", user.getPasswordHash())) {
                    user.setPasswordHash(passwordEncoder.encode("Senha@123"));
                    userRepository.save(user);
                }
            }
        };
    }
}

