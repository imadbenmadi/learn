package dev.jvsp.taskapi;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// A @Bean method is how you register an object of a class you cannot annotate (here a JDK type).
// Injecting a Clock instead of calling Instant.now() makes time a dependency, so unit tests can pass
// a fixed clock.
//
// @Configuration (with the default proxyBeanMethods = true) makes Spring subclass this class with
// CGLIB, so that a call from one @Bean method to another returns the singleton instead of creating
// a new object. The dev-profile startup report shows the generated "$$SpringCGLIB$$" class name.
@Configuration
class TimeConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
