import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.security.*;
import java.nio.file.Files;

public class ConnectionManager {
    private DownloadTaskManager downloadManager;
    private ServerSocket serverSocket;
    private List<NodeConnection> activeConnections; // Lista de conexões ativas

    public ConnectionManager(DownloadTaskManager downloadManager) {
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

                        // Adicionar o nó à lista de conexões ativas se ainda não estiver presente
                        if (activeConnections.stream().noneMatch(
                                conn -> conn.getIpAddress().equals(clientSocket.getInetAddress().getHostAddress())
                                        && conn.getPort() == clientSocket.getLocalPort())) {
                            NodeConnection newConnection = new NodeConnection(
                                    clientSocket.getInetAddress().getHostAddress(), clientSocket.getLocalPort());
                            activeConnections.add(newConnection);
                            System.out.println("Conexões ativas: " + activeConnections);
                        } else {
                            System.out.println("Conexão já existente.");
                        }

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
    private void handleClientRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);) {

            String request = in.readLine();

            // Verificar o tipo de pedido
            // Pedido de conexão
            if (request != null && request.startsWith("HELLO:")) {
                String[] parts = request.split(":");
                String ipAddress = parts[1];
                int port = Integer.parseInt(parts[2]);
                System.out.println("Pedido de conexão recebido de " + ipAddress + ":" + port);
                out.println("HELLO"); // Enviar confirmação
            }
            // Pedido de pesquisa
            else if (request != null && request.startsWith("SEARCH:")) {
                String keyword = request.substring(7); // Extrair o termo de busca
                System.out.println("Pedido de busca recebido: " + keyword);
                List<File> searchResults = downloadManager.getSharedFilesManager().searchFiles(keyword);
                try {
                    for (File file : searchResults) {
                        // Calcular o hash do ficheiro
                        byte[] data = Files.readAllBytes(file.toPath());
                        byte[] hash = MessageDigest.getInstance("MD5").digest(data);
                        String checksum = new BigInteger(1, hash).toString(16);
                        out.println(file.getName() + ":" + file.length() + ":" + checksum); // Enviar resultados
                    }
                } catch (NoSuchAlgorithmException e) {
                    System.out.println("Erro ao criar instância de MessageDigest: " + e.getMessage());
                }

                System.out.println("Resultados enviados para o cliente.");
                // Pedido de download
            } else if (request != null && request.startsWith("DOWNLOAD")) {
                String fileName = request.substring(9); // Extrair o nome do ficheiro
                String checksum = "";
                System.out.println("Pedido de download recebido: " + fileName);
                File file = downloadManager.getSharedFilesManager().getFileByName(fileName);
                if (file != null) {
                    System.out.println("Ficheiro encontrado: " + file.getName());
                    int fileBlocks = 0;
                    try {
                        byte[] data = Files.readAllBytes(file.toPath());
                        byte[] hash = MessageDigest.getInstance("MD5").digest(data);
                        checksum = new BigInteger(1, hash).toString(16);
                    } catch (NoSuchAlgorithmException e) {
                        System.out.println("Erro ao criar instância de MessageDigest: " + e.getMessage());
                    }
                    for (FileBlockRequestMessage message : downloadManager.getBlockRequestMessages()) {
                        if (message.getFileName().equals(fileName)) {
                            fileBlocks++;
                        }
                    }
                    out.println("DOWNLOAD_RESPONSE:true:" + checksum + ":" + fileBlocks); // Enviar resposta de sucesso
                } else {
                    out.println("DOWNLOAD_RESPONSE:false"); // Enviar resposta de erro
                }
            } else if (request != null && request.startsWith("BLOCK_REQUEST:")) {
                String[] parts = request.split(":");
                try (ObjectOutputStream objectOut = new ObjectOutputStream(clientSocket.getOutputStream())) {
                    String fileChecksum = parts[1];
                    int blockIndex = Integer.parseInt(parts[2]);
                    for (FileBlockRequestMessage message : downloadManager.getBlockRequestMessages()) {
                        if (message.getFileChecksum().equals(fileChecksum) && message.getBlockIndex() == blockIndex) {
                            objectOut.writeObject(message); // Enviar bloco de ficheiro
                            System.out.println("Bloco de ficheiro" + blockIndex + "enviado para o cliente.");
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Erro ao enviar bloco de ficheiro: " + e.getMessage() + ":" + request);
                }
            } else {
                System.out.println("Pedido inválido recebido.");
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