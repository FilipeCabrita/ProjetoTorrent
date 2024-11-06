import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class ConnectionManager {
    private DownloadManager downloadManager;
    private ServerSocket serverSocket;

    public ConnectionManager(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    // Iniciar o servidor para escutar na porta especificada
    public void startServer() {
        try {
            serverSocket = new ServerSocket(downloadManager.getPort());
            System.out.println("Servidor iniciado no IP " + downloadManager.getIpAddress() + " e porta " + downloadManager.getPort());

            // Thread para escutar conexões
            new Thread(() -> {
                while (true) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("Conexão recebida de " + clientSocket.getInetAddress().getHostAddress());
                        handleClientRequest(clientSocket);
                    } catch (IOException e) {
                        System.out.println("Erro ao aceitar conexão: " + e.getMessage());
                    }
                }
            }).start();
        } catch (IOException e) {
            System.out.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }

    // Método para processar pedidos do cliente
    private void handleClientRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Exemplo simples de comunicação: Cliente envia nome do ficheiro
            String fileName = in.readLine();
            List<FileBlockRequestMessage> blocks = downloadManager.getBlockRequestMessages();

            for (FileBlockRequestMessage block : blocks) {
                if (block.getFileName().equals(fileName)) {
                    out.println(block); // Envia detalhes do bloco como string
                }
            }
        } catch (IOException e) {
            System.out.println("Erro ao processar pedido do cliente: " + e.getMessage());
        }
    }
}