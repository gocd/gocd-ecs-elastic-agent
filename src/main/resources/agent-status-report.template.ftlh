<style>
  .main-container {
    margin-top: 0;
  }

  [data-plugin-style-id="ecs-plugin"] .ecs-elastic-agent {
    padding: 60px 20px 0 20px;
  }

  [data-plugin-style-id="ecs-plugin"] .sub-nav {
    padding-left: 25px;
  }

  [data-plugin-style-id="ecs-plugin"] .header {
    margin:          25px 0 10px 3px;
    font-weight:     600;
    text-decoration: underline;
  }

  [data-plugin-style-id="ecs-plugin"] .tabs {
    position: relative;
    clear:    both;
  }

  [data-plugin-style-id="ecs-plugin"] .tabs .tab {
    float: left;
  }

  [data-plugin-style-id="ecs-plugin"] .tabs .tab .content {
  }

  [data-plugin-style-id="ecs-plugin"] .tab label {
    background-color: #d1c4e9;
    padding:          7px 15px;
    line-height:      19px;
    font-size:        13px;
    border-radius:    3px 3px 0 0;
    font-weight:      600;
    color:            #333;
  }

  [data-plugin-style-id="ecs-plugin"] .tab-content.pod-logs {
    background-color: #383838;
    font-size:        13px;
    font-family:      monaco;
    color:            white;
    display:          block;
    font-weight:      400;
    line-height:      20px;
    padding-left:     10px;
  }

  [data-plugin-style-id="ecs-plugin"] .pod-configuration {
    background-color: white;
    font-family:      courier;
    font-size:        15px;
    white-space:      pre;
    font-weight:      600;
    display:          block;
    line-height:      10px;
    color:            #484848;
  }

  [data-plugin-style-id="ecs-plugin"] .tab-content {
    margin-top: 0;
    height:     calc(100vh - 280px);
    overflow-x: hidden;
  }

  [data-plugin-style-id="ecs-plugin"] .tab-content-outer {
    background: white;
    padding:    20px;
  }

  [data-plugin-style-id="ecs-plugin"] .warning-message {
    background-color: #fdf5e2;
    color:            #b88c34;
    font-weight:      600;
    font-size:        15px;
    padding:          10px 20px;
    border-radius:    3px;
    margin:           0 0 1rem 0;
    border:           1px solid rgba(10, 10, 10, 0.25);
    position:         relative;
  }

  [data-plugin-style-id="ecs-plugin"] .entity_title span.label {
    position:   absolute;
    margin-top: -10px;
    font-size:  0.625em;
  }

  [data-plugin-style-id="ecs-plugin"] .status-report-page-header {
    width:           100%;
    height:          50px;
    z-index:         10;
    position:        fixed;
    background:      #fff;
    box-shadow:      0 0 0 #000;
    border-bottom:   1px solid #ddd;
    background-size: cover;
  }

  [data-plugin-style-id="ecs-plugin"] .status-report-page-header .entity_title {
    top:      12px;
    left:     20px;
    position: absolute;
  }

  [data-plugin-style-id="ecs-plugin"] .status-report-page-header .entity_title .last h1 {
    margin-top: 2px !important;
  }

  [data-plugin-style-id="ecs-plugin"] .status-report-page-header .entity_title li a {
    font-size:   0.9375rem;
    font-weight: 600;
    line-height: normal;
  }

  [data-plugin-style-id="ecs-plugin"] textarea[readonly="readonly"], textarea[readonly] {
    cursor: auto;
  }

  [data-plugin-style-id="ecs-plugin"] .name-value {
    float:           none;
    list-style-type: none;
  }

  [data-plugin-style-id="ecs-plugin"] .name-value_pair {
    float:         none;
    display:       flex;
    border-bottom: 1px dotted #ddd;
  }

  [data-plugin-style-id="ecs-plugin"] .name-value_pair label {
    width:       300px;
    padding:     5px 10px 5px 0px;
    min-width:   150px;
    word-wrap:   break-word;
    font-size:   13px;
    font-weight: 600;
  }

  [data-plugin-style-id="ecs-plugin"] .name-value_pair span {
    padding:     5px 10px 5px 0px;
    font-size:   13px;
    line-height: 20px;
  }

  [data-plugin-style-id="ecs-plugin"] table {
    text-align:      left;
    border-spacing:  0;
    border-collapse: collapse;
  }

  [data-plugin-style-id="ecs-plugin"] table thead tr th {
    font-weight:   600;
    border-bottom: 2px solid #ddd;
  }

  [data-plugin-style-id="ecs-plugin"] .name-value_pair table tr th,
  [data-plugin-style-id="ecs-plugin"] .name-value_pair table tr td {
    padding:       8px 8px 8px 0px;
    border-bottom: 1px dotted #ddd;
  }

  [data-plugin-style-id="ecs-plugin"] table tr th,
  [data-plugin-style-id="ecs-plugin"] table tr td {
    padding:        8px;
    font-size:      13px;
    border-top:     0;
    line-height:    1.42857143;
    border-bottom:  1px solid #ddd;
    vertical-align: bottom;
  }

</style>

