// Classe que representa um pedido de bloco de ficheiro
class FileBlockRequestMessage {
    private String fileName;
    private int blockIndex;
    private int blockSize;

    public FileBlockRequestMessage(String fileName, int blockIndex, int blockSize) {
        this.fileName = fileName;
        this.blockIndex = blockIndex;
        this.blockSize = blockSize;
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

    public long getOffset() {
        return (long) blockIndex * blockSize;
    }

    @Override
    public String toString() {
        return "FileBlockRequestMessage{" +
                "fileName='" + fileName + '\'' +
                ", blockIndex=" + blockIndex +
                ", blockSize=" + blockSize +
                '}';
    }
}