package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import searchengine.config.SearchResult;
import searchengine.config.SitesList;
import searchengine.config.Status;
import searchengine.model.*;
import searchengine.services.link.NodeLink;
import searchengine.services.link.PoolThread;
import searchengine.services.link.NewLink;
import searchengine.repository.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
@Service
@RequiredArgsConstructor
public class ApiService {
    private final static Log log = LogFactory.getLog(ApiService.class);
    private static final int THREADS = 3;
    private static final Map<String, Float> fields = new HashMap<>(Map.of("title", 1.0F, "body", 0.8F));
    private NewLink transition;
    private final FieldRepository fieldRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final SitesList sites;
    private List<Thread> threads;
    private List<ForkJoinPool> forkJoinPools;
    private void clearData() {
        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        siteRepository.deleteAll();
        fieldRepository.deleteAll();
    }
    private NewLink transition(Site site, NodeLink nodeLink) {
        transition = new NewLink(nodeLink, site.getUrl(), site, fieldRepository, siteRepository,
                                        indexRepository, pageRepository, lemmaRepository);
        transition.onStartIndexing();
        return transition;
    }
    public void fields(){
        for (String name : fields.keySet()) {
            Field field = fieldRepository.findByName(name);
            if (field == null) {
                field = new Field();
            }
            field.setName(name);
            field.setSelector(name);
            field.setWeight(fields.get(name));
            fieldRepository.save(field);
        }
    }
    public void indexing() {
        threads = new ArrayList<>();
        forkJoinPools = new ArrayList<>();
        NewLink.removeDataFromLemmasMap();
        clearData();

        List<NewLink> parses = new ArrayList<>();
        List<searchengine.config.Site> siteList = sites.getSites();
        log.info("Sites number " + siteList.size());

        fields();

        for (searchengine.config.Site valueSite : siteList) {
            String mainPage = valueSite.getUrl();

            Site site = siteRepository.findSiteByUrl(mainPage);

            if (site == null) {
                site = new Site();
            }

            site.setUrl(mainPage);
            site.setStatusTime(new Date());
            site.setStatus(Status.INDEXING);
            site.setError("");
            site.setName(valueSite.getName());
            NodeLink nodeLink = new NodeLink(site.getUrl());

            parses.add(transition(site, nodeLink));
            siteRepository.save(site);
        }

        for (NewLink parse : parses) {
            PoolThread thread = new PoolThread(parse, forkJoinPools, siteRepository, THREADS);
            thread.start();
            threads.add(thread);
        }
    }

    public boolean startIndexing() {
        AtomicBoolean isIndexing = new AtomicBoolean(false);

        siteRepository.findAll().forEach(site -> {
            if (site.getStatus().equals(Status.INDEXING) && !(forkJoinPools == null)) {
                if (!(forkJoinPools.size() == 0)) {
                    isIndexing.set(true);
                }
            }
        });

        if (isIndexing.get()) {
            return true;
        }
        new Thread(this::indexing).start();
        return false;
    }

    public boolean stopIndexing() {
        if (!(transition == null)) {
            transition.offStartIndexing();
        }
        AtomicBoolean isIndexing = new AtomicBoolean(false);

        siteRepository.findAll().forEach(site -> {
            if (site.getStatus().equals(Status.INDEXING)) {
                isIndexing.set(true);
            }
        });

        if (!isIndexing.get()) {
            return true;
        }

        for (ForkJoinPool pool : forkJoinPools) {
            pool.shutdownNow();
            try {
                isIndexing.set(pool.awaitTermination(5, TimeUnit.MINUTES));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        threads.forEach(Thread::interrupt);

        if (isIndexing.get()) {
            for (Site site : siteRepository.findAll()) {
                saveSite(site);
            }
        }
        threads.clear();
        forkJoinPools.clear();

        log.warn("Stop indexing");
        return false;
    }

    public void saveSite(Site site) {
        site.setError("Индексация остановлена");
        site.setStatus(Status.FAILED);
        site.setStatusTime(new Date());
        siteRepository.save(site);
    }

    public boolean indexPage(String url) {
        List<Site> siteList = siteRepository.findAll();
        fields();

        if (siteList.size() == 0) {
            List<searchengine.config.Site> configSites = sites.getSites();

            for (searchengine.config.Site configSite : configSites) {
                Page page = pageRepository.findByPath(url.replaceAll(configSite.getUrl(), ""));

                if (!(page == null)) {
                    return true;
                }

                if (url.contains(configSite.getUrl())) {
                    String mainPage = configSite.getUrl();

                    Site site = new Site();

                    site.setUrl(mainPage);
                    site.setStatusTime(new Date());
                    site.setError("");
                    site.setName(configSite.getName());
                    addPage(url, site);
                    return true;
                }
            }
        } else {
            for (Site site : siteList) {
                Page page = pageRepository.findByPath(url.replaceAll(site.getUrl(), ""));

                if (!(page == null)) {
                    return true;
                }
                if (url.contains(site.getUrl())) {
                    addPage(url, site);
                    return true;
                }
            }
        }
        return false;
    }

    public void addPage(String url, Site site) {
        site.setStatus(Status.INDEXING);
        siteRepository.save(site);
        NodeLink node = new NodeLink(url);
        NewLink parse = new NewLink(node, site.getUrl(), site,
                fieldRepository, siteRepository, indexRepository,
                pageRepository, lemmaRepository);
        parse.addPage(url);
        site.setStatus(Status.INDEXED);
        siteRepository.save(site);
    }

    public searchengine.config.Search search(String query, String site, int offset, int limit) {

        Search searchText = new Search(lemmaRepository);
        searchengine.config.Search search = searchText.search(query, siteRepository.findSiteByUrl(site), pageRepository,
                                                indexRepository, fieldRepository, siteRepository);
        if (search.getCount() < offset) {
            return new searchengine.config.Search();
        }

        if (search.getCount() > limit) {
            Set<SearchResult> searchResults = new TreeSet<>(Comparator.comparing(SearchResult::getRelevance));

            search.getData().forEach(it -> {
                if (searchResults.size() <= limit) {
                    searchResults.add(it);
                }
            });

            search.setData(searchResults);
        }
        return search;
    }
}
