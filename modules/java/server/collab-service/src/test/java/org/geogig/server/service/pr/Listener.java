package org.geogig.server.service.pr;

import java.util.ArrayList;
import java.util.List;

import org.geogig.server.service.pr.PullRequestWorkerService.InitEvent;
import org.geogig.server.service.pr.PullRequestWorkerService.PRStatusSchanged;
import org.geogig.server.service.pr.PullRequestWorkerService.PrepareStartEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

public @Component class Listener {//@formatter:off
    InitEvent initEvent;
    List<PrepareStartEvent> prepareStart = new ArrayList<>(); List<PRStatusSchanged> prepareEnd = new ArrayList<>();
    public @EventListener void init(InitEvent e){this.initEvent = e;}
    public @EventListener synchronized void prepareStart(PrepareStartEvent e){this.prepareStart.add(e);}
    public @EventListener synchronized void prepareEnd(PRStatusSchanged e){this.prepareEnd.add(e);}
    public void clear() {
        prepareStart.clear();prepareEnd.clear();
    }
}//@formatter:on