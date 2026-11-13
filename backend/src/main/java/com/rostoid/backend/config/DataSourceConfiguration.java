package com.rostoid.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DataSourceConfiguration {
    @Bean
    HikariDataSource dataSource() {
        String raw = System.getenv("DATABASE_URL");
        HikariDataSource ds = new HikariDataSource();
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(1);
        ds.setPoolName("rostoid-db");
        if (!StringUtils.hasText(raw)) {
            ds.setDriverClassName("org.h2.Driver");
            ds.setJdbcUrl("jdbc:h2:mem:rostoid;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            ds.setUsername("sa");
            ds.setPassword("");
            return ds;
        }

        String url = raw.trim();
        if (url.startsWith("mysql://")) {
            String remainder = url.substring("mysql://".length());
            int queryStart = remainder.indexOf('?');
            String query = queryStart < 0 ? null : remainder.substring(queryStart + 1);
            String address = queryStart < 0 ? remainder : remainder.substring(0, queryStart);
            int pathStart = address.indexOf('/');
            String authority = pathStart < 0 ? address : address.substring(0, pathStart);
            String path = pathStart < 0 ? "" : address.substring(pathStart);
            String userInfo = null;
            int at = authority.lastIndexOf('@');
            if (at >= 0) userInfo = authority.substring(0, at);
            if (userInfo != null) {
                String[] credentials = userInfo.split(":", 2);
                ds.setUsername(decode(credentials[0]));
                if (credentials.length > 1) ds.setPassword(decode(credentials[1]));
                authority = authority.substring(at + 1);
            }
            url = "jdbc:mysql://" + authority + path + (query == null ? "" : "?" + query);
        } else if (!url.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("DATABASE_URL deve usar mysql:// ou jdbc:mysql://");
        }
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setJdbcUrl(requireTls(url));
        return ds;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private static String requireTls(String url) {
        String base = url;
        String query = "";
        int q = url.indexOf('?');
        if (q >= 0) {
            base = url.substring(0, q);
            query = url.substring(q + 1);
        }
        String[] params = query.isEmpty() ? new String[0] : query.split("&");
        StringBuilder rebuilt = new StringBuilder();
        for (String param : params) {
            if (param.isEmpty()) continue;
            String name = param.split("=", 2)[0];
            if (name.equalsIgnoreCase("sslMode") || name.equalsIgnoreCase("useSSL") || name.equalsIgnoreCase("ssl")) continue;
            if (!rebuilt.isEmpty()) rebuilt.append('&');
            rebuilt.append(param);
        }
        if (!rebuilt.isEmpty()) rebuilt.append('&');
        rebuilt.append("sslMode=REQUIRED");
        return base + "?" + rebuilt;
    }
}
