package org.example.utils;

import org.apache.zookeeper.ZooKeeper;
import org.example.watcher.DefaultWatcher;

import java.io.IOException;
import java.util.Properties;

/**
 * @ClassDesc 功能描述:
 * @Author huangjk
 * @create 2020/5/23 21:11
 */
public class ZKUtil {
    private static ZooKeeper zk;

    /**
     * init zookeeper server when loaded
     */
    static {
        Properties properties = new Properties();
        try {
            properties.load(ZKUtil.class.getResourceAsStream("/zk-lock.properties"));
            zk = new ZooKeeper(properties.getProperty("connectString"), 3000, new DefaultWatcher());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ZooKeeper getZK() {
        return zk;
    }
}
