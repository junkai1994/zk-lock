package org.example.watcher;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

/**
 * @ClassDesc 功能描述:
 * @Author huangjk
 * @create 2020/5/23 21:18
 */
public class DefaultWatcher implements Watcher {
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getState()) {
            case Disconnected:
                break;
            case SyncConnected:
                System.out.println("Connected ...");
                break;
            case AuthFailed:
                break;
            case ConnectedReadOnly:
                break;
            case SaslAuthenticated:
                break;
            case Expired:
                break;
        }
    }
}
