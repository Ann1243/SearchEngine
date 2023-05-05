package searchengine.config;

import searchengine.model.Page;

import java.util.HashMap;
import java.util.Map;

public class IndexRank {
    public static class IndexRanks {
        private static float maxrabs = 0;
        private Page page;
        private final Map<String, Float> ranks;
        private float rabs;
        private float rrel;

        public IndexRanks() {
            ranks = new HashMap<>();
        }

        public Page getPage() {
            return page;
        }

        public void setPage(Page page) {
            this.page = page;
        }

        public Map<String, Float> getRanks() {
            return ranks;
        }

        public void setRanks(String word, Float rank) {
            ranks.put(word, rank);
        }

        public float getRabs() {
            return rabs;
        }

        public void setRabs() {
            ranks.forEach((key, value) -> {
                this.rabs += value;
            });

            if (this.rabs > maxrabs) {
                maxrabs = rabs;
            }
        }

        public float getRrel() {
            return rrel;
        }

        public void setRrel() {
            rrel = maxrabs / rabs;
        }
    }
}
