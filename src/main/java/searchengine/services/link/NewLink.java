package searchengine.services.link;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.services.lemma.LemGenerate;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.repository.FieldRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveTask;

@Component
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NewLink extends RecursiveTask<Integer> {
    private final static Log log = LogFactory.getLog(NewLink.class);
    private final NodeLink nodeLink;
    private String startPath;
    private final String mainPath;
    private static boolean isStopping;
    private static final Map<String, Float> fields = new HashMap<>();
    private static final ConcurrentHashMap<String, Lemma> lemmasMap = new ConcurrentHashMap<>();
    private final LemGenerate generate;
    private static PageRepository pageRepository;
    private static IndexRepository indexRepository;
    private static SiteRepository siteRepository;
    private static LemmaRepository lemmaRepository;
    private final Site site;
    public NewLink(NodeLink nodeLink, String sitePath, Site site, FieldRepository fieldRepository,
                          SiteRepository siteRepository, IndexRepository indexRepository,
                          PageRepository pageRepository, LemmaRepository lemmaRepository) {
        this.startPath = sitePath;
        this.nodeLink = nodeLink;

        this.mainPath = sitePath;
        generate = new LemGenerate(lemmaRepository);

        fieldRepository.findAll().forEach(it ->
                NewLink.fields.put(it.getName(), it.getWeight())
        );

        if (NewLink.indexRepository == null) {
            NewLink.indexRepository = indexRepository;
        }

        if (NewLink.pageRepository == null) {
            NewLink.pageRepository = pageRepository;
        }

        if (NewLink.siteRepository == null) {
            NewLink.siteRepository = siteRepository;
        }

        if (NewLink.lemmaRepository == null) {
            NewLink.lemmaRepository = lemmaRepository;
        }
        this.site = site;
    }

    public NewLink(NodeLink nodeLink, String startPath, Site site, String mainPath) {
        this.nodeLink = nodeLink;
        this.startPath = startPath;
        generate = new LemGenerate(lemmaRepository);

        this.mainPath = mainPath;
        this.site = site;
    }

    @Override
    protected Integer compute() {

            if (!startPath.endsWith("/")) {
                startPath += "/";
            }

            Set<String> links = Collections.synchronizedSet(nodeLink.parseLink(startPath));
            Set<NodeLink> nodeLinkSet = new CopyOnWriteArraySet<>();
            try {
                for (String link : links) {

                    if (pageRepository.findByPath(link) == null && !isStopping()) {
                        Document document = Jsoup.connect(mainPath + link).get();
                        addPage(document, link);
                        NodeLink nodeLink = new NodeLink(mainPath + link, document);
                        log.info(mainPath + link);
                        nodeLinkSet.add(nodeLink);
                    }
                }

            } catch (IOException | NullPointerException exception) {
                exception.fillInStackTrace();
            }

            List<NewLink> listTask = new ArrayList<>();
            for (NodeLink node : nodeLinkSet) {
                if (node.getLink().contains(mainPath) && !node.getLink().contains("#")) {
                    NewLink task = new NewLink(node, node.getLink(), site, mainPath);
                    task.fork();
                    listTask.add(task);
                }
            }
        return addResultsFromTasks(listTask);
    }

    public void addPage(String link)  {
        try {
            addPage(Jsoup.connect(link).get(), link.replaceAll(site.getUrl(), ""));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addPage(Document doc, String link) {
        Page page = new Page();
        int code = doc.connection().response().statusCode();
        page.setCode(code);
        page.setPath(link);
        page.setContent(doc.html());
        page.setSite(site);

        pageRepository.save(page);

        if (code < 400) {
            addLemmas(doc, page);
        }
    }
    public void addLemmas(Document doc, Page page) {

        ConcurrentHashMap<String, IndexModel> indices = new ConcurrentHashMap<>();
        Lemma oldLemma;
        for (String key : fields.keySet()) {
            Map<String, Integer> words = LemGenerate.create(doc.select(key).text());
            for (String word : words.keySet()) {
                oldLemma = lemmasMap.get(word);

                if (oldLemma == null) {
                    oldLemma = new Lemma();
                    oldLemma.setLemma(word);
                    oldLemma.setFrequency(1);
                    oldLemma.setSite(site);
                } else {
                    int freq = oldLemma.getFrequency();
                    oldLemma.setFrequency(freq + 1);
                }
                lemmaRepository.save(oldLemma);
                lemmasMap.put(word, oldLemma);

                addIndex(oldLemma, page, words, word, indices, key);
            }
        }
    }
    public void addIndex(Lemma lemma, Page page, Map<String, Integer> map,
                         String word, ConcurrentHashMap<String, IndexModel> indices, String key) {
        IndexModel index = indices.get(word);
        if (index == null) {
            IndexModel ind = new IndexModel();
            ind.setLemma(lemma);
            ind.setPage(page);
            ind.setLemmaRank(map.get(word) * fields.get(key));
            indexRepository.save(ind);
            indices.put(word, ind);
        } else {
            float rank = index.getLemmaRank();
            index.setLemmaRank(rank + map.get(word) * fields.get(key));
            indexRepository.save(index);
        }
    }

    private int addResultsFromTasks(List<NewLink> tasks) {
        int count = 0;
            for (NewLink item : tasks) {
                count += item.join();
            }
            return count;
    }
    public Site getSite() {
        return site;
    }

    public void onStartIndexing() {
        isStopping = false;
    }
    public void offStartIndexing() {
        isStopping = true;
    }

    public static boolean isStopping() {
        return isStopping;
    }

    public static void removeDataFromLemmasMap() {
        lemmasMap.clear();
    }
}