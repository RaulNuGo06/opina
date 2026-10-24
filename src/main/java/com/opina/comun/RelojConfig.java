package com.opina.comun;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Un reloj inyectable permite fijar la hora en los tests en lugar de depender de Instant.now(). */
@Configuration(proxyBeanMethods = false)
public class RelojConfig {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
