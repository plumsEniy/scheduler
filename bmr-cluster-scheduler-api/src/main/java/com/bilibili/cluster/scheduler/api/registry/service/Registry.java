
package com.bilibili.cluster.scheduler.api.registry.service;

import com.bilibili.cluster.scheduler.api.exceptions.RegistryException;
import lombok.NonNull;

import java.io.Closeable;
import java.time.Duration;
import java.util.Collection;

/**
 * Registry
 */
public interface Registry extends Closeable {

    /**
     * Connect to the registry, will wait in the given timeout
     *
     * @param timeout max timeout, if timeout <= 0 will wait indefinitely.
     * @throws RegistryException cannot connect in the given timeout
     */
    void connectUntilTimeout(@NonNull Duration timeout) throws RegistryException;

    boolean subscribe(String path, SubscribeListener listener);

    /**
     * Remove the path from the subscribe list.
     */
    void unsubscribe(String path);

    /**
     * Add a connection listener to collection.
     */
    void addConnectionStateListener(ConnectionListener listener);

    /**
     * @return the value
     */
    String get(String key);

    /**
     *
     * @param key
     * @param value
     * @param deleteOnDisconnect if true, when the connection state is disconnected, the key will be deleted
     */
    void put(String key, String value, boolean deleteOnDisconnect);

    /**
     * This function will delete the keys whose prefix is {@param key}
     * @param key the prefix of deleted key
     * @throws if the key not exists, there is a registryException
     */
    void delete(String key);

    /**
     * @return {@code true} if key exists.
     * E.g: registry contains  the following keys:[/test/test1/test2,]
     * if the key: /test
     * Return: test1
     */
    Collection<String> children(String key);

    /**
     * @return if key exists,return true
     */
    boolean exists(String key);

    /**
     * Acquire the lock of the prefix {@param key}
     */
    boolean acquireLock(String key);

    /**
     * Release the lock of the prefix {@param key}
     */
    boolean releaseLock(String key);
}
