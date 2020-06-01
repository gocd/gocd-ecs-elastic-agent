# GoCD - Amazon ECS Elastic Agent Plugin

This Elastic Agent Plugin for Amazon EC2 Container Service allows you to run elastic agents on Amazon ECS (Docker container service on AWS). The plugin takes care of spinning up and shutting down EC2 instances based on the need of your deployment pipeline, thus removing bottlenecks and reducing the cost of your agent infrastructure.

When a pipeline is triggered, GoCD sees that the jobs in the pipeline have been configured to use ECS agents, and passes on information about the configured elastic agent profiles to the plugin. The plugin, based on the user-defined configuration, decides how many EC2 instances to bring up or reuse and how many ECS elastic agents to bring up within those EC2 instances.

The settings allow you to choose the AMI to be used for the EC2 instance, the instance type, security groups, the Docker image for the ECS container and memory limits among other settings. Since the Docker image is specified as a part of the profile, provisioning software for a build, test or deploy agent becomes much easier.

Once the builds finish and the EC2 instances are idle for a while, they will be automatically scaled down and destroyed, removing the cost of running idle EC2 instances. Along with saving cost, this enables a flexible and dynamic build grid in which you don’t need to worry about configuration drift.

The set of images [here](docs/plugin_as_images.md) explain this concept as well.

Table of Contents
=================

  * [Building the code base](#building-the-code-base)
  * [Installing and configuring the plugin](docs/installation.md)
    * [Prerequisites](docs/installation.md#prerequisites)
        * [Configure ECS using the *AWS CloudFormation* template](docs/installation.md#configure-ecs-using-the-aws-cloudformation-template)
        * [Alternatively configure ECS manually](docs/installation.md#alternatively-configure-ecs-manually)
    * [Installation](docs/installation.md#installation)
    * [Configuration](docs/installation.md#configuration)
        * [Configure cluster profile(s)](docs/cluster_profile_configuration.md)
            * [Cluster configuration](docs/cluster_profile_configuration.md#cluster-configuration)
            * [Advanced Container Configuration](docs/cluster_profile_configuration.md#advanced-container-configuration)
            * [Aws Credentials](docs/cluster_profile_configuration.md#aws-credentials)
            * [EC2 Instance Settings](docs/cluster_profile_configuration.md#ec2-instance-settings)
                * [For Linux](docs/cluster_profile_configuration.md#ec2-instance-settings-for-linux)
                * [For Windows](docs/cluster_profile_configuration.md#ec2-instance-settings-for-windows)
            * [AWS Cluster Configuration](docs/cluster_profile_configuration.md#aws-cluster-configuration)
            * [Log Configuration](docs/cluster_profile_configuration.md#log-configuration)
            * [Docker Registry](docs/cluster_profile_configuration.md#docker-registry)
            * [EFS](docs/cluster_profile_configuration.md#efs)
        * [Create elastic agent profile(s)](docs/elastic_profile_configuration.md)
            *[container-configuration](docs/elastic_profile_configuration.md#container-configuration)
            *[ec2-instance-configuration](docs/elastic_profile_configuration.md#ec2-instance-configuration)
        * [Configure job to use an elastic agent profile](docs/job_configuration.md)
  * [FAQ](docs/faq.md)
  * [Troubleshooting](docs/troubleshooting.md)

## Building the code base

To build the jar, run `./gradlew clean check assemble`

## License

```plain
Copyright 2020 ThoughtWorks, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
