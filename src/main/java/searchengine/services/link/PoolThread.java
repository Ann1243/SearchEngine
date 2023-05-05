package searchengine.services.link;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import searchengine.config.Status;
import searchengine.model.Site;
import searchengine.repository.SiteRepository;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;

@RequiredArgsConstructor
@Getter
@Setter
public class PoolThread extends Thread {
    private final NewLink transition;
    private final List<ForkJoinPool> forkJoinPools;
    private final SiteRepository siteRepository;
    private ForkJoinPool pool;
    private final int numberThreads;

    @Override
    public void run() {
        Site site = transition.getSite();

        try {
            pool = new ForkJoinPool(numberThreads);
            forkJoinPools.add(pool);
            pool.execute(transition);
            transition.join();

            site.setStatusTime(new Date());
            site.setStatus(Status.INDEXED);
            siteRepository.save(site);

        } catch (RejectedExecutionException ex) {
            ex.printStackTrace();
            saveSite(site);
        }
    }
    public void saveSite(Site site) {
        site.setError("Индексация остановлена");
        site.setStatusTime(new Date());
        site.setStatus(Status.FAILED);
        siteRepository.save(site);
    }
}
