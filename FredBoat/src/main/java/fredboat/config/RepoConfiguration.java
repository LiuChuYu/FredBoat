/*
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fredboat.config;

import com.google.gson.Gson;
import fredboat.config.property.BackendConfig;
import fredboat.db.repositories.api.*;
import fredboat.db.repositories.impl.rest.*;
import fredboat.main.BotController;
import fredboat.main.ShutdownHandler;
import fredboat.shared.constant.ExitCodes;
import fredboat.util.rest.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nullable;

/**
 * Created by napster on 24.02.18.
 * <p>
 * Create our entity repositories
 */
@Configuration
public class RepoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RepoConfiguration.class);

    private final BackendConfig backendConfig;
    private final Gson gson = new Gson();
    private final Http http = BotController.HTTP; //todo replace

    public RepoConfiguration(BackendConfig backendConfig, ShutdownHandler shutdownHandler) throws InterruptedException {
        this.backendConfig = backendConfig;

        log.info("Contacting the backend");
        Version apiVersion = null;
        int attempts = 0;
        Exception lastException = null;
        while (apiVersion == null && attempts < 100) { //total time is 100 sec
            try {
                String s = http.get(backendConfig.getHost() + "info/version").auth(backendConfig.getBasicAuth()).asString();
                apiVersion = gson.fromJson(s, Version.class);
            } catch (Exception ignored) {
                lastException = ignored;
                attempts++;
                Thread.sleep(1000);
            }
        }

        if (apiVersion == null) {
            log.error("Could not contact the backend. Please make sure it is started and configuration values are correct", lastException);
            shutdownHandler.shutdown(ExitCodes.EXIT_CODE_ERROR);
            return;
        }

        if (apiVersion.getNumber() >= RestRepo.API_VERSION) {
            log.info("Backend API version is v{}", apiVersion.getNumber());
        } else {
            log.error("Backend API is version v{}, but this FredBoat expects at least v{}", apiVersion.getNumber(), RestRepo.API_VERSION);
            shutdownHandler.shutdown(ExitCodes.EXIT_CODE_ERROR);
        }
    }

    @Bean
    public BlacklistRepo blacklistRepo() {
        return new RestBlacklistRepo(backendConfig.getHost(), http, gson, backendConfig.getBasicAuth());
    }

    @Bean
    public GuildConfigRepo guildConfigRepo() {
        return new RestGuildConfigRepo(backendConfig.getHost(), http, gson, backendConfig.getBasicAuth());
    }

    @Bean
    public GuildDataRepo guildDataRepo() {
        return new RestGuildDataRepo(backendConfig.getHost(), http, gson, backendConfig.getBasicAuth());
    }

    @Bean
    public GuildModulesRepo guildModulesRepo() {
        return new RestGuildModulesRepo(backendConfig.getHost(), http, gson, backendConfig.getBasicAuth());
    }

    @Bean
    public GuildPermsRepo guildPermsRepo() {
        return new RestGuildPermsRepo(backendConfig.getHost(), http, gson, backendConfig.getBasicAuth());
    }

    @Bean
    public PrefixRepo prefixRepo() {
        return new RestPrefixRepo(backendConfig.getHost(), http, gson, backendConfig.getBasicAuth());
    }

    @Nullable
    @Bean
    public SearchResultRepo searchResultRepo() {
        return new RestSearchResultRepo(backendConfig.getHost(), http, gson, backendConfig.getBasicAuth());
    }

    private static class Version {
        private int number = -1;

        public void setNumber(int number) {
            this.number = number;
        }

        public int getNumber() {
            return number;
        }
    }
}