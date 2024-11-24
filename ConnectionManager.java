import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ConnectionManager {
    private DownloadManager downloadManager;
    private ServerSocket serverSocket;
    private List<NodeConnection> activeConnections; // Lista de conexões ativas

    public ConnectionManager(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
        this.activeConnections = new CopyOnWriteArrayList<>(); // Lista thread-safe
    }

    // Iniciar o servidor para escutar na porta especificada
    public void startServer() {
        try {
            serverSocket = new ServerSocket(downloadManager.getPort());
            System.out.println("Servidor iniciado no IP " + downloadManager.getIpAddress() + " e porta "
                    + downloadManager.getPort());

            // Thread para escutar conexões
            new Thread(() -> {
                while (true) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("Conexão recebida de " + clientSocket.getInetAddress().getHostAddress());

                        // Adicionar o nó à lista de conexões
                        NodeConnection newConnection = new NodeConnection(
                                clientSocket.getInetAddress().getHostAddress(), clientSocket.getLocalPort());
                        activeConnections.add(newConnection);
                        System.out.println("Conexões ativas: " + activeConnections);

                        // Processar o pedido do cliente
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
    // Método para processar pedidos do cliente
    private void handleClientRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String request = in.readLine();

            if (request != null && request.startsWith("SEARCH:")) {
                String keyword = request.substring(7); // Extrair o termo de busca
                System.out.println("Pedido de busca recebido: " + keyword);

                List<File> searchResults = downloadManager.getSharedFilesManager().searchFiles(keyword);
                for (File file : searchResults) {
                    out.println("Nome: " + file.getName() + " | Tamanho: " + App.convertBytes(file.length()));
                }

                System.out.println("Resultados enviados para o cliente.");
            }
        } catch (IOException e) {
            System.out.println("Erro ao processar pedido do cliente: " + e.getMessage());
        }
    }

    // Método para obter as conexões ativas
    public List<NodeConnection> getActiveConnections() {
        return activeConnections;
    }
}