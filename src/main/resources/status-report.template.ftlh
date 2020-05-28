<style>
  /*Note: This is to make UI same across different version of GoCD*/
  .main-container {
    margin-top: 70px;
  }

  [data-plugin-style-id="ea-plugin"] .status-report-page-header {
    top:             40px;
    width:           100%;
    height:          50px;
    display:         flex;
    z-index:         10;
    position:        fixed;
    background:      #fff;
    box-shadow:      0 0 0 #000;
    border-bottom:   1px solid #ddd;
    background-size: cover;
  }

  [data-plugin-style-id="ea-plugin"] .page_title {
    font-size:   17px;
    font-weight: 600;
    margin:      0;
    padding:     0 30px;
    align-self:  center;
  }

  [data-plugin-style-id="ea-plugin"] .plugin-version-info {
    position:   absolute;
    padding:    0 30px;
    align-self: center;
    font-size:  12px;
    right:      0;
    color:      #555;
  }

  [data-plugin-style-id="ea-plugin"] .plugin-version-info label {
    color:       #555;
    display:     inline-block;
    font-size:   12px;
    font-weight: bold;
  }

  [data-plugin-style-id="ea-plugin"] .plugin-version-info span {
    padding: 4px;
  }

  [data-plugin-style-id="ea-plugin"] .right {
    float: right;
  }

  [data-plugin-style-id="ea-plugin"] .clear-fix:after {
    clear:   both;
    content: "";
    display: table;
  }

  [data-plugin-style-id="ea-plugin"] .clear-fix {
    clear: both;
  }

  [data-plugin-style-id="ea-plugin"] .ea-panel {
    margin-bottom: 15px;
  }

  [data-plugin-style-id="ea-plugin"] .ea-panel_header {
    padding:       10px 30px;
    background:    #d1c4e9;
    border-radius: 3px 3px 0 0;
  }

  [data-plugin-style-id="ea-plugin"] .ea-panel_header_details {
    font-weight: 600;
    font-size:   14px;
    line-height: 30px;
  }

  [data-plugin-style-id="ea-plugin"] .ea-panel_header_details * {
    margin:  0 20px 0 0;
    display: inline-block;
  }

  [data-plugin-style-id="ea-plugin"] .ea-panel_body {
    padding:    20px 30px;
    background: #fff;
  }

  [data-plugin-style-id="ea-plugin"] .ea-c-collapse {
    background:    #fff;
    position:      relative;
    border:        1px solid #ececec;
    border-radius: 3px;
    margin-bottom: 10px;
  }

  [data-plugin-style-id="ea-plugin"] .ea-c-collapse_header {
    cursor:      pointer;
    padding:     10px 50px 10px 30px;
    display:     flex;
    align-items: baseline;
  }

  [data-plugin-style-id="ea-plugin"] .ea-c-collapse.expanded {
    box-shadow: 0 3px 11px 0 rgba(0, 0, 0, 0.15);
  }

  [data-plugin-style-id="ea-plugin"] .ea-c-collapse.expanded .ea-c-collapse_header {
    border-bottom: 1px solid #ececec;
  }

  [data-plugin-style-id="ea-plugin"] .ea-c-collapse_body {
    padding: 20px 30px;
  }

  [data-plugin-style-id="ea-plugin"] ul.properties {
    margin:          0 0 20px 0;
    padding:         0;
    display:         flex;
    flex-wrap:       wrap;
    list-style-type: none;
    justify-content: space-between;
    width:           100%;
  }

  [data-plugin-style-id="ea-plugin"] ul.properties .property {
    float:         none;
    padding:       5px;
    box-sizing:    border-box;
    border-bottom: 1px dashed #e6e6e6;
  }

  [data-plugin-style-id="ea-plugin"] .properties.inline .property {
    width: 30.3%;
  }

  [data-plugin-style-id="ea-plugin"] .key {
    font-size:   0.8125rem;
    font-weight: 400;
    line-height: 30px;
  }

  [data-plugin-style-id="ea-plugin"] .value {
    font-weight: 600;
    font-size:   0.8125rem;
    line-height: 30px;
  }

  [data-plugin-style-id="ea-plugin"] .value:before {
    content:      ":";
    margin-right: 10px;
    font-weight:  600;
  }

  [data-plugin-style-id="ea-plugin"] .properties.inline .key {
    float:         left;
    clear:         left;
    width:         25%;
    max-width:     200px;
    word-break:    break-all;
    min-width:     130px;
    margin-bottom: 0;
    line-height:   1.8;
  }

  [data-plugin-style-id="ea-plugin"] .properties.inline .value {
    float:         left;
    position:      relative;
    padding-left:  15px;
    line-height:   1.8;
    max-width:     65%;
    display:       inline-block;
    text-overflow: ellipsis;
    overflow:      hidden;
    white-space:   nowrap;
  }

  [data-plugin-style-id="ea-plugin"] .cluster-properties .properties .property {
    width:  31%;
    margin: 0 15px;
  }

  [data-plugin-style-id="ea-plugin"] .cluster-properties .properties.inline .key {
    width:     50.0%;
    max-width: 253px;
  }

  [data-plugin-style-id="ea-plugin"] .containers {
    overflow-x: auto;
  }

  [data-plugin-style-id="ea-plugin"] .containers table {
    width:     100%;
    border:    1px solid #D8D8D8;
    font-size: 13px;
  }

  [data-plugin-style-id="ea-plugin"] .containers table thead {
    background: #b9f6ca;
  }

  [data-plugin-style-id="ea-plugin"] .containers table thead th {
    padding:     10px;
    text-align:  left;
    font-weight: bold;
  }

  [data-plugin-style-id="ea-plugin"] .containers table tbody tr {
    border-top:      1px solid #D8D8D8;
    border-bottom:   1px solid #D8D8D8;
    border-collapse: collapse;
  }

  [data-plugin-style-id="ea-plugin"] .containers table tbody td {
    padding:       10px;
    text-align:    left;
    white-space:   nowrap;
    max-width:     160px;
    text-overflow: ellipsis;
    overflow:      hidden;
  }

  [data-plugin-style-id="ea-plugin"] .fa {
    width:      14px;
    height:     14px;
    text-align: center;
  }

  [data-plugin-style-id="ea-plugin"] .warning {
    font-size:   13px;
    font-weight: 600;
  }

  [data-plugin-style-id="ea-plugin"] .event-stream .event {
    padding:       11px;
    margin-bottom: 10px;
    border-left:   2px solid;
  }

  [data-plugin-style-id="ea-plugin"] .event-stream .error {
    border-color: #f00;
  }

  [data-plugin-style-id="ea-plugin"] .event-stream .warning {
    border-color: #f7bc08;
  }

  [data-plugin-style-id="ea-plugin"] .event-stream .event:last-child {
    margin-bottom: 0px;
  }

  [data-plugin-style-id="ea-plugin"] .event-stream .event .event-header {
    font-size:   13px;
    font-weight: bold;
    overflow:    hidden;
    word-break:  break-all;
  }

  [data-plugin-style-id="ea-plugin"] .event-stream .error .event-description {
    margin-top:   7px;
    padding-left: 10px;
    text-align:   justify;
  }

  [data-plugin-style-id="ea-plugin"] .relative-time {
    font-size:   11px;
    font-weight: 600;
  }

  [data-plugin-style-id="ea-plugin"] .ellipsized {
    max-width:     140px;
    display:       inline-block;
    overflow:      hidden;
    text-overflow: ellipsis;
  }

  [data-plugin-style-id="ea-plugin"] .ellipsized .external-link {
    float:        right;
    margin-top:   -12px;
    margin-right: -10px;
  }

  [data-plugin-style-id="ea-plugin"] .icon {
    font-family:            'FontAwesome';
    font-weight:            normal;
    font-style:             normal;
    display:                inline-block;
    text-decoration:        inherit;
    line-height:            1em;
    margin:                 5px;
    color:                  #ddd;
    position:               absolute;
    right:                  10px;
    font-size:              30px;
    top:                    3px;
    -webkit-font-smoothing: antialiased;
  }

  [data-plugin-style-id="ea-plugin"] .icon.collapsed:before {
    content: "\f105";
  }

  [data-plugin-style-id="ea-plugin"] .icon.expanded:before {
    content: "\f107";
  }

  [data-plugin-style-id="ea-plugin"] .header {
    margin-bottom: 10px;
    font-size:     16px;
  }

  [data-plugin-style-id="ea-plugin"] .container_instance_arn {
    max-width:     50%;
    display:       inline-block;
    text-overflow: ellipsis;
    overflow:      hidden;
    white-space:   nowrap;
  }

  [data-plugin-style-id="ea-plugin"] .instance-state {
    display:         flex;
    width:           480px;
    background:      #D3DAFE;
    border-radius:   5px;
    justify-content: space-between;
    margin-left:     auto;
  }

  [data-plugin-style-id="ea-plugin"] .instance-state div {
    display: inline-block;
    padding: 0 10px;
  }

  @media only screen and (max-width: 1360px) and (min-width: 1024px) {
    [data-plugin-style-id="ea-plugin"] .cluster-properties .properties .property {
      width: 30%;
    }
  }

  @media only screen and (max-width: 1024px) {
    [data-plugin-style-id="ea-plugin"] .containers {
      overflow-x: scroll;
    }

    [data-plugin-style-id="ea-plugin"] .cluster-properties .properties .property {
      width: 100%;
    }

    [data-plugin-style-id="ea-plugin"] .ea-c-collapse_body .properties .property {
      width: 100%;
    }
  }
