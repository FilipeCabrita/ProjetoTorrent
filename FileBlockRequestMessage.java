// Classe que representa um pedido de bloco de ficheiro
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class FileBlockRequestMessage implements Serializable {
    private String fileName;
    private int blockIndex;
    private int blockSize;
    private byte[] data;
    private String fileChecksum;
    private String blockChecksum;

    public FileBlockRequestMessage(String fileName, int blockIndex, int blockSize, byte[] data, String fileChecksum) {
        this.fileName = fileName;
        this.blockIndex = blockIndex;
        this.blockSize = blockSize;
        this.data = data;
        this.fileChecksum = fileChecksum;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            this.blockChecksum = new BigInteger(1, hash).toString(16);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Erro ao criar instância de MessageDigest: " + e.getMessage());
        }
    }

    public String getFileName() {
        return fileName;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public byte[] getData() {
        return data;
    }

    public String getFileChecksum() {
        return fileChecksum;
    }

    public String getBlockChecksum() {
        return blockChecksum;
    }

    @Override
    public String toString() {
        return "FileBlockRequestMessage{" +
                "fileName='" + fileName + '\'' +
                ", blockIndex=" + blockIndex +
                ", blockSize=" + blockSize +
                ", fileChecksum='" + fileChecksum + '\'' +
                ", blockChecksum='" + blockChecksum + '\'' +
                '}';
    }
}