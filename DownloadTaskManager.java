import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

public class DownloadTaskManager {
    private SharedFilesManager sharedFilesManager;
    private List<FileBlockRequestMessage> blockRequestMessages;
    private static final int BLOCK_SIZE = 10240; // 10KB
    private String ipAddress;
    private int port;
    private List<NodeConnection> activeConnections; // Lista de conexões ativas
    private List<FileBlockRequestMessage> blockRequests = new ArrayList<>();

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
            String fileChecksum = sharedFilesManager.getFileChecksum(fileName);
            for (int blockIndex = 0; blockIndex < totalBlocks; blockIndex++) {
                int currentBlockSize = (int) Math.min(BLOCK_SIZE, fileSize - (blockIndex * BLOCK_SIZE));
                // Separar os dados do bloco do ficheiro
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.skip(blockIndex * BLOCK_SIZE);
                    byte[] blockData = new byte[currentBlockSize];
                    fis.read(blockData);
                    FileBlockRequestMessage blockRequest = new FileBlockRequestMessage(fileName, blockIndex,
                            currentBlockSize, blockData, fileChecksum);
                    blockRequestMessages.add(blockRequest);
                } catch (IOException e) {
                    System.out.println("Erro ao criar pedido de bloco: " + e.getMessage());
                }
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
                            synchronized (results) {
                                results.add(response);
                            }
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
    public void requestDownloadToNodes(String fileName) {
        // Verificar se o ficheiro já existe localmente
        File localFile = sharedFilesManager.getFileByName(fileName);
        if (localFile != null) {
            System.out.println("O ficheiro já existe localmente.");
            return;
        }
        List<NodeConnection> nodesWithFile = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        List<String> responses = new ArrayList<>();
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
                            if (response.startsWith("DOWNLOAD_RESPONSE:")) {
                                if (response.split(":")[1].equals("true")) {
                                    response = response.split(":")[2] + ":" + response.split(":")[3];
                                    responses.add(response);
                                    synchronized (nodesWithFile) {
                                        nodesWithFile.add(connection);
                                    }
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
        requestFileBlocks(fileName, nodesWithFile, responses, null);
        rebuildFile(fileName);
    }

    // Método para solicitar blocos de ficheiros a nós conectados
    private void requestFileBlocks(String fileName, List<NodeConnection> nodesWithFile, List<String> responses,
            List<String> blocksToRepeat) {
        blockRequests.clear();
        List<Thread> threads = new ArrayList<>();
        List<Integer> blocksToRequest = new ArrayList<>();
        String fileChecksum = responses.get(0).split(":")[0];
        // Eliminar duplicados em responses
        responses = new ArrayList<>(new HashSet<>(responses));
        // Se ainda houver respostas diferentes dá erro
        if (responses.size() > 1) {
            System.out.println("Erro: múltiplas respostas diferentes para o mesmo ficheiro.");
            return;
        }
        if (blocksToRepeat == null) {

            for (int i = 0; i < Integer.valueOf(responses.get(0).split(":")[1]); i++) {
                blocksToRequest.add(i);
            }
        } else {
            for (String block : blocksToRepeat) {
                blocksToRequest.add(Integer.valueOf(block));
            }
        }

        // Cria um diretorio temporario para armazenar os blocos dentro do diretorio
        // partilhado
        File tempDir = new File(sharedFilesManager.getSharedFolderPath() + File.separator + ".temp");
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }

        while (blocksToRequest.size() > 0) {
            for (NodeConnection connection : nodesWithFile) {
                // Limitar o número de threads a 10
                if (threads.size() >= 10) {
                    try {
                        for (Thread thread : threads) {
                            thread.join();
                        }
                        threads.clear();
                    } catch (InterruptedException e) {
                        System.out.println("Erro ao esperar pela thread: " + e.getMessage());
                    }
                }

                // Cria uma thread para solicitar os blocos
                Thread thread = new Thread() {
                    public void run() {
                        try (Socket socket = new Socket(connection.getIpAddress(), connection.getPort());
                                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);) {

                            // Enviar pedido de bloco
                            out.println("BLOCK_REQUEST:" + fileChecksum + ":" + blocksToRequest.remove(0));

                            ObjectInputStream objectIn = new ObjectInputStream(socket.getInputStream());

                            // Ler resposta
                            try {
                                FileBlockRequestMessage blockRequest = (FileBlockRequestMessage) objectIn.readObject();
                                synchronized (blockRequests) {
                                    blockRequests.add(blockRequest);
                                }
                            } catch (ClassNotFoundException e) {
                                System.out.println("Erro ao ler objeto: " + e.getMessage());
                            }

                        } catch (IOException e) {
                            System.out.println("Erro ao solicitar blocos ao nó " + connection.getIpAddress() + ":"
                                    + connection.getPort() + " - " + e.getMessage());
                        }
                    }
                };
                threads.add(thread);
                thread.start();
            }
        }
        // Esperar que todas as threads terminem
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.out.println("Erro ao esperar pela thread: " + e.getMessage());
            }
        }

        // Verificar se todos os blocos foram recebidos
        System.out.println("Blocos:");
        for (FileBlockRequestMessage blockRequest : blockRequests) {
            System.out.println(blockRequest.toString());
        }
        System.out.println("Blocos recebidos: " + blockRequests.size());
        blocksToRepeat = new ArrayList<>();

        // Escrever os ficheiros dos blocos na pasta temporária
        for (int i = 0; i < Integer.valueOf(responses.get(0).split(":")[1]); i++) {
            for (FileBlockRequestMessage blockRequest : blockRequests) {
                if (blockRequest.getBlockIndex() == i) {
                    File blockFile = new File(
                            tempDir.getAbsolutePath() + File.separator + blockRequest.getFileName() + ".part"
                                    + blockRequest.getBlockIndex());
                    try (FileOutputStream fos = new FileOutputStream(blockFile)) {
                        fos.write(blockRequest.getData());
                        break;
                    } catch (IOException e) {
                        System.out.println("Erro ao escrever bloco: " + e.getMessage());
                    }
                } else if (blockRequest == blockRequests.getLast()) {
                    blocksToRepeat.add(String.valueOf(i));
                    System.out.println("Erro: bloco " + i + " não recebido.");
                }
            }
        }

        // Verificar se todos os blocos foram recebidos
        File[] blockFiles = tempDir.listFiles();
        if (blockFiles != null && blockFiles.length == Integer.valueOf(responses.get(0).split(":")[1])) {
            System.out.println("Todos os blocos foram recebidos.");
        } else {
            System.out.println("Erro: nem todos os blocos foram recebidos.");
            requestFileBlocks(fileName, nodesWithFile, responses, blocksToRepeat);
        }
    }

    // Método para reconstruir o ficheiro a partir dos blocos
    private void rebuildFile(String fileName) {
        File tempDir = new File(sharedFilesManager.getSharedFolderPath() + File.separator + ".temp");
        File[] blockFiles = tempDir.listFiles();
        for (int i = 0; i < blockFiles.length; i++) {
            try (FileInputStream fis = new FileInputStream(sharedFilesManager.getSharedFolderPath() + File.separator
                    + ".temp" + File.separator + fileName + ".part" + i);
                    FileOutputStream fos = new FileOutputStream(
                            sharedFilesManager.getSharedFolderPath() + File.separator + fileName, true)) {
                byte[] data = new byte[(int) blockFiles[i].length()];
                fis.read(data);
                fos.write(data);
            } catch (IOException e) {
                System.out.println("Erro ao reconstruir o ficheiro: " + e.getMessage());
            }
        }

        // Eliminar os blocos temporários
        for (File blockFile : blockFiles) {
            blockFile.delete();
        }
        tempDir.delete();
    }

    public SharedFilesManager getSharedFilesManager() {
        return sharedFilesManager;
    }
}