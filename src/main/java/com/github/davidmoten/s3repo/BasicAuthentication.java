package com.github.davidmoten.s3repo;

import java.util.Base64;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.auth.AUTH;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

public final class BasicAuthentication {
    private final String username;
    private final String password;

    private BasicAuthentication(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public static BasicAuthentication create(String username, String password) {
        return new BasicAuthentication(username, password);
    }

    public static BasicAuthentication from(HttpServletRequest req) {
        String authorization = req.getHeader(AUTH.WWW_AUTH_RESP);
        return from(authorization);
    }

    public static BasicAuthentication from(String authorizationHeader) {
        Preconditions.checkNotNull(authorizationHeader, "authorizationHeader cannot be null");
        String[] fields = authorizationHeader.split(" ");
        String encodedValue = fields[1];
        String decodedValue = new String(Base64.getDecoder().decode(encodedValue), Charsets.UTF_8);
        String[] items = decodedValue.split(":");
        String username = items[0].trim();
        String password = items[1].trim();
        return create(username, password);
    }

    public String password() {
        return password;
    }

    public String username() {
        return username;
    }
}
