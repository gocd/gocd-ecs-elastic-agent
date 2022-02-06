/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.gocd.elasticagent.ecs.ECSTasks;
import com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceHelper;
import com.thoughtworks.gocd.elasticagent.ecs.aws.TaskHelper;
import com.thoughtworks.gocd.elasticagent.ecs.builders.PluginStatusReportViewBuilder;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ClusterProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ECSContainer;
import com.thoughtworks.gocd.elasticagent.ecs.events.Event;
import com.thoughtworks.gocd.elasticagent.ecs.events.EventFingerprint;
import com.thoughtworks.gocd.elasticagent.ecs.events.EventStream;
import com.thoughtworks.gocd.elasticagent.ecs.requests.ClusterStatusReportRequest;
import freemarker.template.Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ClusterStatusReportExecutorTest {

    private ClusterStatusReportRequest request;
    private ContainerInstanceHelper containerInstanceHelper;
    private ClusterProfileProperties clusterProfileProperties;
    private TaskHelper taskHelper;
    private ClusterStatusReportExecutor clusterStatusReportExecutor;
    private PluginStatusReportViewBuilder pluginStatusReportViewBuilder;
    private ECSTasks ecsTasks;
    private EventStream eventStream;

    @BeforeEach
    void setUp() {
        request = mock(ClusterStatusReportRequest.class);
        containerInstanceHelper = mock(ContainerInstanceHelper.class);
        clusterProfileProperties = mock(ClusterProfileProperties.class);
        when(clusterProfileProperties.isConfigured()).thenReturn(true);
        taskHelper = mock(TaskHelper.class);
        pluginStatusReportViewBuilder = mock(PluginStatusReportViewBuilder.class);
        eventStream = mock(EventStream.class);
        ecsTasks = mock(ECSTasks.class);

        clusterStatusReportExecutor = new ClusterStatusReportExecutor(request, ecsTasks, containerInstanceHelper, taskHelper, pluginStatusReportViewBuilder, eventStream);
    }

    @Test
    void shouldFetchClusterInformation() throws Exception {
        final List<ContainerInstance> containerInstances = Collections.emptyList();
        final List<Instance> ec2Instances = Collections.emptyList();
        final List<ECSContainer> ecsContainers = Collections.emptyList();
        final Cluster cluster = mock(Cluster.class);

        when(cluster.getRegisteredContainerInstancesCount()).thenReturn(2);
        when(request.clusterProfileProperties()).thenReturn(clusterProfileProperties);
        when(containerInstanceHelper.getCluster(clusterProfileProperties)).thenReturn(cluster);
        when(containerInstanceHelper.getContainerInstances(clusterProfileProperties)).thenReturn(containerInstances);
        when(containerInstanceHelper.ec2InstancesFromContainerInstances(clusterProfileProperties, containerInstances)).thenReturn(ec2Instances);
        when(taskHelper.allRunningContainers(clusterProfileProperties)).thenReturn(ecsContainers);
        when(pluginStatusReportViewBuilder.build(any(Template.class), anyMap())).thenReturn("plugin_health_html");

        clusterStatusReportExecutor.execute();

        verify(ecsTasks).refreshAll(clusterProfileProperties);
        verify(containerInstanceHelper).getCluster(clusterProfileProperties);
        verify(containerInstanceHelper).getContainerInstances(clusterProfileProperties);
        verify(containerInstanceHelper).ec2InstancesFromContainerInstances(eq(clusterProfileProperties), anyList());
        verify(taskHelper).allRunningContainers(clusterProfileProperties);
    }

    @Test
    void shouldNotFetchClusterInfoIfClusterHasNoContainerInstances() throws Exception {
        final Cluster cluster = mock(Cluster.class);

        when(cluster.getRegisteredContainerInstancesCount()).thenReturn(0);
        when(request.clusterProfileProperties()).thenReturn(clusterProfileProperties);
        when(containerInstanceHelper.getCluster(clusterProfileProperties)).thenReturn(cluster);
        when(pluginStatusReportViewBuilder.build(any(Template.class), anyMap())).thenReturn("plugin_health_html");

        clusterStatusReportExecutor.execute();

        verify(containerInstanceHelper).getCluster(clusterProfileProperties);
        verify(containerInstanceHelper, times(0)).getContainerInstances(clusterProfileProperties);
        verify(containerInstanceHelper, times(0)).ec2InstancesFromContainerInstances(eq(clusterProfileProperties), anyList());
        verifyNoInteractions(taskHelper);
    }

    @Test
    void shouldGenerateHealthView() throws Exception {
        final List<ContainerInstance> containerInstances = Collections.emptyList();
        final List<Instance> ec2Instances = Collections.emptyList();
        final List<ECSContainer> ecsContainers = Collections.emptyList();
        final Cluster cluster = mock(Cluster.class);

        when(request.clusterProfileProperties()).thenReturn(clusterProfileProperties);
        when(containerInstanceHelper.getCluster(clusterProfileProperties)).thenReturn(cluster);
        when(containerInstanceHelper.getContainerInstances(clusterProfileProperties)).thenReturn(containerInstances);
        when(containerInstanceHelper.ec2InstancesFromContainerInstances(clusterProfileProperties, containerInstances)).thenReturn(ec2Instances);
        when(taskHelper.allRunningContainers(clusterProfileProperties)).thenReturn(ecsContainers);
        when(pluginStatusReportViewBuilder.build(any(), anyMap())).thenReturn("plugin_health_html");

        final GoPluginApiResponse response = clusterStatusReportExecutor.execute();

        final String expectedJSON = "{\n" +
                "\"view\" : \"plugin_health_html\"\n" +
                "}";

        assertThat(response.responseCode()).isEqualTo(200);
        JSONAssert.assertEquals(expectedJSON, response.responseBody(), true);
    }

    @Test
    void shouldUpdateEventStreamClusterProfileIsNotConfiguredProperly() throws Exception {
        when(clusterProfileProperties.isConfigured()).thenReturn(false);
        clusterStatusReportExecutor.execute();
        verify(eventStream).update(Event.errorEvent(EventFingerprint.forStatusReport(), "Error accessing ECS cluster details", "Please configure the cluster profile properly before using the plugin."));
    }
}
