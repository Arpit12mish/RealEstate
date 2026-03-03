package com.brandPitara.sfs.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DbConnectionDebugRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        var row = jdbcTemplate.queryForMap("""
            select
              current_database() as db,
              current_schema() as schema,
              inet_server_addr() as addr,
              inet_server_port() as port,
              current_user as user,
              version() as version
        """);

        System.out.println("===========================================");
        System.out.println("✅ SPRING DB CONNECTION => " + row);
        System.out.println("===========================================");
    }
}