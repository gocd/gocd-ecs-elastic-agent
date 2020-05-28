# GoCD elastic agent images

GoCD elastic agents run inside Docker containers on an ECS cluster. You can either use the Docker images provided by the GoCD team or build your own images with the right software already provisioned on it.

  * [Docker images provided by the GoCD team](https://hub.docker.com/u/gocd/) on Docker Hub
  * Corresponding `Dockerfile` and init scripts of these docker images can be found [here](https://github.com/gocd?utf8=%E2%9C%93&q=docker-gocd-agent)

## Using your own docker image with elastic agents

The plugin executes the equivalent of the following docker command to start the agent:

```bash
$ docker run -e GO_EA_SERVER_URL=...
           -e GO_EA_AUTO_REGISTER_KEY=...
           -e GO_EA_AUTO_REGISTER_ENVIRONMENT=...
           -e GO_EA_AUTO_REGISTER_ELASTIC_AGENT_ID=...
           -e GO_EA_AUTO_REGISTER_ELASTIC_PLUGIN_ID=...
           ...
           IMAGE_ID
```

Your docker image is expected to contain a bootstrap script (to be executed via docker's `CMD`) that will create an [`autoregister.properties`](https://docs.gocd.org/current/advanced_usage/agent_auto_register.html) file using these variables. The `GO_EA_SERVER_URL` will point to the server url that the agent must communicate with.

Here is an example shell script to do this —

```bash
# write out autoregister.properties
$ (
    cat <<EOF
    agent.auto.register.key=${GO_EA_AUTO_REGISTER_KEY}
    agent.auto.register.environments=${GO_EA_AUTO_REGISTER_ENVIRONMENT}
    agent.auto.register.elasticAgent.agentId=${GO_EA_AUTO_REGISTER_ELASTIC_AGENT_ID}
    agent.auto.register.elasticAgent.pluginId=${GO_EA_AUTO_REGISTER_ELASTIC_PLUGIN_ID}
    EOF
  ) > /var/lib/go-agent/config/autoregister.properties
```

Here is an example powershell script to do this -

```powershell
# write out autoregister.properties
$properties = @("agent.auto.register.key=$env:GO_EA_AUTO_REGISTER_KEY",
"agent.auto.register.environments=$env:GO_EA_AUTO_REGISTER_ENVIRONMENT",
"agent.auto.register.elasticAgent.agentId=$env:GO_EA_AUTO_REGISTER_ELASTIC_AGENT_ID",
"agent.auto.register.elasticAgent.pluginId=$env:GO_EA_AUTO_REGISTER_ELASTIC_PLUGIN_ID")

$properties | Out-File "c:\gocd-agent\config\autoregister.properties" -Encoding "default" -append
```

**Note:** GoCD agent supports only `Little-endian` encoding. Please make sure that generated file has valid encoding.

Refer to the bootstrap script template [here](https://github.com/gocd/gocd/blob/master/buildSrc/src/main/resources/gocd-docker-agent/docker-entrypoint.sh) to understand what is necessary for the image to be a GoCD agent docker image. To look for the bootstrap script for a particular container image of your choice, check one of the docker agent image repositories [here](https://github.com/gocd?utf8=%E2%9C%93&q=docker-gocd-agent).
