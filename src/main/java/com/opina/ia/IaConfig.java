package com.opina.ia;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Con la variable ANTHROPIC_API_KEY la IA se activa; sin ella, la aplicación arranca igual
 * y el panel muestra las estadísticas sin clasificar ni resumir.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class IaConfig {

    private static final Logger log = LoggerFactory.getLogger(IaConfig.class);

    @Bean
    AsistenteIa asistenteIa(@Value("${opina.ia.api-key:}") String clave, @Value("${opina.ia.modelo}") String modelo) {
        if (clave.isBlank()) {
            log.warn("No hay ANTHROPIC_API_KEY: las opiniones no se clasificarán ni se resumirán");
            return new SinIa();
        }
        log.info("IA activa con el modelo {}", modelo);
        return new ClaudeAsistente(AnthropicOkHttpClient.builder().apiKey(clave).build(), modelo);
    }
}
