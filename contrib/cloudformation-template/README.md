## Run following commands to create cloud-formation stack

```bash
bundle
bundle exec ruby ecs_cloud_formation_template.rb create --stack-name <your-stack-name> --parameters "ClusterName=<cluster-name>;GoCDServerId=<gocd-server-id>"
```

The `GoCDServerId` is the unique ID of the GoCD server that will be allowed to assume the plugin's IAM role, found
in the `serverId` attribute of the `<server/>` element within _Admin > Config XML_ (`cruise-config.xml`).

## Regenerate the JSON template in docs/ after changing the ruby template

```bash
bundle
bundle exec ruby ecs_cloud_formation_template.rb expand > ../../docs/ecs_cloud_formation_template.json
```
