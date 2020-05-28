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

package com.thoughtworks.gocd.elasticagent.ecs.builders;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ECSCluster;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ECSContainer;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ECSContainerInstance;
import com.thoughtworks.gocd.elasticagent.ecs.domain.JobIdentifier;
import com.thoughtworks.gocd.elasticagent.ecs.events.Event;
import com.thoughtworks.gocd.elasticagent.ecs.events.EventFingerprint;
import freemarker.template.Template;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.amazonaws.services.ec2.model.InstanceType.C3Large;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceMother.containerInstance;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.InstanceMother.instance;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.AWSModelMother.*;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.assertj.core.api.Assertions.assertThat;

class PluginStatusReportViewTest {

    @Test
    void shouldBuildViewWithoutContainerInstanceAndPrintNoRunningContainerInstanceMessage() throws Exception {
        final Cluster cluster = clusterWith("GoCD", 0, 0, 0);

        final ECSCluster ecsCluster = new ECSCluster(cluster, emptyList(), emptyList(), emptyList(), 2, 3, 0, 0);
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("cluster", ecsCluster);
        dataModel.put("errors", Collections.emptyList());
        dataModel.put("region", "us-east-2");

        final PluginStatusReportViewBuilder statusReportViewBuilder = PluginStatusReportViewBuilder.instance();
        final Template template = statusReportViewBuilder.getTemplate("status-report.template.ftlh");
        final String view = statusReportViewBuilder.build(template, dataModel);

        assertView(view, ecsCluster);
    }

    @Test
    void shouldBuildStatusReportView() throws Exception {
        final Cluster cluster = clusterWith("GoCD", 5, 10, 0);
        final ContainerInstance containerInstance = containerInstance(
                "instance-id-1",
                "arn/container-instance-1",
                "ACTIVE",
                8,
                4096,
                4,
                1024
        );
        final Instance instance = instance("instance-id-1", C3Large, "ami-2dad3da", toDate("13/05/2017 12:50:20"));
        final ECSContainer alpineContainer = containerWith("arn/container-instance-1", "alpine-container", "alpine", 100, 200, "13/05/2017 12:55:00", "13/05/2017 12:56:30",
                new JobIdentifier("up42", 1L, "foo", "up42_stage", "2", "up42_job", 25632868237L));

        final ECSCluster ecsCluster = new ECSCluster(cluster, singletonList(containerInstance), singletonList(instance), singletonList(alpineContainer), 2, 3, 0, 0);
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("cluster", ecsCluster);
        dataModel.put("errors", Collections.emptyList());
        dataModel.put("region", "us-east-2");

        final PluginStatusReportViewBuilder statusReportViewBuilder = PluginStatusReportViewBuilder.instance();
        final Template template = statusReportViewBuilder.getTemplate("status-report.template.ftlh");
        final String view = statusReportViewBuilder.build(template, dataModel);

        assertThat(view).contains("/go/admin/status_reports/com.thoughtworks.gocd.elastic-agent.ecs/agent/alpine-container?job_id=25632868237");
        assertView(view, ecsCluster);
    }

    @Test
    void shouldBuildStatusReportViewWithDefaultValues() throws Exception {
        final Cluster cluster = clusterWith("GoCD", 0, 0, 0);
        final ContainerInstance containerInstance = containerInstance(
                "instance-id",
                "arn/container-instance-1",
                "ACTIVE",
                0,
                0,
                0,
                0
        );
        final Instance instance = instance("instance-id", C3Large, "ami-23456", toDate("13/05/2017 12:50:20"));
        final ECSContainer alpineContainer = containerWith("arn/container-instance-1", "container-name", "alpine", 100, 200, "13/05/2017 12:55:20", null);

        final ECSCluster ecsCluster = new ECSCluster(cluster, singletonList(containerInstance), singletonList(instance), singletonList(alpineContainer), 2, 3, 0, 0);
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("cluster", ecsCluster);
        dataModel.put("maxAllowedInstances", 5);
        dataModel.put("errors", Collections.emptyList());
        dataModel.put("region", "us-east-2");

        final PluginStatusReportViewBuilder statusReportViewBuilder = PluginStatusReportViewBuilder.instance();
        final Template template = statusReportViewBuilder.getTemplate("status-report.template.ftlh");
        final String view = statusReportViewBuilder.build(template, dataModel);

        assertView(view, ecsCluster);
    }

