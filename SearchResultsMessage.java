import java.util.List;

public class SearchResultsMessage extends Message {
    private List<String> results;

    public SearchResultsMessage(List<String> results) {
        super("SEARCH_RESULTS:" + results, "SEARCH_RESULTS");
        this.results = results;
    }

    public List<String> getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "SEARCH_RESULTS:" + results;
    }
    
}
