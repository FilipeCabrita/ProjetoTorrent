import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;

public class DownloadTaskManager {
    private SharedFilesManager sharedFilesManager;
    private List<FileBlockRequestMessage> blockRequestMessages;
    private static final int BLOCK_SIZE = 10240; // 10KB
    private String ipAddress;
    private int port;
    private List<NodeConnection> activeConnections; // Lista de conexões ativas

    public DownloadTaskManager(SharedFilesManager sharedFilesManager, String ipAddress, int port) {
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
                FileBlockRequestMessage blockRequest = new FileBlockRequestMessage(fileName, blockIndex,
                        currentBlockSize);
                blockRequestMessages.add(blockRequest);
            }
        }
    }

    // Método para iniciar a conexão com outro nó
    public void connectToNode(String nodeIp, int nodePort) {
        try (Socket socket = new Socket(nodeIp, nodePort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {


            out.println("HELLO:" + ipAddress + ":" + port);
            
            // Ler a resposta
            String response = in.readLine();
            System.out.println("Resposta do nó " + nodeIp + ":" + nodePort + ": " + response);
            
            if (!response.equals("HELLO")) {
                System.out.println("Erro ao conectar ao nó: resposta inesperada.");
                return;
            }

            System.out.println(
                    "Conectado ao nó: " + nodeIp + ":" + nodePort + " a usar a porta local " + socket.getLocalPort());

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

    // Método para pesquisar ficheiros em nós conectados
    public List<String> searchFilesInConnectedNodes(String keyword) {
        List<String> results = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        for (NodeConnection connection : activeConnections) {
            Thread thread = new Thread() {
                public void run() {
                    try (Socket socket = new Socket(connection.getIpAddress(), connection.getPort());
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                        // Enviar pedido de busca
                        out.println("SEARCH:" + keyword);

                        // Ler respostas
                        String response;
                        while ((response = in.readLine()) != null) {
                            results.add(response);
                        }
                        System.out.println("Conexões ativas: " + activeConnections);

                    } catch (IOException e) {
                        System.out.println("Erro ao buscar no nó " + connection.getIpAddress() + ":"
                                + connection.getPort() + " - " + e.getMessage());
                    }
                }
            };
            threads.add(thread);
            thread.start();
        }

        // Esperar que todas as threads terminem
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.out.println("Erro ao esperar pela thread: " + e.getMessage());
            }
        }

        return results;
    }

    // Método para solicitar download de ficheiros a nós conectados
    public List<NodeConnection> requestDownloadToNodes(String fileName) {
        List<NodeConnection> nodesWithFile = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        for (NodeConnection connection : activeConnections) {
            Thread thread = new Thread() {
                public void run() {
                    try (Socket socket = new Socket(connection.getIpAddress(), connection.getPort());
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                        // Enviar pedido de download
                        out.println("DOWNLOAD:" + fileName);

                        // Ler respostas
                        String response;
                        while ((response = in.readLine()) != null) {
                            if (response.equals("true")) {
                                synchronized (nodesWithFile) {
                                    nodesWithFile.add(connection);
                                }
                            }
                            System.out.println("Resposta do nó " + connection.getIpAddress() + ":"
                                    + connection.getPort() + ": " + response);
                        }
                    } catch (IOException e) {
                        System.out.println("Erro ao solicitar download ao nó " + connection.getIpAddress() + ":"
                                + connection.getPort() + " - " + e.getMessage());
                    }
                }
            };
            threads.add(thread);
            thread.start();
        }

        // Esperar que todas as threads terminem
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.out.println("Erro ao esperar pela thread: " + e.getMessage());
            }
        }

        return nodesWithFile;
    }

    public SharedFilesManager getSharedFilesManager() {
        return sharedFilesManager;
    }
}