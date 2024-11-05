import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class P2PFileSharingGUI extends JFrame {
    
    private JTextField searchField;
    private JButton searchButton;
    private JList<String> searchResultsList;
    private JButton downloadButton;
    private JButton connectNodeButton;
    private DefaultListModel<String> searchResultsModel;

    public P2PFileSharingGUI() {
        setTitle("P2P File Sharing");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Painel principal
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        // Painel de Pesquisa
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BorderLayout());
        searchField = new JTextField();
        searchButton = new JButton("Pesquisar");
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        // Lista de resultados da pesquisa
        searchResultsModel = new DefaultListModel<>();
        searchResultsList = new JList<>(searchResultsModel);
        JScrollPane searchResultsScroll = new JScrollPane(searchResultsList);

        // Painel de botões
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        downloadButton = new JButton("Descarregar");
        connectNodeButton = new JButton("Conectar a Nó");
        buttonPanel.add(downloadButton);
        buttonPanel.add(connectNodeButton);

        // Adicionar os painéis ao painel principal
        mainPanel.add(searchPanel, BorderLayout.NORTH);
        mainPanel.add(searchResultsScroll, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Adicionar painel principal à janela
        add(mainPanel);

        // Ações dos botões
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchFiles();
            }
        });

        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadSelectedFile();
            }
        });

        connectNodeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectToNode();
            }
        });
    }

    private void searchFiles() {
        String query = searchField.getText();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Insira um termo de pesquisa.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Simulação de resultados de pesquisa (normalmente isso viria de uma pesquisa na rede P2P)
        searchResultsModel.clear();
        searchResultsModel.addElement("Ficheiro1.txt");
        searchResultsModel.addElement("Ficheiro2.pdf");
        searchResultsModel.addElement("Ficheiro3.mp3");
    }

    private void downloadSelectedFile() {
        String selectedFile = searchResultsList.getSelectedValue();
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this, "Selecione um ficheiro para descarregar.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Simulação de download do ficheiro (normalmente o download viria da rede P2P)
        JOptionPane.showMessageDialog(this, "Descarregando " + selectedFile + "...");
    }

    private void connectToNode() {
        // Painel para inserir endereço e porta
        JPanel panel = new JPanel(new GridLayout(2, 2));
        
        // Campos de entrada
        JTextField ipField = new JTextField();
        JTextField portField = new JTextField();

        // Adicionar componentes ao painel
        panel.add(new JLabel("Endereço IP:"));
        panel.add(ipField);
        panel.add(new JLabel("Porta:"));
        panel.add(portField);

        // Mostrar diálogo para inserir o IP e a porta
        int result = JOptionPane.showConfirmDialog(this, panel, "Conectar a Nó",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String ipAddress = ipField.getText();
            String port = portField.getText();

            if (ipAddress.isEmpty() || port.isEmpty()) {
                JOptionPane.showMessageDialog(this, "O endereço IP e a porta são obrigatórios.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Simulação de conexão com o nó (em um app real, aqui faria uma conexão com o nó P2P)
            JOptionPane.showMessageDialog(this, "Conectado ao nó " + ipAddress + ":" + port);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new P2PFileSharingGUI().setVisible(true);
            }
        });
    }
}