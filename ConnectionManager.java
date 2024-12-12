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

    public ConnectionManager(DownloadTaskManager downloadManager) {
        this.downloadManager = downloadManager;
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
        try (ObjectOutputStream objectOut = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream objectIn = new ObjectInputStream(clientSocket.getInputStream())) {
            try {
                Message request = (Message) objectIn.readObject();

                // Verificar o tipo de pedido
                // Pedido de conexão
                if (request != null && request.getType().equals("HELLO")) {
                    handleHello(request, objectOut, objectIn);
                }
                // Pedido de pesquisa
                else if (request != null && request.getType().equals("SEARCH")) {
                    handleSearch(request, objectOut, objectIn);
                    // Pedido de download
                } else if (request != null && request.getType().equals("DOWNLOAD")) {
                    handleDownload(request, objectOut, objectIn);
                } else if (request != null && request.getType().equals("BLOCK")) {
                    handleBlockRequest(request, objectOut, objectIn);
                } else {
                    System.out.println("Pedido inválido recebido.");
                }
            } catch (ClassNotFoundException e) {
                System.out.println("Erro ao ler pedido do cliente: " + e.getMessage());
            }
        } catch (IOException e) {
            System.out.println("Erro ao processar pedido do cliente: " + e.getMessage());
        }
    }

    private void handleHello(Message message, ObjectOutputStream objectOut, ObjectInputStream objectIn) {
        HelloMessage helloMessage = (HelloMessage) message;
        String ipAddress = helloMessage.getIpAddress();
        int port = helloMessage.getPort();
        NodeConnection nodeConnection = new NodeConnection(ipAddress, port);
        System.out.println("Pedido de conexão recebido de " + ipAddress + ":" + port);
        downloadManager.addActiveConnection(nodeConnection);
        try {
            HelloMessage response = new HelloMessage(downloadManager.getIpAddress(), downloadManager.getPort());
            objectOut.writeObject(response);
        } catch (IOException e) {
            System.out.println("Erro ao enviar resposta HELLO: " + e.getMessage());
        }
    }

    private void handleSearch(Message message, ObjectOutputStream objectOut, ObjectInputStream objectIn) {
        SearchMessage searchMessage = (SearchMessage) message;
        String keyword = searchMessage.getQuery(); // Extrair o termo de busca
        System.out.println("Pedido de busca recebido: \"" + keyword + "\"");
        List<File> searchResults = downloadManager.getSharedFilesManager().searchFiles(keyword);
        List<String> results = new ArrayList<>();
        try {
            for (File file : searchResults) {
                // Calcular o hash do ficheiro
                byte[] data = Files.readAllBytes(file.toPath());
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
                String checksum = new BigInteger(1, hash).toString(16);
                results.add(file.getName() + ":" + file.length() + ":" + checksum); // Adicionar resultados
            }
            SearchResultsMessage response = new SearchResultsMessage(results); // Criar mensagem de resposta
            objectOut.writeObject(response); // Enviar resultados
        } catch (NoSuchAlgorithmException | IOException e) {
            System.out.println("Erro ao criar instância de MessageDigest: " + e.getMessage());
        }

        System.out.println("Resultados enviados para o cliente.");
    }

    private void handleDownload(Message message, ObjectOutputStream objectOut, ObjectInputStream objectIn) {
        DownloadMessage downloadMessage = (DownloadMessage) message;
        String fileName = downloadMessage.getFileName();
        String checksum = "";
        System.out.println("Pedido de download recebido: " + fileName);
        File file = downloadManager.getSharedFilesManager().getFileByName(fileName);
        if (file != null) {
            System.out.println("Ficheiro encontrado: " + file.getName());
            int fileBlocks = 0;
            try {
                byte[] data = Files.readAllBytes(file.toPath());
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
                checksum = new BigInteger(1, hash).toString(16);
            } catch (NoSuchAlgorithmException | IOException e) {
                System.out.println("Erro ao criar instância de MessageDigest: " + e.getMessage());
            }
            for (FileBlockRequestMessage blockMessage : downloadManager.getBlockRequestMessages()) {
                if (blockMessage.getFileName().equals(fileName)) {
                    fileBlocks++;
                }
            }
            DownloadResultMessage response = new DownloadResultMessage(checksum, true, fileBlocks);
            try {
                objectOut.writeObject(response); // Enviar resposta de sucesso
                System.out.println("Resposta de download enviada para o cliente.");
            } catch (IOException e) {
                System.out.println("Erro ao enviar resposta de download: " + e.getMessage());
            }
        } else {
            DownloadResultMessage response = new DownloadResultMessage(checksum, false, 0);
            try {
                objectOut.writeObject(response); // Enviar resposta de falha
                System.out.println("Resposta de download enviada para o cliente.");
            } catch (IOException e) {
                System.out.println("Erro ao enviar resposta de download: " + e.getMessage());
            }
        }
    }

    private void handleBlockRequest(Message message, ObjectOutputStream objectOut, ObjectInputStream objectIn) {
        BlockMessage blockMessage = (BlockMessage) message;
        String fileChecksum = blockMessage.getChecksum();
        int blockIndex = blockMessage.getBlockIndex();
        for (FileBlockRequestMessage fileBlockMessage : downloadManager.getBlockRequestMessages()) {
            if (fileBlockMessage.getFileChecksum().equals(fileChecksum)
                    && fileBlockMessage.getBlockIndex() == blockIndex) {
                try {
                    objectOut.writeObject(fileBlockMessage); // Enviar bloco de ficheiro
                    System.out.println("Bloco de ficheiro " + blockIndex + " enviado para o cliente.");
                    break;
                } catch (IOException e) {
                    System.out.println("Erro ao enviar bloco de ficheiro: " + e.getMessage());
                }
            }
        }
    }
}