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

import com.thoughtworks.gocd.elasticagent.ecs.domain.ECSContainer;
import com.thoughtworks.gocd.elasticagent.ecs.domain.JobIdentifier;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static com.thoughtworks.gocd.elasticagent.ecs.domain.AWSModelMother.containerWith;
import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class AgentStatusReportViewTest {
    @Test
    void shouldCreateAgentStatusReport() throws ParseException, IOException, TemplateException {
        final JobIdentifier jobIdentifier = new JobIdentifier("up42", 1L, "foo", "up42_stage", "2", "up42_job", 2L);
        final ECSContainer container = containerWith(
                "arn/container-instance-1",
                "alpine-container", "alpine",
                100,
                200,
                "13/05/2017 12:55:00",
                "13/05/2017 12:56:30",
                jobIdentifier
        );

        final PluginStatusReportViewBuilder statusReportViewBuilder = PluginStatusReportViewBuilder.instance();
        final Template template = statusReportViewBuilder.getTemplate("agent-status-report.template.ftlh");
        final String view = statusReportViewBuilder.build(template, container);
        assertView(view, container, jobIdentifier);
    }

    private void assertView(String view, ECSContainer container, JobIdentifier jobIdentifier) {
        final Document document = Jsoup.parse(view);

        assertJobIdentifier(document, container.getName(), jobIdentifier);

        assertThat(document.select(".sub_tabs_container .tabs li")).hasSize(3);
        assertThat(document.select(".sub_tabs_container .tabs li").text()).isEqualTo("Details Environment Variables Log Configuration");
        assertThat(document.select(".tab-content-outer .container-details li label").text()).isEqualTo("Name Hostname Container Arn Task name Image Container Instance Arn CPU Units Max Memory(MB) Min Memory(MB) Privileged Docker Command Created At Started At Last status");

        final List<String> detailsProperties = document.select(".tab-content-outer .container-details li span").stream().map(e -> e.text()).collect(toList());
        assertThat(detailsProperties).containsExactly(container.getName(),
                container.getHostname(),
                container.getContainerArn(),
                container.getTaskName(),
                container.getImage(),
                container.getContainerInstanceArn(),
                container.getCpu().toString(),
                container.getMemory().toString(),
                container.getMemoryReservation().toString(),
                Boolean.toString(container.isPrivileged()),
                container.getDockerCommand(),
                toDateTimeString(container.getCreatedAt()),
                toDateTimeString(container.getStartedAt()),
                container.getLastStatus()
        );

        assertThat(document.select(".tab-content-outer .container-environment-vars ul").text()).isEqualTo("ENV_FOO ENV_FOO_VALUE");
        assertThat(document.select(".tab-content-outer .container-log-configuration ul").text()).isEqualTo("Log Driver awslogs Log Options log-group build-logs");
    }

    private void assertJobIdentifier(Document document, String name, JobIdentifier jid) {
        final Elements jobIdentifierUl = document.select(".ecs-plugin .status-report-page-header ul.entity_title");
        assertThat(jobIdentifierUl).isNotNull();

        final Elements allLisInUl = jobIdentifierUl.select("li");
        assertThat(allLisInUl).hasSize(5);

        final Element pipelineName = allLisInUl.first();
        assertThat(pipelineName.getAllElements().select("span").text()).isEqualTo("Pipeline");

        final Element linkToPipelineHistory = pipelineName.getAllElements().select("a").first();
        assertThat(linkToPipelineHistory.text()).isEqualTo(jid.getPipelineName());
        assertThat(linkToPipelineHistory.attributes()).containsOnlyOnce(
                new Attribute("href", pipelineHistoryPageLink(jid)),
                new Attribute("title", "View this pipeline's activity")
        );

        final Element pipelineRun = allLisInUl.get(1);
        assertThat(pipelineRun.getAllElements().select("span").first().text()).isEqualTo("Instance");
        assertThat(pipelineRun.getAllElements().select("span").get(1).text()).isEqualTo(jid.getPipelineCounter().toString());

        final Element linkToVSM = pipelineRun.getAllElements().select("a").first();
        assertThat(linkToVSM.text()).isEqualTo("VSM");
        assertThat(linkToVSM.attributes()).containsOnlyOnce(
                new Attribute("href", vsmLink(jid)),
                new Attribute("title", "View this stage's jobs summary")
        );


        final Element stageName = allLisInUl.get(2);
        assertThat(stageName.getAllElements().select("span").first().text()).isEqualTo("Stage");

        final Element linkToStageDetails = stageName.getAllElements().select("a").first();
        assertThat(linkToStageDetails.text()).isEqualTo(jid.getStageName() + " / " + jid.getStageCounter());
        assertThat(linkToStageDetails.attributes()).containsOnlyOnce(
                new Attribute("href", linkToStageDetailsPage(jid)),
                new Attribute("title", "View this stage's details")
        );


        final Element jobName = allLisInUl.get(3);
        assertThat(jobName.getAllElements().select("span").first().text()).isEqualTo("Job");

        final Element linkToConsoleLog = jobName.getAllElements().select("a").first();
        assertThat(linkToConsoleLog.text()).isEqualTo(format("{0}", jid.getJobName()));
        assertThat(linkToConsoleLog.attributes()).containsOnlyOnce(
                new Attribute("href", linkToConsoleLog(jid)),
                new Attribute("title", "View this job's details")
        );

        final Element elasticAgentId = allLisInUl.get(4);
        assertThat(elasticAgentId.getAllElements().select("span").first().text()).isEqualTo("Elastic Agent Id");
        assertThat(elasticAgentId.getAllElements().select("h1").first().text()).isEqualTo(name);
    }

    private String linkToConsoleLog(JobIdentifier jid) {
        return format("/go/tab/build/detail/{0}/{1}/{2}/{3}/{4}", jid.getPipelineName(), jid.getPipelineCounter(), jid.getStageName(), jid.getStageCounter(), jid.getJobName());
    }

    private String linkToStageDetailsPage(JobIdentifier jid) {
        return format("/go/pipelines/{0}/{1}/{2}/{3}", jid.getPipelineName(), jid.getPipelineCounter(), jid.getStageName(), jid.getStageCounter());
    }

    private String vsmLink(JobIdentifier jid) {
        return format("/go/pipelines/value_stream_map/{0}/{1}", jid.getPipelineName(), jid.getPipelineCounter());
    }

    private String pipelineHistoryPageLink(JobIdentifier jid) {
        return format("/go/tab/pipeline/history/{0}", jid.getPipelineName());
    }

    private String toDateTimeString(Date date) {
        return date == null ? "" : String.format("{{ %s | date:\"MMM dd, yyyy hh:mm:ss a\"}}", date.getTime());
    }
}
