import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

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
                //searchFile();
                searchFileDistributed();
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

    private void searchFileDistributed() {
        String keyword = searchField.getText();
    
        if (keyword == null || keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Insira um termo para pesquisar!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
    
        // Buscar arquivos localmente
        List<File> localSearchResults = sharedFilesManager.searchFiles(keyword);
    
        // Adicionar resultados locais à interface gráfica
        resultArea.clear();
        for (File file : localSearchResults) {
            resultArea.addElement("Local | Nome: " + file.getName() + " | Tamanho: " + convertBytes(file.length()));
        }
    
        // Buscar arquivos em nós conectados
        List<String> distributedResults = downloadManager.searchFilesInConnectedNodes(keyword);
        for (String result : distributedResults) {
            resultArea.addElement(result);
        }
    }

    private void downloadSelectedFile() {
        String selectedFile = searchResultsList.getSelectedValue();
        if (selectedFile != null) {
            String fileName = selectedFile.split(" | ")[1];
            JOptionPane.showMessageDialog(this, "Ficheiro '" + fileName + "' descarregado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Selecione um ficheiro para descarregar!", "Erro", JOptionPane.ERROR_MESSAGE);
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