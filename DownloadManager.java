import java.io.File;
import java.util.ArrayList;
import java.util.List;

class DownloadManager {
    private SharedFilesManager sharedFilesManager;
    private List<FileBlockRequestMessage> blockRequestMessages;
    private static final int BLOCK_SIZE = 10240; // 10 KB por bloco

    public DownloadManager(SharedFilesManager sharedFilesManager) {
        this.sharedFilesManager = sharedFilesManager;
        this.blockRequestMessages = new ArrayList<>();
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

    // Método para obter a lista de pedidos de blocos
    public List<FileBlockRequestMessage> getBlockRequestMessages() {
        return blockRequestMessages;
    }

    // Exemplo de uso
    public static void main(String[] args) {
        // Exemplo de pasta partilhada
        String sharedFolderPath = "./downloads";
        
        // Inicializar o SharedFilesManager
        SharedFilesManager sharedFilesManager = new SharedFilesManager(sharedFolderPath);
        
        // Inicializar o DownloadManager e criar pedidos de blocos
        DownloadManager downloadManager = new DownloadManager(sharedFilesManager);

        // Exibir os pedidos de blocos
        System.out.println("Pedidos de blocos de ficheiros:");
        for (FileBlockRequestMessage message : downloadManager.getBlockRequestMessages()) {
            System.out.println(message);
        }
    }
}