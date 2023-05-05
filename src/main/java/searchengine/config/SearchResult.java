package searchengine.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchResult {

    private String uri;
    private String title;
    private String snippet;
    private Float relevance;
    private String site;
    private String siteName;
}
