# GoCD - Amazon ECS Elastic Agent Plugin

Table of Contents
=================

  * [Prerequisites](#prerequisites)
  * [Installation](#installation)
  * [Upgrading](#upgrading)
  * [Configuration](#configuration)
  
## Prerequisites

In order to use GoCD's Amazon ECS Elastic Agent Plugin, the following pre-requisites must be met.

* The GoCD server version should be **19.3.0** or higher.
* The plugin mandates a few Amazon ECS pre-requisites like an empty ECS cluster and IAM roles to be configured with appropriate permissions.

### Configure ECS using the *AWS CloudFormation* template:

The easy and quick way to configure ECS is by using the provided **AWS CloudFormation** template. This template creates an empty ECS cluster, an IAM role for the plugin to [assume](cluster_profile_configuration.md#assuming-an-iam-role) with the appropriate permissions, and the IAM role and instance profile for the ECS container instances. No IAM users or long-lived access keys are created: the plugin assumes the role using whatever base credentials the GoCD server already has (for example its EC2 instance profile), and the role's trust policy only permits `sts:AssumeRole` calls that supply your GoCD server's ID as the [external ID](cluster_profile_configuration.md#external-id).

The template requires a `GoCDServerId` parameter — the unique ID of your GoCD server, found in the `serverId` attribute of the `<server/>` element in _Admin > Config XML_ (`cruise-config.xml`).

To configure ECS using the AWS CloudFormation templates, follow these steps,

* Install aws cli (refer - [https://aws.amazon.com/cli/](https://aws.amazon.com/cli/))
* `$ aws configure` - Configure the aws cli with appropriate credentials
* Configure using ruby based template (DSL is somewhat outdated, be aware)
  * See [Ruby Based CloudFormation Template](../contrib/cloudformation-template).
  * `$ bundle install`
  * `$ ruby <cloud_formation_ruby_template_file>.rb create --stack-name <stack_name> --parameters "ClusterName=<cluster_name>;GoCDServerId=<gocd_server_id>"`
* Alternatively, configure using the JSON based template
  * Save [JSON Based CloudFormation Template](ecs_cloud_formation_template.json) to a file.
  * ```$ aws cloudformation create-stack --stack-name <stack_name> --region <region_name> --capabilities CAPABILITY_IAM --template-body file:///<path_to_json_template_file> --parameters ParameterKey=ClusterName,ParameterValue=<cluster_name> ParameterKey=GoCDServerId,ParameterValue=<gocd_server_id>```

After the stack is created:

* Set the `GoCDECSPluginRole` stack output as the `Assume Role ARN` in your [cluster profile](cluster_profile_configuration.md), leaving the access key and secret key blank.
* Ensure the GoCD server's own identity (e.g. the role of its EC2 instance profile) is granted `sts:AssumeRole` on that role ARN — the trust policy delegates to your account, so the caller still needs this permission on its side.
* Set the `GoCDEC2OptimizedInstanceProfile` stack output as the `Agent IAM Instance Profile` in your cluster profile.

---
### Alternatively configure ECS manually

Listed below are the steps to be followed if you prefer to manually configure ECS instead of using the above CloudFormation template:

  * An ECS cluster. This cluster MUST be empty, as the plugin will manage instances and scaling in this cluster.
  * An IAM identity with permissions to manage EC2 instances and the ECS cluster. Access keys are optional: the credentials for this identity may be provided to the plugin as an access key/secret key pair, detected automatically from the GoCD server's environment (environment variables, Java system properties or the EC2 instance profile of the instance running the GoCD server), or granted to a separate role the plugin is configured to [assume](cluster_profile_configuration.md#assuming-an-iam-role). The following policies grant the required permissions while restricting `iam:PassRole` to the specific roles the plugin passes on your behalf:

    **ManageEC2Instances**

    ```json
    {
      "Version": "2012-10-17",
      "Statement": [
        {
          "Effect": "Allow",
          "Action": [
            "ec2:RunInstances",
            "ec2:StartInstances",
            "ec2:StopInstances",
            "ec2:TerminateInstances",
            "ec2:DescribeInstances",
            "ec2:DescribeSubnets",
            "ec2:CreateTags",
            "ec2:DeleteTags",
            "ec2:CreateVolume",
            "ec2:AttachVolume",
            "ec2:RequestSpotInstances",        // If using spot instances
            "ec2:DescribeSpotInstanceRequests" // If using spot instances
          ],
          "Resource": "*"
        },
        {
          "Effect": "Allow",
          "Action": "iam:PassRole",
          "Resource": [
            "arn:aws:iam::111111111111:role/gocd-ecs-instance-role"
          ],
          "Condition": {
            "StringEquals": {
              "iam:PassedToService": "ec2.amazonaws.com"
            }
          }
        }
      ]
    }
    ```

    The `iam:PassRole` statement must list the role(s) behind the `IAM Instance Profile` configured in your cluster profiles — the container instance role described below. `ec2:RequestSpotInstances` and `ec2:DescribeSpotInstanceRequests` are only needed if you use spot instances.

    **ManageECSTasks**

    ```json
    {
      "Version": "2012-10-17",
      "Statement": [
        {
          "Effect": "Allow",
          "Action": [
            "ecs:DescribeClusters",
            "ecs:ListContainerInstances",
            "ecs:DescribeContainerInstances",
            "ecs:DeregisterContainerInstance",
            "ecs:DescribeTaskDefinition",
            "ecs:RegisterTaskDefinition",
            "ecs:DeregisterTaskDefinition",
            "ecs:DeleteTaskDefinitions",
            "ecs:StartTask",
            "ecs:StopTask",
            "ecs:ListTasks",
            "ecs:DescribeTasks"
          ],
          "Resource": "*"
        },
        {
          "Effect": "Allow",
          "Action": "iam:PassRole",
          "Resource": [
            "arn:aws:iam::111111111111:role/gocd-ecs-task-role"
          ],
          "Condition": {
            "StringEquals": {
              "iam:PassedToService": "ecs-tasks.amazonaws.com"
            }
          }
        }
      ]
    }
    ```

    The `iam:PassRole` statement here is only required if your elastic agent profiles configure a `Task Role ARN`. List those roles as its resources, or omit the statement entirely if you do not use task roles.

    > **Why not simply grant `iam:PassRole` on all resources?** Granting unscoped `iam:PassRole` alongside `ec2:RunInstances` allows privilege escalation: anything holding these credentials could launch an EC2 instance with *any* IAM role in the account, including highly privileged ones. Restricting `iam:PassRole` to the specific roles the plugin needs to pass, and to the services they are passed to (via the `iam:PassedToService` condition), removes this risk. Earlier versions of this documentation and of the CloudFormation template granted `iam:PassRole` and `iam:GetRole` on all resources — existing installations should consider tightening their policies to match the above (`iam:GetRole` is not needed by the plugin at all).

  * An IAM Role that allows the ECS agent running on the EC2 instance to register container instances with ECS cluster. The IAM instance profile must have following permissions:

    ```
    ecs:DiscoverPollEndpoint
    ecs:Poll
    ecs:RegisterContainerInstance
    ecs:DeregisterContainerInstance
    ecs:StartTelemetrySession
    ecs:SubmitTaskStateChange
    ecs:SubmitContainerStateChange
    logs:PutLogEvents
    logs:CreateLogStream
    ```

### Related help topics

1. [Getting Started with Amazon ECS](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/Welcome.html)
2. [Amazon ECS Container Instance IAM Role](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/instance_IAM_role.html)
3. [Amazon ECS IAM Policy Examples](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/security_iam_id-based-policy-examples.html)
4. [Amazon ECS Task Role](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html)

## Installation

* Copy the file `build/libs/ecs-elastic-agent-plugin-VERSION.jar` to the GoCD server under `${GO_SERVER_DIR}/plugins/external`
and restart the server.
* The `GO_SERVER_DIR` is usually `/var/lib/go-server` on **Linux** and `C:\Program Files\Go Server` on **Windows**. You can find the location of the GoCD Server installation [here](https://docs.gocd.org/current/installation/installing_go_server.html).

## Upgrading

* Generally the plugin JAR can be replaced with the latest version.
* **Please note** that plugin version `7.3.0+` requires additional permissions for the IAM role that allows creating and managing tasks. See https://github.com/gocd/gocd-ecs-elastic-agent/releases/tag/v7.3.0-407 for more details.

## Configuration

In order to use this plugin, users have to configure the following in the GoCD server.

1. [Configure cluster profile(s)](cluster_profile_configuration.md) - The cluster profile settings are used to provide the cluster level configurations for the plugin. Each cluster profile specifies Configurations such as AWS credentials, EC2 Instance settings, Docker Container settings, ECS Cluster configurations. Users are encouraged to define one cluster per environment. 

2. [Create elastic agent profile(s)](elastic_profile_configuration.md) - The Elastic Agent Profile is used to define the configuration of a docker container. The profile is used to configure the docker image, set memory limits, provide docker command and environment variables. 

3. [Configure job to use an elastic agent profile](job_configuration.md) - This is a job level configuration to specify the elastic profile to be used for a job. When a job is scheduled the plugin would spin up a docker container using the configuration provided in the associated elastic profile.
