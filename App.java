import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class App extends JFrame {
    private SharedFilesManager sharedFilesManager;
    private DownloadTaskManager downloadManager;
    private ConnectionManager connectionManager;

    private JTextField searchField;
    private JButton searchButton;
    private DefaultListModel<String> resultArea;
    private JList<String> searchResultsList;
    private JButton downloadButton;
    private JButton connectButton;
    private JTextArea appInfo;

    // Construtor atualizado para receber a pasta compartilhada e a porta
    public App(String sharedFolderPath, int port) throws UnknownHostException {
        // Obter o IP local do computador
        String ipAddress = InetAddress.getLocalHost().getHostAddress();

        // Configurar as classes principais com o IP local e a porta
        sharedFilesManager = new SharedFilesManager(sharedFolderPath);
        downloadManager = new DownloadTaskManager(sharedFilesManager, ipAddress, port);
        connectionManager = new ConnectionManager(downloadManager);
        connectionManager.startServer();

        // Configurar a interface gráfica
        setTitle("Torrent App");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Painel de Pesquisa
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BorderLayout());
        searchField = new JTextField();
        searchButton = new JButton("Pesquisar");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchResults();
            }
        });
        searchPanel.add(new JLabel("Pesquisar Ficheiro: "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        // Área de Resultados
        resultArea = new DefaultListModel<>();
        searchResultsList = new JList<>(resultArea);
        JScrollPane resultScrollPane = new JScrollPane(searchResultsList);

        // Painel de Botões
        JPanel buttonPanel = new JPanel();
        downloadButton = new JButton("Descarregar Selecionado");
        connectButton = new JButton("Conectar a outro nó");
        appInfo = new JTextArea("IP: " + ipAddress + "\nPorta: " + port);
        appInfo.setEditable(false);
        appInfo.setBackground(UIManager.getColor("Label.background"));

        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadSelectedFile();
            }
        });

        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectToNode();
            }
        });

        buttonPanel.add(downloadButton);
        buttonPanel.add(connectButton);
        buttonPanel.add(appInfo);

        // Adicionar componentes à janela principal
        add(searchPanel, BorderLayout.NORTH);
        add(resultScrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public static String convertBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    private List<String> processResults(List<String> fileList) {
        // Map para contar as ocorrências de cada hash
        Map<String, Integer> hashCounts = new HashMap<>();

        // Contar ocorrências de cada hash
        for (String file : fileList) {
            String[] parts = file.split(":");
            String hash = parts[2];
            hashCounts.put(hash, hashCounts.getOrDefault(hash, 0) + 1);
        }

        // Adicionar o número de ocorrências e remover duplicados
        Set<String> resultSet = new LinkedHashSet<>();
        for (String file : fileList) {
            String[] parts = file.split(":");
            String name = parts[0];
            String size = parts[1];
            String hash = parts[2];
            int count = hashCounts.get(hash);

            // Construir a nova string com a contagem
            String updatedFile = String.format("%s:%s:%s:%d", name, size, hash, count);
            resultSet.add(updatedFile); // LinkedHashSet mantém a ordem e remove duplicados
        }

        // Retornar como lista
        return new ArrayList<>(resultSet);
    }

    private void searchResults() {
        String keyword = searchField.getText();

        if (keyword == null || keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Insira um termo para pesquisar!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Adicionar resultados locais à interface gráfica
        resultArea.clear();

        CountDownLatch latch = new CountDownLatch(downloadManager.getActiveConnections().size());
        // Buscar arquivos em nós conectados
        List<String> distributedResults = downloadManager.searchFilesInConnectedNodes(keyword, latch);
        System.out.println("Resultados distribuídos: " + distributedResults);

        // Aguardar até que todas as conexões retornem resultados
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Processar os resultados
        List<String> uniqueResults = processResults(distributedResults);

        // Adicionar resultados à interface gráfica
        for (String result : uniqueResults) {
            String name = result.split(":")[0];
            String size = result.split(":")[1];
            int count = Integer.parseInt(result.split(":")[3]);
            result = name + " | Tamanho: " + convertBytes(Long.parseLong(size)) + " | Nós: " + count;
            resultArea.addElement(result);
        }
    }

    private void downloadSelectedFile() {
        String selectedFile = searchResultsList.getSelectedValue();
        Map<String, Integer> downloadResults;
        if (selectedFile != null) {
            String fileName = selectedFile.split("\\|")[0].trim();
            try {
                downloadResults = downloadManager.requestDownloadToNodes(fileName);
                if (downloadResults.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Ficheiro '" + fileName + "' não encontrado!", "Erro",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro ao descarregar o ficheiro '" + fileName + "'", "Erro",
                        JOptionPane.ERROR_MESSAGE);
                sharedFilesManager.loadSharedFiles();
                return;
            }
            String message = "Ficheiro '" + fileName + "' descarregado com sucesso!\n\n";
            for (Map.Entry<String, Integer> entry : downloadResults.entrySet()) {
                message += "Nó: " + entry.getKey() + " | Blocos: "
                        + entry.getValue() + "\n";
            }
            JOptionPane.showMessageDialog(this, message, "Sucesso",
                    JOptionPane.INFORMATION_MESSAGE);
            sharedFilesManager.loadSharedFiles();
        } else {
            JOptionPane.showMessageDialog(this, "Selecione um ficheiro para descarregar!", "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void connectToNode() {
        String nodeIp = JOptionPane.showInputDialog(this, "Insira o IP do nó:");
        String nodePortStr = JOptionPane.showInputDialog(this, "Insira a porta do nó:");

        if (nodeIp != null && !nodeIp.isEmpty() && nodePortStr != null && !nodePortStr.isEmpty()) {
            try {
                int nodePort = Integer.parseInt(nodePortStr);
                downloadManager.connectToNode(nodeIp, nodePort);
                setTitle("Conectado ao nó: " + nodeIp + ":" + nodePort + "\n");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Porta inválida!", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "IP ou Porta inválidos!", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        // Recebe porta e pasta compartilhada como parâmetros
        if (args.length < 2) {
            System.out.println("Uso: java App.java <porta> <caminho_da_pasta>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        String sharedFolderPath = args[1];

        try {
            // Criar a GUI da aplicação
            App app = new App(sharedFolderPath, port);
            app.setVisible(true);
        } catch (UnknownHostException e) {
            JOptionPane.showMessageDialog(null, "Erro ao obter o IP do computador.", "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}