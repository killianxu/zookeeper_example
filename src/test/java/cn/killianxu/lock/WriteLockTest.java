package cn.killianxu.lock;

import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class WriteLockTest
{
    protected int sessionTimeout = 10 * 1000;
    protected String dir = "/" + getClass().getName();
    protected WriteLock[] nodes;
    protected CountDownLatch latch = new CountDownLatch(1);
    private boolean restartServer = true;
    private boolean workAroundClosingLastZNodeFails = true;
    private boolean killLeader = true;

    @Test
    public void testRun() throws Exception {
        runTest(3);
    }

    class LockCallback implements LockListener {
        /**
         * 成功获得锁时回调
         */
        public void lockAcquired() {
            latch.countDown();
        }

        /**
         * 释放锁时回调
         */
        public void lockReleased() {

        }

    }
    protected void runTest(int count) throws Exception {
        nodes = new WriteLock[count];
        for (int i = 0; i < count; i++) {
            ZooKeeper keeper = createClient();
            WriteLock leader = new WriteLock(keeper, dir, null);
            leader.setLockListener(new LockCallback());
            nodes[i] = leader;
            leader.lock();
        }

        // lets wait for any previous leaders to die and one of our new
        // nodes to become the new leader
        latch.await(30, TimeUnit.SECONDS);

        WriteLock first = nodes[0];
        dumpNodes(count);

        // lets assert that the first election is the leader
        Assert.assertTrue("The first znode should be the leader " + first.getId(), first.isOwner());

        for (int i = 1; i < count; i++) {
            WriteLock node = nodes[i];
            Assert.assertFalse("Node should not be the leader " + node.getId(), node.isOwner());
        }

        if (count > 1) {
            if (killLeader) {
                System.out.println("Now killing the leader");
                // now lets kill the leader
                latch = new CountDownLatch(1);
                first.unlock();
                latch.await(30, TimeUnit.SECONDS);
                //Thread.sleep(10000);
                WriteLock second = nodes[1];
                dumpNodes(count);
                // lets assert that the first election is the leader
                Assert.assertTrue("The second znode should be the leader " + second.getId(), second.isOwner());

                for (int i = 2; i < count; i++) {
                    WriteLock node = nodes[i];
                    Assert.assertFalse("Node should not be the leader " + node.getId(), node.isOwner());
                }
            }
        }
    }
    protected ZooKeeper createClient() throws IOException {
        String connStr="106.52.210.35:2181";
        ZooKeeper zk = new ZooKeeper(connStr,sessionTimeout,null);
        return zk;
    }
    protected void dumpNodes(int count) {
        for (int i = 0; i < count; i++) {
            WriteLock node = nodes[i];
            System.out.println("node: " + i + " id: " +
                    node.getId() + " is leader: " + node.isOwner());
        }
    }

    @After
    public void tearDown() throws Exception {
        if (nodes != null) {
            for (int i = 0; i < nodes.length; i++) {
                WriteLock node = nodes[i];
                if (node != null) {
                    System.out.println("Closing node: " + i);
                    node.close();
                    System.out.println("Closing zookeeper: " + i);
                    node.getZookeeper().close();
                    System.out.println("Closed zookeeper: " + i);
                    }
                }
        }
    }

}