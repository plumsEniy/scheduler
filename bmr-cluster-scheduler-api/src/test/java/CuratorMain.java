import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

public class CuratorMain {
    private final CuratorFramework curatorFramework;
    public CuratorMain(){
        curatorFramework=CuratorFrameworkFactory.builder()
                .connectString("10.150.65.63:2181")
                .sessionTimeoutMs(5000).connectionTimeoutMs(20000)
                .retryPolicy(new ExponentialBackoffRetry(1000,3))
                .namespace("curator").build();
        curatorFramework.start();
    }
    public void nodeCRUD() throws Exception {
        System.out.println("开始创建节点");
        String node=curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath("/node");
        System.out.println("节点创建成功："+node);
        Stat stat=new Stat(); //存储节点信息
        curatorFramework.getData().storingStatIn(stat).forPath(node);
        System.out.println("查询节点："+node+"信息，stat:"+stat.toString());
        stat=curatorFramework.setData().withVersion(stat.getVersion()).forPath(node,"Hello World".getBytes());
        String result=new String(curatorFramework.getData().forPath(node));
        System.out.println("修改节点后的数据信息："+result);
        System.out.println("开始删除节点");
        curatorFramework.delete().forPath(node);
        Stat exist=curatorFramework.checkExists().forPath(node);
        if(exist==null){
            System.out.println("节点删除成功");
        }
    }

    public static void main(String[] args) throws Exception {
        CuratorMain curatorMain=new CuratorMain();
        curatorMain.nodeCRUD();
    }

}