<div data-plugin-style-id="ecs-plugin" class="ecs-plugin">
  <div class="status-report-page-header">
    <ul class="entity_title">
      <li class="name">
        <span class="label">Pipeline</span>
        <a href="${jobIdentifier.pipelineHistoryPageLink!''}"
           title="View this pipeline's activity"
           rel="nofollow noreferrer"
           target="_blank">${jobIdentifier.pipelineName!''}</a>
      </li>
      <li class="pipeline_label">
        <span class="label">Instance</span>
        <span class="run_no">${jobIdentifier.pipelineCounter!''}</span>
        <a href="${jobIdentifier.vsmPageLink!''}"
           title="View this stage's jobs summary"
           rel="nofollow noreferrer" target="_blank">VSM</a>
      </li>
      <li class="stage_name">
        <span class="label">Stage</span>
        <a
          href="${jobIdentifier.stageDetailsPageLink!''}"
          title="View this stage's details"
          rel="nofollow noreferrer"
          target="_blank">${jobIdentifier.stageName!''} / ${jobIdentifier.stageCounter!''}</a>
      </li>
      <li class="job_label">
        <span class="label">Job</span>
        <a
          href="${jobIdentifier.jobDetailsPageLink!''}"
          title="View this job's details"
          rel="nofollow noreferrer" target="_blank">${jobIdentifier.jobName!''}</a>
      </li>

      <li class="last">
        <span class="label">Elastic Agent Id</span>
        <h1>${ name !}</h1>
      </li>
    </ul>
  </div>

  <div class="content_wrapper_inner ecs-elastic-agent">
    <div class="row">
      <div class="sub_tabs_container">
        <ul class="tabs" ng-init="currenttab=currentab || 'container-details'">
          <li ng-class="{'current_tab' : currenttab == 'container-details' }" ng-click="currenttab='container-details'">
            <a href="#">Details</a>
          </li>
          <li ng-class="{'current_tab' : currenttab == 'container-environment-vars' }"
              ng-click="currenttab='container-environment-vars'">
            <a href="#">Environment Variables</a>
          </li>
          <#if logConfiguration??>
          <li ng-class="{'current_tab' : currenttab == 'container-log-configuration' }"
              ng-click="currenttab='container-log-configuration'">
            <a href="#">Log Configuration</a>
          </li>
          </#if>
        </ul>
      </div>
      <div class="tab-content-outer">
        <div class="tab-content container-details" ng-show="currenttab == 'container-details'">
          <ul class="name-value">
            <li class="name-value_pair">
              <label>Name</label>
              <span>${ name !}</span>
            </li>
            <li class="name-value_pair">
              <label>Hostname</label>
              <span>${ hostname !}</span>
            </li>
            <li class="name-value_pair">
              <label>Container Arn</label>
              <span>${containerArn!}</span>
            </li>
            <li class="name-value_pair">
              <label>Task name</label>
              <span>${ taskName !}</span>
            </li>
            <li class="name-value_pair">
              <label>Image</label>
              <span>${ image !}</span>
            </li>
            <li class="name-value_pair">
              <label>Container Instance Arn</label>
              <span>${containerInstanceArn!}</span>
            </li>
            <li class="name-value_pair">
              <label>CPU Units</label>
              <span>${ cpu !}</span>
            </li>
            <li class="name-value_pair">
              <label>Max Memory(MB)</label>
              <span>${ memory !}</span>
            </li>
            <li class="name-value_pair">
              <label>Min Memory(MB)</label>
              <span>${ memoryReservation !}</span>
            </li>
            <li class="name-value_pair">
              <label>Privileged</label>
              <span>${ privileged?c}</span>
            </li>
            <li class="name-value_pair">
              <label>Docker Command</label>
              <span>${ dockerCommand!}</span>
            </li>
            <li class="name-value_pair">

              <label>Created At</label>
              <span>
              <#if createdAt??>
                {{ ${createdAt?long?c!} | date:"MMM dd, yyyy hh:mm:ss a"}}
              </#if>
              </span>
            </li>
            <li class="name-value_pair">
              <label>Started At</label>
              <span>
              <#if startedAt??>
                {{ ${startedAt?long?c!} | date:"MMM dd, yyyy hh:mm:ss a"}}
              </#if>
              </span>
            </li>
            <li class="name-value_pair">
              <label>Last status</label>
              <span>${ lastStatus !}</span>
            </li>
            <#if exitCode??>
            <li class="name-value_pair">
              <label>Container Exited</label>
              <span>
                Exit Code: ${exitCode!}<br/>
                Reason: ${reason!}
              </span>
            </li>
            </#if>

            <#if volumeMounts?size != 0>
            <li class="name-value_pair">
              <label>Volume Mounts</label>
              <table>
                <thead>
                <tr>
                  <th>Source Path</th>
                  <th>Container Path</th>
                </tr>
                </thead>
                <tbody>
                  <#list volumeMounts?keys as key>
                  <tr>
                    <td>${key!}</td>
                    <td>${volumeMounts[key]!}</td>
                  </tr>
                  </#list>
                </tbody>
              </table>
            </li>
            </#if>
          </ul>
        </div>
        <div class="tab-content container-environment-vars" ng-show="currenttab == 'container-environment-vars'">
          <ul class="name-value">
                <#list environments as environment>
                  <li class="name-value_pair">
                    <label>${environment.name}</label>
                    <span>${environment.value!}</span>
                  </li>
                <#else>
                    <li>No environment variable specified.</li>
                </#list>
          </ul>
        </div>
        <#if logConfiguration??>
        <div class="tab-content container-log-configuration" ng-show="currenttab == 'container-log-configuration'">
          <ul class="name-value">
            <li class="name-value_pair">
              <label>Log Driver</label>
              <span>${ logConfiguration.logDriver !}</span>
            </li>

            <li class="name-value_pair">
              <label>Log Options</label>
              <table>
                <#list logConfiguration.options?keys as key>
                  <tr>
                    <th><strong>${key}</strong></th>
                    <td>${logConfiguration.options[key]!}</td>
                  </tr>
                </#list>
              </table>
            </li>
          </ul>
        </div>
        </#if>
      </div>
    </div>
  </div>
</div>


