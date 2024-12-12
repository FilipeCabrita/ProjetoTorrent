public class DownloadMessage extends Message {
    private String fileName;
    
    public DownloadMessage(String fileName) {
        super("DOWNLOAD:" + fileName,"DOWNLOAD");
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String toString() {
        return "DOWNLOAD:" + fileName;
    }
}
