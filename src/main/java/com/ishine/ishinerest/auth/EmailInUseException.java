package com.ishine.ishinerest.auth;

public class EmailInUseException extends RuntimeException {
    public EmailInUseException(String msg) { super(msg); }
}
