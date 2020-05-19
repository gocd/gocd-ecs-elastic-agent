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
  * [FAQ](docs/faq.md)
  * [Troubleshooting](docs/troubleshooting.md)

## Building the code base

To build the jar, run `./gradlew clean test assemble`

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
