package org.example.lock;

import org.apache.log4j.Logger;
import org.apache.zookeeper.*;
import org.example.utils.ZKUtil;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @ClassDesc 功能描述: 基于zookeeper实现的分布式锁
 * @Author huangjk
 * @create 2020/5/23 21:44
 */
public class ZKLock implements DistributedLock, AsyncCallback.StringCallback,
        AsyncCallback.ChildrenCallback, Watcher {
    private static Logger logger = Logger.getLogger(ZKLock.class);
    /**
     * zookeeper server
     */
    private static ZooKeeper zk;
    /**
     * zookeeper回调时的默认上下文
     */
    private static String CTX = "ctx";

    private static int DEFAULT_LATCH = 1;
    /**
     * 标记锁重入次数
     */
    private int reentrant = 0;

    private CountDownLatch latch = new CountDownLatch(DEFAULT_LATCH);
    /**
     * 在zookeeper中锁节点的名称
     */
    private String lockName;

    private String threadName;

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    /**
     *  注入zk资源
     */
    static {
        zk = ZKUtil.getZK();
    }

    /**
     * 加锁，若拿不到锁应该阻塞当前线程
     * 创建一个临时的序列化节点，然后阻塞当前线程，在创建成功的回调中获取当前目录的所有节点，
     * 然后对这些节点进行排序，如果发现自己是第一个节点，则取消阻塞，否则监听自己的前继节点
     * 然后继续排队等待。
     */
    @Override
    public void tryLock() {
        try {
            // 判断是否重入锁
            if (isReentrantLock()) {
                latch = new CountDownLatch(++reentrant + DEFAULT_LATCH);
            } else {
                zk.create(String.format("/%s-", threadName), threadName.getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL, this, CTX);
                latch.await();
            }
        } catch (Exception e) {
            logger.error("unknown error on trying lock ", e);
        }
    }

    /**
     * 判断是否发生所的重入
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    public boolean isReentrantLock() throws KeeperException, InterruptedException {
        if (lockName == null) {
            return false;
        }
        List<String> children = zk.getChildren("/", false);
        for (String child : children) {
            if (child.contains(lockName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解锁
     */
    @Override
    public void unLock() {
        try {
            if (reentrant > 0) {
                reentrant--;
                zk.getChildren("/", false, this, CTX);
            } else {
                // 删除当前节点
                zk.delete("/" + lockName, -1);
            }
        } catch (Exception e) {
            logger.error("unknown error on unlock ", e);
        }
    }

    /**
     * tryLock 创建节点后的回调
     * @param rc
     * @param path
     * @param ctx
     * @param name
     */
    @Override
    public void processResult(int rc, String path, Object ctx, String name) {
        if (name != null && !name.trim().isEmpty()) {
            logger.info(String.format(threadName + " create lock node...%s", name));
            lockName = name.substring(1);
            // 这里不需要注册监听器是因为我们只需要关注前继节点
            zk.getChildren("/", false, this, CTX);
        }
    }

    /**
     * getChildren 时回调
     * @param rc
     * @param path
     * @param ctx
     * @param children
     */
    @Override
    public void processResult(int rc, String path, Object ctx, List<String> children) {
        // 获取到当前目录创建了多少把锁，进行排序，然后判断自己是否是第一个节点，如果是则执行，
        // 如果不是则监听前继节点继续阻塞。
        Collections.sort(children);
        int index = children.indexOf(lockName);
        if (index == 0) {
            // 如果当前节点是第一个，则取消阻塞
            logger.info(threadName + " is working...");
            latch.countDown();
        } else {
            try {
                // 监听前继节点
                zk.exists("/" + children.get(index - 1), this);
            } catch (Exception e) {
                logger.error(String.format("unknown error while listen to the path %s",
                        children.get(index - 1)), e);
            }
        }
    }

    /**
     * 监听前继节点，一旦前继节点被删除，意味着轮到当前节点执行了
     */
    @Override
    public void process(WatchedEvent event) {
        switch (event.getType()) {
            case None:
                break;
            case NodeCreated:
                break;
            case NodeDeleted:
                logger.info(threadName + "监听到前置node被删除了");
                // 重新计算节点的情况
                zk.getChildren("/", false, this, CTX);
                break;
            case NodeDataChanged:
                break;
            case NodeChildrenChanged:
                break;
        }
    }
}