</style>

<div data-plugin-style-id="ea-plugin">
  <div class="status-report-page-header">
    <h1 class="page_title">ECS Plugin Status Report</h1>
    <#if fullVersion??>
      <div class="plugin-version-info">
        <label>Version: </label>
        <span>${fullVersion!''}</span>
      </div>
    </#if>
  </div>

<#if cluster??>
  <div class="ea-panel row expanded cluster">
    <div class="ea-panel_header clear-fix">
      <div class="ea-panel_header_details">
        <h5>Cluster: ${cluster.name}
          <a
            href="https://${region}.console.aws.amazon.com/ecs/home?region=${region}#/clusters/${cluster.name}/services"
            rel="nofollow noreferrer" target="_blank"><i class="fa fa-external-link" aria-hidden="true"></i></a>
        </h5>
      </div>
    </div>

    <div class="ea-panel_body">
      <#if cluster.containerInstances?size == 0>
        <div class="warning">No running container instances.</div>
      <#else>
      <div class="cluster-properties clear-fix">
        <ul class="properties inline">
          <li class="property">
            <label class="key">Max linux on-demand instances allowed</label>
            <span class="value">${cluster.maxLinuxInstancesAllowed}</span>
          </li>
          <li class="property">
            <label class="key">Max windows on-demand instances allowed</label>
            <span class="value">${cluster.maxWindowsInstancesAllowed}</span>
          </li>
          <li class="property">
            <label class="key">Max linux spot instances allowed</label>
            <span class="value">${cluster.maxLinuxSpotInstanceAllowed}</span>
          </li>
          <li class="property">
            <label class="key">Max windows spot instances allowed</label>
            <span class="value">${cluster.maxWindowsSpotInstanceAllowed}</span>
          </li>
          <li class="property">
            <label class="key">Running tasks</label>
            <span class="value">${cluster.runningTasksCount}</span>
          </li>
          <li class="property">
            <label class="key">Registered linux on-demand instances</label>
            <span class="value">${cluster.registeredLinuxOnDemandInstanceCount}</span>
          </li>
          <li class="property">
            <label class="key">Registered windows on-demand instances</label>
            <span class="value">${cluster.registeredWindowsOnDemandInstanceCount}</span>
          </li>
          <li class="property">
            <label class="key">Registered linux spot instances</label>
            <span class="value">${cluster.registeredLinuxSpotInstanceCount}</span>
          </li>
          <li class="property">
            <label class="key">Registered windows spot instances</label>
            <span class="value">${cluster.registeredWindowsSpotInstanceCount}</span>
          </li>
          <li class="property">
            <label class="key">Pending tasks</label>
            <span class="value">${cluster.pendingTasksCount}</span>
          </li>
        </ul>
      </div>
      </#if>
      <#list cluster.containerInstances as containerInstance>
          <#assign index = containerInstance?index>
          <#assign ngModel = "ContainerInstanceInfo${index}">
      <div class="ea-c-collapse" ng-init="${ngModel} = true" ng-class="{'expanded' : ${ngModel}}">
        <div class="ea-c-collapse_header" ng-click="${ngModel} = !${ngModel}">
          <label class="key inline">Container Instance ARN</label>
          <span class="value container_instance_arn">${containerInstance.containerInstanceArn!}</span>
          <a
            href="https://${region}.console.aws.amazon.com/ecs/home?region=${region}#/clusters/${cluster.name}/containerInstances/${containerInstance.id}"
            rel="nofollow noreferrer" target="_blank">
            <i class="fa fa-external-link" aria-hidden="true"></i>
          </a>
          <i class="icon expanded" aria-hidden="true" ng-show="${ngModel}"></i>
          <i class="icon collapsed" aria-hidden="true" ng-hide="${ngModel}"></i>
          <div class="right instance-state">
            <div>
              <label class="key inline">Platform</label>
              <span class="value">${containerInstance.platform?capitalize!}</span>
            </div>
            <div>
              <label class="key inline">State</label>
              <span class="value">${containerInstance.instance.state.name?capitalize!}</span>
            </div>
            <div>
              <label class="key inline">Instance Type</label>
              <span class="value">${cluster.ec2InstanceType[containerInstance.instance.instanceId]} &nbsp;&nbsp;</span>
            </div>
          </div>
        </div>
        <div class="ea-c-collapse_body" ng-model="${ngModel}" ng-value="false" ng-show="${ngModel}">
          <ul class="properties inline">
            <li class="property">
              <label class="key">EC2 Instance ID</label>
              <span class="value">${containerInstance.instance.instanceId!}
                <a
                  href="https://${region}.console.aws.amazon.com/ec2/v2/home?region=${region}#Instances:instanceId=${containerInstance.instance.instanceId};sort=instanceId"
                  rel="nofollow noreferrer" target="_blank">
                  <i class="fa fa-external-link" aria-hispanen="true"></i>
                </a>
              </span>
            </li>
            <li class="property">
              <label class="key">Status</label>
              <span class="value">${containerInstance.status!}</span>
            </li>
            <li class="property">
              <label class="key">Registered CPU</label>
              <span class="value">${containerInstance.registeredResources.cpu!}</span>
            </li>
            <li class="property">
              <label class="key">AMI</label>
              <span class="value">${containerInstance.instance.imageId!}</span>
            </li>
            <li class="property">
              <label class="key">ECS Agent Connected</label>
              <span class="value">${containerInstance.agentConnected?c}</span>
            </li>
            <li class="property">
              <label class="key">Remaining CPU</label>
              <span class="value">${containerInstance.remainingResources.cpu!}</span>
            </li>
            <li class="property">
              <label class="key">Instance Type</label>
              <span class="value">${containerInstance.instance.instanceType!}</span>
            </li>
            <li class="property">
              <label class="key">Docker Version</label>
              <span class="value">${containerInstance.dockerInfo.dockerVersion!}</span>
            </li>
            <li class="property">
              <label class="key">Registered Memory</label>
              <span class="value">${containerInstance.registeredResources.memory!}</span>
            </li>
            <li class="property">
              <label class="key">Launch Time</label>
              <span class="value">
                <#if containerInstance.instance.launchTime??>
                  {{ ${containerInstance.instance.launchTime?long?c!} | date:"MMM dd, yyyy hh:mm a"}}
                </#if>
              </span>
            </li>
            <li class="property">
              <label class="key">ECS Agent Version</label>
              <span class="value">${containerInstance.dockerInfo.agentVersion!}</span>
            </li>
            <li class="property">
              <label class="key">Remaining Memory</label>
              <span class="value">${containerInstance.remainingResources.memory!}</span>
            </li>
            <li class="property">
              <label class="key">Uptime Time</label>
              <span class="value">
                  ${containerInstance.runningSince!}
              </span>
            </li>
          </ul>

          <div class="containers clear-fix">
            <h4 class="header">Running Containers</h4>
            <table>
              <thead>
              <tr>
                <th>Name</th>
                <th>Job Identifier</th>
                <th>Image</th>
                <th>Status</th>
                <th>Hard/Soft<br/>Memory Limits (MB)</th>
                <th>CPU Units</th>
                <th>Created At</th>
                <th>Started At</th>
              </tr>
              </thead>
              <tbody>
                <#if containerInstance.containers??>
                    <#list containerInstance.containers as container>
                    <tr>
                      <td>
                        <span class="ellipsized" title="${container.name!}">${container.name!}</span>
                        <a rel="nofollow noreferrer" target="_blank" class="external-link"
                           href="https://${region}.console.aws.amazon.com/ecs/home?region=${region}#/clusters/${cluster.name}/tasks/${container.taskName}">
                          <i class="fa fa-external-link" aria-hidden="true"></i>
                        </a>
                      </td>
                      <td>
                        <a rel="nofollow noreferrer" title="${container.jobIdentifier.representation!}"
                           target="_blank"
                           href="/go/admin/status_reports/com.thoughtworks.gocd.elastic-agent.ecs/agent/${container.name!}?job_id=<#if container.jobIdentifier.jobId??>${container.jobIdentifier.jobId?c}</#if>">${container.jobIdentifier.representation!}</a>
                      </td>
                      <td><span title="${container.image!}">${container.image!}</span></td>
                      <td>${container.lastStatus!}</td>
                        <#assign memory = "${container.memory!} / ${container.memoryReservation!}">
                      <td>${memory!}</td>
                      <td>${container.cpu!}</td>
                      <td>
                        <span class="${container.createdSince!}">
                          <#if container.createdAt??>
                            {{ ${container.createdAt?long?c!} | date:"MMM dd, yyyy hh:mm a"}}
                            <span class="relative-time">(${container.createdSince!})</span>
                          </#if>
                        </span>
                      </td>
                      <td>
                          <span title="${container.startedSince!}">
                          <#if container.startedAt??>
                            {{ ${container.startedAt?long?c!} | date:"MMM dd, yyyy hh:mm a"}}
                            <span class="relative-time">(${container.startedSince!})</span>
                          </#if>
                          </span>
                      </td>
                    </tr>
                    </#list>
                <#else>
                <tr>
                  <td colspan="8" class="warning">No running containers.</td>
                </tr>
                </#if>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      </#list>
    </div>
  </div>
</#if>
<#if (errors?? && errors?size != 0) || (warnings?? && warnings?size != 0)>
      <div class="ea-panel event-stream row expanded">
        <div class="ea-panel_header ea-panel_header_details">Errors & Warnings</div>
        <div class="ea-panel_body">
            <#if errors?? && errors?size != 0>
                <#list errors as error>
                  <div class="event error">
                    <div class="event-header">${error.message!}</div>
                      <#if error.description??>
                        <div class="event-description">${error.description!}</div>
                      </#if>
                  </div>
                </#list>
            </#if>
            <#if warnings?? && warnings?size != 0>
                <#list warnings as warning>
                  <div class="event warning">
                    <div class="event-header">${warning.message!}</div>
                      <#if warning.description??>
                        <div class="event-description">${warning.description!}</div>
                      </#if>
                  </div>
                </#list>
            </#if>
        </div>
      </div>
</#if>
</div>
