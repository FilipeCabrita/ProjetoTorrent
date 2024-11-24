import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;

public class DownloadManager {
    private SharedFilesManager sharedFilesManager;
    private List<FileBlockRequestMessage> blockRequestMessages;
    private static final int BLOCK_SIZE = 10240; // 10KB
    private String ipAddress;
    private int port;
    private List<NodeConnection> activeConnections; // Lista de conexões ativas

    public DownloadManager(SharedFilesManager sharedFilesManager, String ipAddress, int port) {
        this.sharedFilesManager = sharedFilesManager;
        this.blockRequestMessages = new ArrayList<>();
        this.ipAddress = ipAddress;
        this.port = port;
        this.activeConnections = new ArrayList<>();
        createBlockRequests();
    }

    // Método para criar mensagens de pedido de blocos de ficheiros
    private void createBlockRequests() {
        List<File> sharedFiles = sharedFilesManager.getSharedFiles();

        for (File file : sharedFiles) {
            long fileSize = file.length();
            String fileName = file.getName();
            int totalBlocks = (int) Math.ceil((double) fileSize / BLOCK_SIZE);

            for (int blockIndex = 0; blockIndex < totalBlocks; blockIndex++) {
                int currentBlockSize = (int) Math.min(BLOCK_SIZE, fileSize - (blockIndex * BLOCK_SIZE));
                FileBlockRequestMessage blockRequest = new FileBlockRequestMessage(fileName, blockIndex, currentBlockSize);
                blockRequestMessages.add(blockRequest);
            }
        }
    }

    // Método para iniciar a conexão com outro nó
    public void connectToNode(String nodeIp, int nodePort) {
        try (Socket socket = new Socket(nodeIp, nodePort)) {
            System.out.println("Conectado ao nó: " + nodeIp + ":" + nodePort);

            // Adicionar o nó à lista de conexões ativas
            NodeConnection newConnection = new NodeConnection(nodeIp, nodePort);
            activeConnections.add(newConnection);
            System.out.println("Conexões ativas: " + activeConnections);

            // Aqui você poderia enviar um pedido ou iniciar o processo de transferência
        } catch (IOException e) {
            System.out.println("Erro ao conectar ao nó: " + e.getMessage());
        }
    }

    public List<FileBlockRequestMessage> getBlockRequestMessages() {
        return blockRequestMessages;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    // Método para adicionar uma conexão ativa
    public void addActiveConnection(NodeConnection nodeConnection) {
        activeConnections.add(nodeConnection);
    }

    // Método para obter as conexões ativas
    public List<NodeConnection> getActiveConnections() {
        return activeConnections;
    }

    public List<String> searchFilesInConnectedNodes(String keyword) {
        List<String> results = new ArrayList<>();
    
        for (NodeConnection connection : activeConnections) {
            try (Socket socket = new Socket(connection.getIp(), connection.getPort());
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
    
                // Enviar pedido de busca
                out.println("SEARCH:" + keyword);
    
                // Ler respostas
                String response;
                while ((response = in.readLine()) != null) {
                    results.add("Remoto | " + response);
                }
    
            } catch (IOException e) {
                System.out.println("Erro ao buscar no nó " + connection.getIp() + ":" + connection.getPort() + " - " + e.getMessage());
            }
        }
    
        return results;
    }

    public SharedFilesManager getSharedFilesManager() {
        return sharedFilesManager;
    }
}