package searchengine.dto.statistics;

import lombok.Data;


import java.util.Date;
@Data
public class DetailedStatisticsItem {
    private String uri;
    private String name;
    private String status;
    private Date statusTime;
    private String error;
    private int pages;
    private int lemmas;
}
