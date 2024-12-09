import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SharedFilesManager {
    private List<File> sharedFiles;
    private String sharedFolderPath;

    // Construtor que recebe o caminho da pasta a ser partilhada
    public SharedFilesManager(String sharedFolderPath) {
        this.sharedFolderPath = sharedFolderPath;
        this.sharedFiles = new ArrayList<>();
        loadSharedFiles();
    }

    // Método para carregar os ficheiros da pasta especificada
    private void loadSharedFiles() {
        sharedFiles.clear();
        File folder = new File(sharedFolderPath);
        
        // Verifica se o caminho é uma pasta válida
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            
            if (files != null) {
                for (File file : files) {
                    // Verifica se é um ficheiro e não uma subpasta
                    if (file.isFile()) {
                        sharedFiles.add(file);
                    }
                }
            }
        } else {
            System.out.println("O caminho especificado não é uma pasta válida.");
        }
    }

    // Método para obter o caminho da pasta partilhada
    public String getSharedFolderPath() {
        return sharedFolderPath;
    }

    // Método para obter a lista de ficheiros partilhados
    public List<File> getSharedFiles() {
        return sharedFiles;
    }

    // Método para realizar uma pesquisa por nome de ficheiro nos ficheiros partilhados
    public List<File> searchFiles(String query) {
        List<File> searchResults = new ArrayList<>();
        for (File file : sharedFiles) {
            if (file.getName().toLowerCase().contains(query.toLowerCase())) {
                searchResults.add(file);
            }
        }
        return searchResults;
    }

    // Método para obter um ficheiro pelo nome
    public File getFileByName(String fileName) {
        for (File file : sharedFiles) {
            if (file.getName().equals(fileName)) {
                return file;
            }
        }
        return null;
    }
}