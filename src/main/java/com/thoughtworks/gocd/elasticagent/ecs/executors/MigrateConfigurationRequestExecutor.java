/*
 * Copyright 2022 Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.gocd.elasticagent.ecs.executors;

import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.gocd.elasticagent.ecs.Constants;
import com.thoughtworks.gocd.elasticagent.ecs.RequestExecutor;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ClusterProfile;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfile;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import com.thoughtworks.gocd.elasticagent.ecs.requests.MigrateConfigurationRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;

public class MigrateConfigurationRequestExecutor implements RequestExecutor {
    private MigrateConfigurationRequest migrateConfigurationRequest;

    public MigrateConfigurationRequestExecutor(MigrateConfigurationRequest migrateConfigurationRequest) {
        this.migrateConfigurationRequest = migrateConfigurationRequest;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        LOG.info("[Migrate Config] Request for Config Migration Started...");

        PluginSettings pluginSettings = migrateConfigurationRequest.getPluginSettings();
        List<ClusterProfile> existingClusterProfiles = migrateConfigurationRequest.getClusterProfiles();
        List<ElasticAgentProfile> existingElasticAgentProfiles = migrateConfigurationRequest.getElasticAgentProfiles();

        if (!arePluginSettingsConfigured(pluginSettings)) {
            LOG.info("[Migrate Config] No Plugin Settings are configured. Skipping Config Migration...");
            return new DefaultGoPluginApiResponse(200, migrateConfigurationRequest.toJSON());
        }

        if (existingClusterProfiles.size() == 0) {
            LOG.info("[Migrate Config] Did not find any Cluster Profile. Possibly, user just have configured plugin settings and haven't define any elastic agent profiles.");
            String newClusterId = UUID.randomUUID().toString();
            LOG.info("[Migrate Config] Migrating existing plugin settings to new cluster profile '{}'", newClusterId);
            ClusterProfile clusterProfile = new ClusterProfile(newClusterId, Constants.PLUGIN_ID, pluginSettings);

            return getGoPluginApiResponse(pluginSettings, Collections.singletonList(clusterProfile), existingElasticAgentProfiles);
        }

        LOG.info("[Migrate Config] Checking to perform migrations on Cluster Profiles '{}'.", existingClusterProfiles.stream().map(ClusterProfile::getId).collect(Collectors.toList()));

        for (ClusterProfile clusterProfile : existingClusterProfiles) {
            List<ElasticAgentProfile> associatedElasticAgentProfiles = findAssociatedElasticAgentProfiles(clusterProfile, existingElasticAgentProfiles);
            if (associatedElasticAgentProfiles.size() == 0) {
                LOG.info("[Migrate Config] Skipping migration for the cluster '{}' as no Elastic Agent Profiles are associated with it.", clusterProfile.getId());
                continue;
            }

            if (!arePluginSettingsConfigured(clusterProfile.getClusterProfileProperties())) {
                List<String> associatedProfileIds = associatedElasticAgentProfiles.stream().map(ElasticAgentProfile::getId).collect(Collectors.toList());
                LOG.info("[Migrate Config] Found an empty cluster profile '{}' associated with '{}' elastic agent profiles.", clusterProfile.getId(), associatedProfileIds);
                migrateConfigForCluster(pluginSettings, associatedElasticAgentProfiles, clusterProfile);
            } else {
                LOG.info("[Migrate Config] Skipping migration for the cluster '{}' as cluster has already been configured.", clusterProfile.getId());
            }
        }

        return new DefaultGoPluginApiResponse(200, migrateConfigurationRequest.toJSON());
    }

    //this is responsible to copy over plugin settings configurations to cluster profile and if required rename no op cluster
    private void migrateConfigForCluster(PluginSettings pluginSettings, List<ElasticAgentProfile> associatedElasticAgentProfiles, ClusterProfile clusterProfile) {
        LOG.info("[Migrate Config] Coping over existing plugin settings configurations to '{}' cluster profile.", clusterProfile.getId());
        clusterProfile.setClusterProfileProperties(pluginSettings);

        if (clusterProfile.getId().equals(String.format("no-op-cluster-for-%s", Constants.PLUGIN_ID))) {
            String newClusterId = UUID.randomUUID().toString();
            LOG.info("[Migrate Config] Renaming dummy cluster profile from '{}' to '{}'.", clusterProfile.getId(), newClusterId);
            clusterProfile.setId(newClusterId);

            LOG.info("[Migrate Config] Changing all elastic agent profiles to point to '{}' cluster profile.", clusterProfile.getId());
            associatedElasticAgentProfiles.forEach(elasticAgentProfile -> elasticAgentProfile.setClusterProfileId(newClusterId));
        }
    }

    private List<ElasticAgentProfile> findAssociatedElasticAgentProfiles(ClusterProfile clusterProfile, List<ElasticAgentProfile> elasticAgentProfiles) {
        return elasticAgentProfiles.stream().filter(profile -> Objects.equals(profile.getClusterProfileId(), clusterProfile.getId())).collect(Collectors.toList());
    }

    private GoPluginApiResponse getGoPluginApiResponse(PluginSettings pluginSettings, List<ClusterProfile> clusterProfiles, List<ElasticAgentProfile> elasticAgentProfiles) {
        MigrateConfigurationRequest response = new MigrateConfigurationRequest();

        response.setPluginSettings(pluginSettings);
        response.setClusterProfiles(clusterProfiles);
        response.setElasticAgentProfiles(elasticAgentProfiles);

        return new DefaultGoPluginApiResponse(200, response.toJSON());
    }

    private boolean arePluginSettingsConfigured(PluginSettings pluginSettings) {
        return !StringUtils.isBlank(pluginSettings.getGoServerUrl());
    }
}