    @Test
    void shouldBuildViewWithErrors() throws Exception {
        Map<String, List<Event>> dataModel = new HashMap<>();
        dataModel.put("errors", asList(Event.errorEvent(EventFingerprint.forStatusReport(), "Error-Title", "Error-Description")));
        dataModel.put("warnings", asList(Event.warningEvent(EventFingerprint.forStatusReport(), "Warning-Title", "Warning-Description")));

        final PluginStatusReportViewBuilder statusReportViewBuilder = PluginStatusReportViewBuilder.instance();
        final Template template = statusReportViewBuilder.getTemplate("status-report.template.ftlh");
        final String view = statusReportViewBuilder.build(template, dataModel);

        assertEventStreamView(view, "error", dataModel.get("errors"));
        assertEventStreamView(view, "warning", dataModel.get("warnings"));
    }

    @Test
    void shouldNotGenerateEventStreamViewIfNoErrorsAndNoWarnings() throws Exception {
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("errors", Collections.emptyList());
        dataModel.put("warnings", Collections.emptyList());

        final PluginStatusReportViewBuilder statusReportViewBuilder = PluginStatusReportViewBuilder.instance();
        final Template template = statusReportViewBuilder.getTemplate("status-report.template.ftlh");
        final String view = statusReportViewBuilder.build(template, dataModel);

        Document document = Jsoup.parse(view);

        final Elements eventStream = document.select(".event-stream");

        assertThat(eventStream).hasSize(0);
    }

    private void assertEventStreamView(String view, String type, List<Event> events) {
        final Document document = Jsoup.parse(view);

        final Elements errorHeaders = document.select(format(".event-stream .%s.event .event-header", type));
        final Elements errorDescriptions = document.select(format(".event-stream .%s.event .event-description", type));

        assertThat(errorHeaders).hasSameSizeAs(errorDescriptions).hasSameSizeAs(events);

        IntStream.range(0, events.size()).forEach(index -> {
            Event error = events.get(index);

            assertThat(errorHeaders.get(index).text()).isEqualTo(error.getMessage());
            assertThat(errorDescriptions.get(index).text()).isEqualTo(error.getDescription());
        });

    }

    private void assertView(String view, ECSCluster cluster) {
        final Document document = Jsoup.parse(view);
        assertThat(document.select(".cluster .ea-panel_header_details h5").text())
                .isEqualTo(format("Cluster: %s", cluster.getName()));

        if (cluster.getContainerInstances().isEmpty()) {
            final Elements warningMessage = document.select(".cluster .ea-panel_body");
            assertThat(warningMessage.text()).isEqualTo("No running container instances.");
        } else {
            cluster.getContainerInstances().stream().forEach(containerInstance -> assertContainerInstanceView(document, containerInstance));
        }

    }

    private void assertContainerInstanceView(Document document, ECSContainerInstance containerInstance) {
        final Elements containerInstanceHeader = document.select(".cluster .ea-panel_body .ea-c-collapse_header");

        final String expectedHeader = format("Container Instance ARN %s Platform %s State %s", containerInstance.getContainerInstanceArn(), capitalize(containerInstance.getPlatform().toLowerCase()), capitalize(containerInstance.getInstance().getState().getName()));

        assertThat(containerInstanceHeader.get(0).text()).contains(expectedHeader);

        assertContainerInstanceProperties(document, containerInstance);
        assertEC2InstanceProperties(document, containerInstance);
        assertContainersView(document, containerInstance.getContainers());
    }

    private void assertEC2InstanceProperties(Document document, ECSContainerInstance containerInstance) {
        final Elements ec2InstanceProperties = document.select(".cluster .ea-c-collapse_body .properties");

        assertProperty(ec2InstanceProperties, "EC2 Instance ID", containerInstance.getInstance().getInstanceId());
        assertProperty(ec2InstanceProperties, "AMI", containerInstance.getInstance().getImageId());
        assertProperty(ec2InstanceProperties, "Instance Type", containerInstance.getInstance().getInstanceType());
        assertPropertyStartWith(ec2InstanceProperties, "Launch Time", toDateTimeString(containerInstance.getInstance().getLaunchTime()));
    }

