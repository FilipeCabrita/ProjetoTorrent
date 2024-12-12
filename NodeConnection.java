import java.io.Serializable;

public class NodeConnection implements Serializable {
    private String ip;
    private int port;

    public NodeConnection(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIpAddress() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return ip + ":" + port;
    }
}