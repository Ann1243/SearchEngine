package searchengine.services.lemma;

import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import searchengine.model.Site;
import searchengine.model.Field;
import searchengine.model.Lemma;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@RequiredArgsConstructor
public class Snippet {
    private static final String TAGS_SCRIPT = "(<script.*>).*|\\s*(</script>)";
    private static final String REG_ALL_TAGS = "/</?[\\w\\s]*>|<.+[\\W]>/g";//"<!?\\/?[a-z\\s\"0-9=_]*>";
    private static final int SNIPPET_INTERVAL = 5;
    private static final int SNIPPET_MAX_LENGTH = 200;

    public static String snippet (Document content, SortedSet<Lemma> lemmas, List<Field> fields,
                                  LemGenerate generate, Site site) {
        StringBuilder snippetBuilder = new StringBuilder();
        Pattern pattern_script = Pattern.compile(TAGS_SCRIPT);
        for (Field field : fields) {
            Element element = content.select(field.getSelector()).first();
            String text = element.text();
            String contentStr = text;
            Matcher matcher = pattern_script.matcher(text);
            while (matcher.find()) {
                int start_script = matcher.start(1);
                int end_script = Math.max(matcher.end(2), matcher.end(1));
                text = text.replaceAll(contentStr.substring(start_script, end_script), "");
            }
            text = text.replaceAll(REG_ALL_TAGS, "");
            String nextOneFieldSnippet = snippetField(text, lemmas, generate, site);
            if (snippetBuilder.length() > 0 && nextOneFieldSnippet.length() > 0) {
                snippetBuilder.append("...");
            }
            snippetBuilder.append(nextOneFieldSnippet);
        }
        return snippetBuilder.toString();
    }

    private static String snippetField(String text, SortedSet<Lemma> lemmas, LemGenerate materialize, Site site) {
        StringBuilder snippetBuilder = new StringBuilder();

        List<String> words = LemGenerate.getSeparateWordsList(text);
        Set<String> wordsFound = new TreeSet<>();
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i).trim();
            Lemma lemmaWord = materialize.getLemma(word, site);
            if (word.length() == 0 || lemmaWord == null) {
                continue;
            }
            if (lemmas.stream().toList().contains(lemmaWord)) {
                wordsFound.add(word.replaceAll("[^a-zA-Zа-яА-ЯёЁ]", ""));
                int start = Math.max(i - SNIPPET_INTERVAL, 0);
                int end = Math.min(i + SNIPPET_INTERVAL, words.size());
                int totalLength = snippetBuilder.length() + List.of(words).subList(start, end)
                                        .stream().flatMapToInt(s -> IntStream.of(s.size())).sum();
                if (totalLength >= SNIPPET_INTERVAL) {
                    end = i;
                }
                snippetBuilder.append(start > 0 ? "..." : "");
                List.of(words).subList(start, end).forEach(w -> {
                    snippetBuilder.append(w).append(" ");
                });
                if (snippetBuilder.length() > SNIPPET_MAX_LENGTH) {
                    break;
                }
                snippetBuilder.append(end < words.size() ? "..." : "");
            }
        }

        String snippet = snippetBuilder.toString();
        if (snippet.length() > 0) {
            for (String word : wordsFound) {
                snippet = snippet.replaceAll(word, "<b>" + word + "</b>");
            }

        }
        return snippet;
    }
}
