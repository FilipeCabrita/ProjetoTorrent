public class SearchMessage extends Message {
    private String query;

    public SearchMessage(String query) {
        super("SEARCH:" + query, "SEARCH");
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return "SEARCH:" + query;
    }
    
}
