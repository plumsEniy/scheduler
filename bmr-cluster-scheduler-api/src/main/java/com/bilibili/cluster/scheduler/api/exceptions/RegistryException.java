
package com.bilibili.cluster.scheduler.api.exceptions;

public final class RegistryException extends RuntimeException {

    public RegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RegistryException(String message) {
        super(message);
    }
}
