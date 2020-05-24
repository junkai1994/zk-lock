package org.example;

import org.apache.log4j.Logger;
import org.example.lock.ZKLock;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @ClassDesc 功能描述:
 * @Author huangjk
 * @create 2020/5/23 21:21
 */
public class BaseTest {
    private static Logger logger = Logger.getLogger(BaseTest.class);

    @Test
    public void test() {
        for (int i=0; i<10; i++) {
            new Thread( () -> {
                ZKLock lock = new ZKLock();
                lock.setThreadName(Thread.currentThread().getName());
                lock.tryLock();
                // 业务逻辑
                try {
                    TimeUnit.SECONDS.sleep(1);
                    logger.info(Thread.currentThread().getName() + " is done");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                lock.unLock();
            }).start();
        }
        while (true) {

        }
    }
}
