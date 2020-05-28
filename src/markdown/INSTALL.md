# GoCD Elastic agent plugin for AWS

Elastic Agent Plugin for *Amazon EC2 Container Service* allows for optimal use of EC2 instances thereby increasing utilization and reducing cost.

It allows flexible scaling and in many cases. Imagine an automated performance test which runs occasionally. Instances to perform builds can be started at the beginning of the performance test, and then brought down when not needed. This feature should enable a more flexible and dynamic build grid.

## Requirements

* GoCD server version v17.2.0 or above
* AWS account with permissions to manage an ECS cluster. The following IAM permissions are needed:
  * `AmazonEC2ContainerServiceFullAccess`
* Running cluster on ECS. The cluster should be empty, since the plugin will manage instances in this cluster.

## Installation

Copy the file `dist/ecs-elastic-agent-plugin-VERSION.jar` to the GoCD server under `${GO_SERVER_DIR}/plugins/external` and restart the server. The `GO_SERVER_DIR` is usually `/var/lib/go-server` on Linux and `C:\Program Files\Go Server` on Windows.

## Configuration

#### GoCD server configuration

* Navigate to *Admin > Plugin* in the main menu.
* Click settings next to the plugin ECS plugin and enter the required plugin configuration.

#### Create Elastic Agent Profile for ECS
   
* Navigate to *Admin > Elastic Agent Profiles* in the main menu.
* Click the *Add* button to create a new elastic agent profile.
  * Specify unique id for this profile.
  * Select the plugin id `com.thoughtworks.gocd.elastic-agent.ecs` from the dropdown.
  * Specify the docker container properties and the elastic container instance properties on which the elastic agent should run on.    
   
#### Create Job for Elastic Agents

* Click the gear icon on *Pipeline*. 
* Click on *Quick Edit* button.
* Click on *Stages*.
* Create/Edit a job, in the job settings enter the unique id of the elastic profile you created above.
* Save your changes

## Using your own docker image with elastic agents

The plugin executes the equivalent of the following docker command to start the agent —

```bash
docker run -e GO_EA_SERVER_URL=...
           -e GO_EA_AUTO_REGISTER_KEY=...
           -e GO_EA_AUTO_REGISTER_ENVIRONMENT=...
           -e GO_EA_AUTO_REGISTER_ELASTIC_AGENT_ID=...
           -e GO_EA_AUTO_REGISTER_ELASTIC_PLUGIN_ID=...
           ...
           IMAGE_ID
```

Your docker image is expected to contain a bootstrap program (to be executed via docker's `CMD`) that will create an [`autoregister.properties`](https://docs.gocd.io/current/advanced_usage/agent_auto_register.html) file using these variables. The `GO_EA_SERVER_URL` will point to the server url that the agent must communicate with.

Here is an example shell script to do this —

```bash
# write out autoregister.properties
(
cat <<EOF
agent.auto.register.key=${GO_EA_AUTO_REGISTER_KEY}
agent.auto.register.environments=${GO_EA_AUTO_REGISTER_ENVIRONMENT}
agent.auto.register.elasticAgent.agentId=${GO_EA_AUTO_REGISTER_ELASTIC_AGENT_ID}
agent.auto.register.elasticAgent.pluginId=${GO_EA_AUTO_REGISTER_ELASTIC_PLUGIN_ID}
EOF
) > /var/lib/go-agent/config/autoregister.properties
```

### Using the GoCD agent, installed via `.deb/.rpm`

See the bootstrap script and docker file here under [`contrib/scripts/bootstrap-via-installer`](contrib/scripts/bootstrap-via-installer).

### Use a custom bootstrapper

This method uses lesser memory and boots up the agent process and starts off a build quickly:

See the bootstrap script and docker file here under [`contrib/scripts/bootstrap-without-installed-agent`](contrib/scripts/bootstrap-without-installed-agent).
