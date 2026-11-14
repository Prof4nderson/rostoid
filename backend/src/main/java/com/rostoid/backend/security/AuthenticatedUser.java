package com.rostoid.backend.security;
public final class AuthenticatedUser {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();
    private AuthenticatedUser() {}
    public static void set(String username) { CURRENT.set(username); }
    public static String username() { return CURRENT.get(); }
    public static void clear() { CURRENT.remove(); }
}
