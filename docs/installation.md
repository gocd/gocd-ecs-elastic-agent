# GoCD - Amazon ECS Elastic Agent Plugin

Table of Contents
=================

  * [Prerequisites](#prerequisites)
  * [Installation](#installation)
  * [Configuration](#configuration)
  
## Prerequisites

In order to use GoCD's Amazon ECS Elastic Agent Plugin, the following pre-requisites must be met.

* The GoCD server version should be **19.3.0** or higher.
* The plugin mandates a few Amazon ECS pre-requisities like an empty ECS cluster and IAM user and role to be configured with appropriate permissions.

### Configure ECS using the *AWS CloudFormation* template:

The easy and quick way to configure ECS is by using the provided **AWS CloudFormation** template. This template creates an empty ECS cluster and defines IAM user and role with appropriate permission.

To configure ECS using the AWS CloudFormation templates, follow these steps,

* `$ pip install awscli` - To install aws cli (refer - [https://aws.amazon.com/cli/](https://aws.amazon.com/cli/))
* `$ aws configure` - Configure the aws cli with appropriate credentials
* Configure using ruby based template,
  * Save [Ruby Based Cloudformation Template](ecs_cloud_formation_template.rb.txt) to a file.
  * `$ gem install cloudformation-ruby-dsl`
  * `$ ruby <cloud_formation_ruby_template_file>.rb create --stack-name <stack_name> --parameters "ClusterName=<cluster_name>"`
* Alternatively, configure using the JSON based template
  * Save [JSON Based Cloudformation Template](ecs_cloud_formation_template.json) to a file.
  * ```$ aws cloudformation create-stack --stack-name <stack_name> --region <region_name> --capabilities CAPABILITY_IAM --template-body file:///<path_to_json_template_file>```

---
### Alternatively configure ECS manually

Listed below are the steps to be followed if you prefer to manually configure ECS instead of using the above CloudFormation template:

  * An ECS cluster. This cluster MUST be empty, as the plugin will manage instances and scaling in this cluster.
  * An IAM user with permissions to manage the ECS cluster. The credentials for this IAM user may be provided to the this plugin or applied via an IAM instance profile to the EC2 instance running the GoCD server. The following IAM permissions are needed

    ```
    ec2:CreateTags
    ec2:RunInstances
    ec2:DescribeSubnets
    ec2:DescribeInstances
    ec2:TerminateInstances
    ec2:CreateVolume
    ec2:AttachVolume
    iam:PassRole
    iam:GetRole
    ecs:DescribeClusters
    ecs:StartTask
    ecs:StopTask
    ecs:ListTasks
    ecs:DescribeTasks
    ecs:DescribeTaskDefinition
    ecs:RegisterTaskDefinition
    ecs:DeregisterTaskDefinition
    ecs:DeleteTaskDefinitions
    ecs:ListContainerInstances
    ecs:DescribeContainerInstances
    ecs:DeregisterContainerInstance
    ec2:StopInstances
    ec2:StartInstances
    ec2:DeleteTags
    ```
    
    Additionally, if Spot Instances are used, the additional permissions are needed:
    
    ```
    ec2:RequestSpotInstances
    ec2:DescribeSpotInstanceRequests
    ```


  * An IAM Role that allows the ECS agent running on the EC2 instance to register container instances with ECS cluster. The IAM instance profile must have following permissions:

    ```
    ecs:Poll
    ecs:DescribeClusters
    ecs:DiscoverPollEndpoint
    ecs:StartTelemetrySession
    ecs:SubmitTaskStateChange
    ecs:RegisterContainerInstance
    ecs:SubmitContainerStateChange
    ecs:DeregisterContainerInstance
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

## Configuration

In order to use this plugin, users have to configure the following in the GoCD server.

1. [Configure cluster profile(s)](cluster_profile_configuration.md) - The cluster profile settings are used to provide the cluster level configurations for the plugin. Each cluster profile specifies Configurations such as AWS credentials, EC2 Instance settings, Docker Container settings, ECS Cluster configurations. Users are encouraged to define one cluster per environment. 

2. [Create elastic agent profile(s)](elastic_profile_configuration.md) - The Elastic Agent Profile is used to define the configuration of a docker container. The profile is used to configure the docker image, set memory limits, provide docker command and environment variables. 

3. [Configure job to use an elastic agent profile](job_configuration.md) - This is a job level configuration to specify the elastic profile to be used for a job. When a job is scheduled the plugin would spin up a docker container using the configuration provided in the associated elastic profile.
