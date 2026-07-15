package com.familog.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "familog")
public record FamilogProperties(
        String aiBaseUrl,
        String dataDir,
        boolean aiMock,
        String voiceScriptTemplate,
        String greetingTemplate
) {

    public String voiceScriptFor(String memberName) {
        return String.format(voiceScriptTemplate, memberName);
    }

    public String greetingFor(String memberName) {
        return String.format(greetingTemplate, memberName);
    }
}
