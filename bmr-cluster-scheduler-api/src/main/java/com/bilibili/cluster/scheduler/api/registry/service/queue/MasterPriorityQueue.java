
package com.bilibili.cluster.scheduler.api.registry.service.queue;


import com.bilibili.cluster.scheduler.api.dto.registry.Server;
import com.bilibili.cluster.scheduler.common.utils.NetUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MasterPriorityQueue implements TaskPriorityQueue<Server> {

    /**
     * queue size
     */
    private static final Integer QUEUE_MAX_SIZE = 20;

    /**
     * queue
     */
    private PriorityBlockingQueue<Server> queue = new PriorityBlockingQueue<>(QUEUE_MAX_SIZE, new ServerComparator());

    private HashMap<String, Integer> hostIndexMap = new HashMap<>();

    @Override
    public void put(Server serverInfo) {
        this.queue.put(serverInfo);
        refreshMasterList();
    }

    @Override
    public Server take() throws InterruptedException {
        return queue.take();
    }

    @Override
    public Server poll(long timeout, TimeUnit unit) {
        return queue.poll();
    }

    @Override
    public int size() {
        return queue.size();
    }

    public void putList(List<Server> serverList) {
        for (Server server : serverList) {
            this.queue.put(server);
        }
        refreshMasterList();
    }

    public void remove(Server server) {
        this.queue.remove(server);
    }

    public void clear() {
        queue.clear();
        refreshMasterList();
    }

    private void refreshMasterList() {
        hostIndexMap.clear();
        Iterator<Server> iterator = queue.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            Server server = iterator.next();
            String addr = NetUtils.getAddr(server.getHost(), server.getPort());
            hostIndexMap.put(addr, index);
            index += 1;
        }

    }

    public int getIndex(String addr) {
        if (!hostIndexMap.containsKey(addr)) {
            return -1;
        }
        return hostIndexMap.get(addr);
    }

    /**
     * server comparator, used to sort server by createTime in reverse order.
     */
    private class ServerComparator implements Comparator<Server> {

        @Override
        public int compare(Server o1, Server o2) {
            return o2.getCreateTime().compareTo(o1.getCreateTime());
        }
    }

}
