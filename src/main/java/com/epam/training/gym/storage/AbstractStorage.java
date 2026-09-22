package com.epam.training.gym.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public abstract class AbstractStorage<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractStorage.class);

    private final Map<Long, T> storage = new ConcurrentHashMap<>();

    public void save(Long id, T entity) {
        storage.put(id, entity);
        log.debug("[{}] saved entity with id={}", getClass().getSimpleName(), id);
    }

    public T get(Long id) {
        return storage.get(id);
    }

    public void delete(Long id) {
        storage.remove(id);
        log.debug("[{}] deleted entity with id={}", getClass().getSimpleName(), id);
    }

    public Collection<T> getAll() {
        return storage.values();
    }

    public boolean exists(Long id) {
        return storage.containsKey(id);
    }

    public int size() {
        return storage.size();
    }
}