package searchengine.services.lemma;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;

public class LucMorph {

    private static volatile LuceneMorphology luceneMorph;
    private LucMorph(){}
    static LuceneMorphology getLuceneMorph() throws IOException {
        if (luceneMorph == null) {
            synchronized (LucMorph.class) {
                if (luceneMorph == null){
                    luceneMorph = new RussianLuceneMorphology();
                }
            }
        }
        return luceneMorph;
    }
}
