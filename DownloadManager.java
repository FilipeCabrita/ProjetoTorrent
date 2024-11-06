import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;

public class DownloadManager {
    private SharedFilesManager sharedFilesManager;
    private List<FileBlockRequestMessage> blockRequestMessages;
    private static final int BLOCK_SIZE = 10240; // 10KB
    private String ipAddress;
    private int port;

    public DownloadManager(SharedFilesManager sharedFilesManager, String ipAddress, int port) {
        this.sharedFilesManager = sharedFilesManager;
        this.blockRequestMessages = new ArrayList<>();
        this.ipAddress = ipAddress;
        this.port = port;
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
}