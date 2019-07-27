package cn.killianxu;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Test;

import java.io.IOException;

/**
 * Unit test for simple example
 */
public class SimpleExampleTest
{
    @Test
    public void testSimple(){
        try{
            ZooKeeper zk=createClient();
            new SimpleExample().simple(zk);
        }
        catch (Exception e){

        }

    }
    protected ZooKeeper createClient() throws IOException {
        String connStr="106.52.210.35:2181";
        ZooKeeper zk = new ZooKeeper(connStr,60, new Watcher() {
            // 监控所有被触发的事件
            public void process(WatchedEvent event) {
                System.out.println("已经触发了" + event.getType() + "事件！");
            }
        });
        return zk;
    }
}
