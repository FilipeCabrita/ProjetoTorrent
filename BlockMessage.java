public class BlockMessage extends Message {
    private String checksum;
    private int blockIndex;

    public BlockMessage(String checksum, int blockIndex) {
        super("BLOCK:" + checksum + ":" + blockIndex, "BLOCK");
        this.checksum = checksum;
        this.blockIndex = blockIndex;
    }

    public String getChecksum() {
        return checksum;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    @Override
    public String toString() {
        return "BLOCK: " + checksum + ":" + blockIndex;
    }
}
