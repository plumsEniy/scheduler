package com.bilibili.cluster.scheduler.common.utils;

import com.bilibili.cluster.scheduler.common.Constants;

import java.net.*;
import java.util.*;

/**
 * @description:
 * @Date: 2024/1/19 14:26
 * @Author: nizhiqiang
 */
public class AddressUtils {

    private static boolean isLocal = false;

    public static void setLocalModel(boolean type) {
        isLocal = type;
    }

    public static List<Inet4Address> getLocalIp4AddressFromNetworkInterface() throws SocketException {
        List<Inet4Address> addresses = new ArrayList<>(1);
        Enumeration e = NetworkInterface.getNetworkInterfaces();
        if (e == null) {
            return addresses;
        }
        while (e.hasMoreElements()) {
            NetworkInterface n = (NetworkInterface) e.nextElement();
            if (!isValidInterface(n)) {
                continue;
            }
            Enumeration ee = n.getInetAddresses();
            while (ee.hasMoreElements()) {
                InetAddress i = (InetAddress) ee.nextElement();
                if (isValidAddress(i)) {
                    addresses.add((Inet4Address) i);
                }
            }
        }
        return addresses;
    }

    private static boolean isValidInterface(NetworkInterface ni) throws SocketException {
        return !ni.isLoopback() && !ni.isPointToPoint() && ni.isUp() && !ni.isVirtual()
                && (ni.getName().startsWith("eth") || ni.getName().startsWith("ens"));
    }

    /**
     * 判断是否是IPv4，并且内网地址并过滤回环地址.
     */
    private static boolean isValidAddress(InetAddress address) {
        return address instanceof Inet4Address && address.isSiteLocalAddress() && !address.isLoopbackAddress();
    }

    private static Optional<Inet4Address> getIpBySocket() throws SocketException {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            if (socket.getLocalAddress() instanceof Inet4Address) {
                return Optional.of((Inet4Address) socket.getLocalAddress());
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public static Optional<Inet4Address> getLocalIp4Address() throws SocketException {
        final List<Inet4Address> ipByNi = getLocalIp4AddressFromNetworkInterface();
        if (ipByNi.isEmpty() || ipByNi.size() > 1) {
            final Optional<Inet4Address> ipBySocketOpt = getIpBySocket();
            if (ipBySocketOpt.isPresent()) {
                return ipBySocketOpt;
            } else {
                return ipByNi.isEmpty() ? Optional.empty() : Optional.of(ipByNi.get(0));
            }
        }
        return Optional.of(ipByNi.get(0));
    }

    public static String getHostIp() {
        String sIP = "";
        InetAddress ip = null;
        try {
            boolean bFindIP = false;
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                if (bFindIP)
                    break;
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> ips = ni.getInetAddresses();
                while (ips.hasMoreElements()) {
                    ip = ips.nextElement();
                    if (!ip.isLoopbackAddress()
                            && ip.getHostAddress().matches("(\\d{1,3}\\.){3}\\d{1,3}")) {
                        bFindIP = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (null != ip)
            sIP = ip.getHostAddress();
        return sIP;
    }


    public static void main(String[] args) throws SocketException {
        System.out.println(getHostIp());
        System.out.println(getLocalIp4Address());
        System.out.println(getLocalIp4AddressFromNetworkInterface());
        String ip = null;

        String host = null;

        try{

            InetAddress ia = InetAddress.getLocalHost();

            host = ia.getHostName();//获取计算机名字

            ip = ia.getHostAddress();//获取IP


            System.out.println(host);

            System.out.println(ip);
            System.out.println(resolveLocalAddresses());
            // 出自比如这篇：http://www.cnblogs.com/zrui-xyu/p/5039551.html
            System.out.println("get LocalHost Address : " + getLocalHostAddress().getHostAddress());

            // 正确的IP拿法
            System.out.println("get LocalHost LAN Address : " + getLocalHostLANAddress().getHostAddress());

        } catch(UnknownHostException e) {

            e.printStackTrace();

        }

    }

    // 正确的IP拿法，即优先拿site-local地址
    private static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // 遍历所有的网络接口
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // 在所有的接口下再遍历IP
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {// 排除loopback类型地址
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            // site-local类型的地址未被发现，先记录候选地址
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            // 如果没有发现 non-loopback地址.只能用最次选的方案
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException(
                    "Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }

    //出自这篇：http://www.cnblogs.com/zrui-xyu/p/5039551.html
    //实际上的代码是不准的
    private static InetAddress getLocalHostAddress() throws UnknownHostException {
        Enumeration allNetInterfaces;
        try {
            allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip = null;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();

                Enumeration addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    ip = (InetAddress) addresses.nextElement();
                    if (!ip.isSiteLocalAddress() && !ip.isLoopbackAddress() && ip.getHostAddress().indexOf(":") == -1) {
                        if (ip != null && ip instanceof Inet4Address) {
                            return ip;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
        if (jdkSuppliedAddress == null) {
            throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
        }
        return jdkSuppliedAddress;
    }


    public static Set<String> resolveLocalAddresses() {
        Set<String> addrs = new LinkedHashSet<>();

        if (isLocal) {
            addrs.add(Constants.LOCAL_ADDRESS);
            return addrs;
        }

        Enumeration<NetworkInterface> ns = null;
        try {
            ns = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            // ignored...
        }
        while (ns != null && ns.hasMoreElements()) {
            NetworkInterface n = ns.nextElement();
            Enumeration<InetAddress> is = n.getInetAddresses();
            while (is.hasMoreElements()) {
                InetAddress i = is.nextElement();
                if (!i.isLoopbackAddress() && !i.isLinkLocalAddress() && !i.isMulticastAddress())
                    addrs.add(i.getHostAddress());
            }
        }
        return addrs;
    }
}
