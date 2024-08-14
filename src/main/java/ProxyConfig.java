import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;

import java.net.InetSocketAddress;

public class ProxyConfig {

    private final String TAG = "ProxyConfig";
    private final boolean enableLogging=true;

    private SimpleLogger logger = new SimpleLogger();



    private String host;
    private int port;
    private String userName=null;
    private String password=null;

    public enum ProxyType {
        SOCKS5, SOCKS4, HTTPPROXY
    }

    private ProxyType proxyType;

    public void setProxyHost(String host){
        this.host=host;
    }

    public void setProxyPort(int port){
        this.port=port;
    }

    public void setProxyUserName(String username){
        this.userName = username;
    }

    public void setProxyPassword(String password){
        this.password=password;
    }

    public void setProxyType(ProxyType proxyType){
        this.proxyType=proxyType;
    }

    private void log(String data){
        if (enableLogging){
            logger.log(data);
        }
    }

    public ProxyHandler getProxyHandler(){
        ProxyHandler retval = null;
        switch (this.proxyType){
            case SOCKS4:
                Socks4ProxyHandler s4proxy = null;
                if (this.userName != null && this.password != null) {
                    s4proxy = new Socks4ProxyHandler(new InetSocketAddress(this.host, this.port), this.userName);
                } else {
                    s4proxy = new Socks4ProxyHandler(new InetSocketAddress(this.host, this.port));
                }
                retval = (ProxyHandler) s4proxy;
                break;
            case SOCKS5:
                Socks5ProxyHandler s5proxy = null;
                if (this.userName != null && this.password != null) {
                    s5proxy = new Socks5ProxyHandler(new InetSocketAddress(this.host, this.port), this.userName, this.password);
                    log("Creating Socks5 proxy Handler with username/password");
                } else {
                    s5proxy = new Socks5ProxyHandler(new InetSocketAddress(this.host, this.port));
                    log("Creating Socks5 proxy Handler without username/password");
                }
                retval = (ProxyHandler) s5proxy;
                break;
            case HTTPPROXY:
                HttpProxyHandler httpproxy = null;
                if (this.userName != null && this.password != null) {
                    httpproxy = new HttpProxyHandler(new InetSocketAddress(this.host, this.port), this.userName, this.password);
                } else {
                    httpproxy = new HttpProxyHandler(new InetSocketAddress(this.host, this.port));
                }
                retval = (ProxyHandler) httpproxy;
                break;
        }
        return retval;
    }
}