    private void assertContainerInstanceProperties(Document document, ECSContainerInstance containerInstance) {
        final Elements containerInstanceProperties = document.select(".cluster .ea-c-collapse_body .properties");

        assertProperty(containerInstanceProperties, "Status", containerInstance.getStatus());
        assertProperty(containerInstanceProperties, "ECS Agent Connected", String.valueOf(containerInstance.getAgentConnected()));
        assertProperty(containerInstanceProperties, "Registered CPU", containerInstance.getRegisteredResources().getCpu());
        assertProperty(containerInstanceProperties, "Remaining CPU", containerInstance.getRemainingResources().getCpu());
        assertProperty(containerInstanceProperties, "Registered Memory", containerInstance.getRegisteredResources().getMemory());
        assertProperty(containerInstanceProperties, "Remaining Memory", containerInstance.getRemainingResources().getMemory());
        assertProperty(containerInstanceProperties, "Docker Version", containerInstance.getDockerInfo().getDockerVersion());
        assertProperty(containerInstanceProperties, "ECS Agent Version", containerInstance.getDockerInfo().getAgentVersion());
    }

    private void assertContainersView(Document document, List<ECSContainer> containers) {
        final Elements headers = document.select(".cluster .ea-c-collapse_body .containers thead tr th");
        final String[] expectedHeaders = {"Name", "Job Identifier", "Image", "Status", "Hard/Soft Memory Limits (MB)", "CPU Units", "Created At", "Started At"};

        final List<String> actualHeaders = headers.stream().map(Element::text).collect(Collectors.toList());
        assertThat(actualHeaders).hasSize(expectedHeaders.length);
        assertThat(actualHeaders).contains(expectedHeaders);

        final Elements containerElements = document.select(".cluster .ea-c-collapse_body .containers tbody tr");

        IntStream.range(0, containers.size()).forEach(index -> {
            ECSContainer container = containers.get(index);
            final Element containerElement = containerElements.get(index);

            assertThat(elementAt(containerElement, "td", 1).text()).isEqualTo(container.getName());
            assertThat(elementAt(containerElement, "td", 2).text()).isEqualTo(container.getJobIdentifier().getRepresentation());
            assertThat(elementAt(containerElement, "td", 3).text()).isEqualTo(container.getImage());
            assertThat(elementAt(containerElement, "td", 4).text()).isEqualTo(container.getLastStatus());
            assertThat(elementAt(containerElement, "td", 5).text()).isEqualTo(format("%s / %s", container.getMemory(), container.getMemoryReservation()));
            assertThat(elementAt(containerElement, "td", 6).text()).isEqualTo(Integer.toString(container.getCpu()));
            assertThat(elementAt(containerElement, "td", 7).text()).startsWith(toDateTimeString(container.getCreatedAt()));
            assertThat(elementAt(containerElement, "td", 8).text()).startsWith(toDateTimeString(container.getStartedAt()));
        });

    }

    private Elements elementAt(Element containerElement, String tagName, int index) {
        return containerElement.getAllElements().select(format("%s:nth-child(%s)", tagName, index));
    }

    private void assertProperty(Elements allProperties, String key, String value) {
        final Elements keyElements = allProperties.select(format("label.key:contains(%s)", key));
        assertThat(keyElements).isNotNull();
        assertThat(keyElements.next().text()).describedAs("Asserting property %s", key).isEqualTo(value);
    }

    private void assertPropertyStartWith(Elements allProperties, String key, String value) {
        final Elements keyElements = allProperties.select(format("label.key:contains(%s)", key));
        assertThat(keyElements).isNotNull();
        assertThat(keyElements.next().text()).describedAs("Asserting property %s", key).startsWith(value);
    }

    private void assertProperty(Elements allProperties, String key, Double value) {
        final Elements keyElements = allProperties.select(format("label.key:contains(%s)", key));
        assertThat(keyElements).isNotNull();
        assertThat(Double.parseDouble(keyElements.next().text().replaceAll(",", ""))).describedAs("Asserting property %s", key).isEqualTo(value);
    }

    private String toDateTimeString(Date date) {
        return date == null ? "" : format("{{ %s | date:\"MMM dd, yyyy hh:mm a\"}}", date.getTime());
    }
}
