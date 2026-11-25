package dev.jvsp.taskapi;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Logs what ended up in the application context: how many bean definitions there are, and which of
// them are this application's own. @Profile("dev") means the bean definition is skipped entirely
// unless the dev profile is active, so production never creates it.
@Component
@Profile("dev")
class ContextStartupReport {

    private static final Logger log = LoggerFactory.getLogger(ContextStartupReport.class);

    // ApplicationReadyEvent is published after the context is refreshed, the web server has started
    // and all runners have run: the point where the application starts serving traffic.
    @EventListener
    void onReady(ApplicationReadyEvent event) {
        var context = event.getApplicationContext();
        String[] beanNames = context.getBeanDefinitionNames();
        log.info("Application context ready with {} bean definitions; this application's own beans:",
                beanNames.length);
        Arrays.stream(beanNames)
                .sorted()
                .forEach(name -> {
                    Class<?> type = context.getType(name);
                    if (type != null && type.getPackageName().startsWith("dev.jvsp")) {
                        log.info("  {} -> {}", name, type.getName());
                    }
                });
    }
}
