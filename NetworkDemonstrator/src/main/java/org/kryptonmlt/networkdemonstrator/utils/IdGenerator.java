package org.kryptonmlt.networkdemonstrator.utils;

/**
 *
 * @author Kurt
 */
public class IdGenerator {

    private static IdGenerator instance = null;

    private long nextId = 0;

    private IdGenerator() {

    }

    public static IdGenerator getInstance() {
        if (instance == null) {
            instance = new IdGenerator();
        }
        return instance;
    }

    public synchronized long getNextId() {
        long r = nextId;
        nextId++;
        return r;
    }
}
