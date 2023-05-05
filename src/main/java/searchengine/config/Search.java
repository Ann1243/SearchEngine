package searchengine.config;

import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.Set;

@Getter
@Setter
public class Search {
    private boolean result = true;
    private int count = 0;
    private Set<SearchResult> data = new TreeSet<>(Comparator.comparing(SearchResult::getRelevance));

    public void setData(Set<SearchResult> data) {
        this.data = data;
        setCount(data.size());
    }
}
