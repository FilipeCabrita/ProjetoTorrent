public class DownloadResultMessage extends Message {
    private String checksum;
    private boolean hasFile;
    private int fileBlocks;
    
    public DownloadResultMessage(String checksum, boolean hasFile, int fileBlocks) {
        super("DOWNLOAD_RESULT:" + hasFile + ":" + checksum + ":" + fileBlocks, "DOWNLOAD_RESULT");
        this.checksum = checksum;
        this.hasFile = hasFile;
        this.fileBlocks = fileBlocks;
    }

    public String getChecksum() {
        return checksum;
    }

    public boolean hasFile() {
        return hasFile;
    }

    public int getFileBlocks() {
        return fileBlocks;
    }

    @Override
    public String toString() {
        return "DOWNLOAD_RESULT:" + hasFile + ":" + checksum + ":" + fileBlocks;
    }
}
