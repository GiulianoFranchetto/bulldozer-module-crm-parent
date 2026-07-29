package com.bulldozer_collective.modules.crm.configurations;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class OnShutdownListener {

    @Value("${spring.application.name}")
    private String applicationName;

    @PreDestroy
    void onDestroy() throws IOException {
        Files.createFile(Path.of(System.getProperty("user.dir"), this.applicationName + ".dead"));
    }
}
