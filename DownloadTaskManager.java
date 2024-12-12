import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

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
    public void createBlockRequests() {
        List<File> sharedFiles = sharedFilesManager.getSharedFiles();
        blockRequestMessages.clear();
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
                            currentBlockSize, blockData, fileChecksum, new NodeConnection(ipAddress, port));
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
                ObjectOutputStream objectOut = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream objectIn = new ObjectInputStream(socket.getInputStream())) {

            // Enviar pedido de conexão
            HelloMessage helloMessage = new HelloMessage(ipAddress, port);
            objectOut.writeObject(helloMessage);

            // Ler a resposta
            HelloMessage response = (HelloMessage) objectIn.readObject();
            System.out.println("Resposta do nó " + nodeIp + ":" + nodePort + ": " + response);

            if (!response.getType().equals("HELLO")) {
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
        } catch (IOException | ClassNotFoundException e) {
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
    public List<String> searchFilesInConnectedNodes(String keyword, CountDownLatch latch) {
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();

        for (NodeConnection connection : activeConnections) {
            Thread thread = new Thread() {
                public void run() {
                    try (Socket socket = new Socket(connection.getIpAddress(), connection.getPort());
                            ObjectOutputStream objectOut = new ObjectOutputStream(socket.getOutputStream());
                            ObjectInputStream objectIn = new ObjectInputStream(socket.getInputStream())) {

                        // Enviar pedido de busca
                        SearchMessage searchMessage = new SearchMessage(keyword);
                        objectOut.writeObject(searchMessage);

                        // Ler respostas
                        SearchResultsMessage response;
                        try {
                            response = (SearchResultsMessage) objectIn.readObject();
                            if (response != null && response.getType().equals("SEARCH_RESULTS")) {
                                results.addAll(response.getResults());
                                System.out.println("Resposta do nó " + connection.getIpAddress() + ":"
                                        + connection.getPort()
                                        + ": " + response.getMessage());
                            }
                        } catch (ClassNotFoundException e) {
                            System.out.println("Erro ao ler objeto: " + e.getMessage());
                        }
                    } catch (IOException e) {
                        System.out.println("Erro ao buscar no nó " + connection.getIpAddress() + ":"
                                + connection.getPort() + " - " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
            };
            threads.add(thread);
            thread.start();
        }

        return results;
    }

    // Método para solicitar download de ficheiros a nós conectados
    public Map<String, Integer> requestDownloadToNodes(String fileName) {
        // Verificar se o ficheiro já existe localmente
        File localFile = sharedFilesManager.getFileByName(fileName);
        if (localFile != null) {
            System.out.println("O ficheiro já existe localmente.");
            return null;
        }
        List<NodeConnection> nodesWithFile = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        List<String> responses = new ArrayList<>();
        for (NodeConnection connection : activeConnections) {
            Thread thread = new Thread() {
                public void run() {
                    try (Socket socket = new Socket(connection.getIpAddress(), connection.getPort());
                            ObjectOutputStream objectOut = new ObjectOutputStream(socket.getOutputStream());
                            ObjectInputStream objectIn = new ObjectInputStream(socket.getInputStream())) {

                        // Enviar pedido de download
                        DownloadMessage downloadMessage = new DownloadMessage(fileName);
                        objectOut.writeObject(downloadMessage);

                        // Ler respostas
                        DownloadResultMessage response;
                        try {
                            response = (DownloadResultMessage) objectIn.readObject();
                            if (response != null && response.getType().equals("DOWNLOAD_RESULT")) {
                                if (response.hasFile()) {
                                    String responseString = response.getChecksum() + ":" + response.getFileBlocks();
                                    responses.add(responseString);
                                    synchronized (nodesWithFile) {
                                        nodesWithFile.add(connection);
                                    }
                                }
                            }
                            System.out.println("Resposta do nó " + connection.getIpAddress() + ":"
                                    + connection.getPort() + ": " + response);
                        } catch (ClassNotFoundException e) {
                            System.out.println("Erro ao ler objeto: " + e.getMessage());
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
        List<FileBlockRequestMessage> fileBlocks = requestFileBlocksThreadPool(fileName, nodesWithFile, responses);
        if (fileBlocks == null) {
            throw new RuntimeException("Erro ao solicitar blocos de ficheiros.");
        }

        // Cria um dicionário, como key o originNode e como value um Integer que representa o total de blocos enviados
        Map<String, Integer> blocksSent = new HashMap<>();
        for (FileBlockRequestMessage block : fileBlocks) {
            NodeConnection originNode = block.getOriginNode();
            if (blocksSent.containsKey(originNode.toString())) {
                blocksSent.put(originNode.toString(), blocksSent.get(originNode.toString()) + 1);
            } else {
                blocksSent.put(originNode.toString(), 1);
            }
        }
        rebuildFile(fileBlocks);
        return blocksSent;
    }

    // Método para solicitar blocos de ficheiros a nós conectados
    public List<FileBlockRequestMessage> requestFileBlocksThreadPool(String fileName,
            List<NodeConnection> nodesWithFile,
            List<String> responses) {
        blockRequests.clear();
        List<Integer> blocksToRequest = new ArrayList<>();
        String fileChecksum = responses.get(0).split(":")[0];
        // Eliminar duplicados em responses
        responses = new ArrayList<>(new HashSet<>(responses));

        // Se ainda houver respostas diferentes dá erro
        if (responses.size() > 1) {
            System.out.println("Erro: múltiplas respostas diferentes para o mesmo ficheiro.");
            return null;
        }
        // Numero de blocos
        int totalBlocks = Integer.valueOf(responses.get(0).split(":")[1]);

        for (int i = 0; i < totalBlocks; i++) {
            blocksToRequest.add(i);
        }

        // Cria uma BlockingQueue para coordenar as tarefas
        BlockingQueue<Integer> blockQueue = new LinkedBlockingQueue<>(blocksToRequest);

        // Cria um ThreadPool com 5 threads
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);

        for (NodeConnection connection : nodesWithFile) {
            executor.submit(() -> {
                while (!blockQueue.isEmpty()) {
                    try {
                        Integer blockIndex = blockQueue.poll(1, TimeUnit.SECONDS);
                        if (blockIndex == null) {
                            break;
                        }
                        try (Socket socket = new Socket(connection.getIpAddress(), connection.getPort());
                                ObjectOutputStream objectOut = new ObjectOutputStream(socket.getOutputStream());
                                ObjectInputStream objectIn = new ObjectInputStream(socket.getInputStream())) {

                            // Enviar pedido de bloco
                            BlockMessage blockMessage = new BlockMessage(fileChecksum, blockIndex);
                            objectOut.writeObject(blockMessage);

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
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println("Erro ao esperar pelo bloco: " + e.getMessage());
                    }
                }
            });
        }

        // Desligar o ThreadPool
        executor.shutdown();

        // Esperar que todas as threads terminem
        try {
            executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            System.out.println("Erro ao esperar pelo ThreadPool: " + e.getMessage());
        }

        // Verificar se todos os blocos foram recebidos
        System.out.println("Blocos:");

        // Eliminar blockrequests com blockindex duplicados
        blockRequests = new ArrayList<>(new HashSet<>(blockRequests));

        // Contar o número de blocos não recebidos
        int missingBlocks = totalBlocks - blockRequests.size();

        // Organizar os blocos por ordem de blockindex
        blockRequests.sort(Comparator.comparing(FileBlockRequestMessage::getBlockIndex));
        if (missingBlocks > 0) {
            System.out.println("Erro: " + missingBlocks + " blocos não foram recebidos.");
            return null;
        }

        return blockRequests;
    }

    // Método para reconstruir o ficheiro a partir dos blocos
    private void rebuildFile(List<FileBlockRequestMessage> fileBlocks) {
        for (FileBlockRequestMessage block : fileBlocks) {
            String fileName = block.getFileName();
            String filePath = sharedFilesManager.getSharedFolderPath() + File.separator + fileName;
            try (FileOutputStream fos = new FileOutputStream(filePath, true)) {
                fos.write(block.getData());
            } catch (IOException e) {
                System.out.println("Erro ao reconstruir o ficheiro: " + e.getMessage());
            }
        }
    }

    public SharedFilesManager getSharedFilesManager() {
        return sharedFilesManager;
    }
}