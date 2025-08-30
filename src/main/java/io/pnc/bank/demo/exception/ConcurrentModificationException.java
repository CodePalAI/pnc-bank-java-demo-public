package io.pnc.bank.demo.exception;

public class ConcurrentModificationException extends RuntimeException {
    
    public ConcurrentModificationException(String message) {
        super(message);
    }
    
    public ConcurrentModificationException(String message, Throwable cause) {
        super(message, cause);
    }
}