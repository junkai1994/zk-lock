package org.example.lock;

/**
 * @ClassDesc 功能描述:
 * @Author huangjk
 * @create 2020/5/23 21:42
 */
public interface DistributedLock {
    /**
     * 加锁，若拿不到锁应该阻塞当前线程
     */
    void tryLock();

    /**
     * 解锁
     */
    void unLock();
}
