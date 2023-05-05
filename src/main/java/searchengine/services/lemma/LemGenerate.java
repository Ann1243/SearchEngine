package searchengine.services.lemma;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import searchengine.repository.LemmaRepository;
import searchengine.model.Site;
import searchengine.model.Lemma;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.*;

@Component
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@AllArgsConstructor
public class LemGenerate {
    private final LemmaRepository lemmaRepository;

    public Lemma getLemma(String word, Site site) {

        if (checkRussian(word.toLowerCase(Locale.ROOT))) {
            return lemmaRepository.findLemmaByLemmaAndSite(word.toLowerCase(Locale.ROOT), site);
        }
        return null;
    }
    private boolean checkRussian(String word) {

        String russianAlphabet = "[а-яА-Я]+";

        if (!word.matches(russianAlphabet)) {
            return false;
        }
        List<String> wordBaseForm = null;
        try {
            wordBaseForm = LucMorph.getLuceneMorph().getMorphInfo(word);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return checkWrong(wordBaseForm.toString());
    }

    private static boolean checkWrong(String word) {
        return !word.contains("|n СОЮЗ") && !word.contains("|l ПРЕДЛ") &&
                !word.contains("|f МС-П") && !word.contains("|o МЕЖД") &&
                !word.contains("|e МС") && !word.contains("|Z") &&
                !word.contains("|B") && !word.contains("ВВОДН") &&
                !word.contains("ЧАСТ");
    }

    public static Map<String, Integer> create(String text) {

        Map<String, Integer> wordMap = new HashMap<>();
        for (String str : getSeparateWordsList(text)) {

            String word;
            try {
                word = LucMorph.getLuceneMorph().getNormalForms(str.toLowerCase(Locale.ROOT)
                        .replaceAll("\\p{Punct}", "")
                        .replaceAll("[^а-яё ]", "")
                        .replaceAll("ё", "е")).get(0);
                String infoAboutWord = LucMorph.getLuceneMorph().getMorphInfo(word).get(0);

                if (checkWrong(infoAboutWord)) {
                    Integer oldCount = wordMap.get(word);
                    if (oldCount == null) {
                        oldCount = 0;
                    }
                    wordMap.put(word, oldCount + 1);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return wordMap;
    }

    public static List<String> getSeparateWordsList(String text) {
        String[] words = text.split("[\\W\\d&&[^а-яА-Я]]+");
        List<String> list = new ArrayList<>();
        for (String word : words) {
            if (word.matches("[\\d[a-zA-Z_]]+") || word.equals("") || text.equals("")) {
                continue;
            }
            list.add(word);
        }
        return list;
    }
}
