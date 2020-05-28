# FAQ

# FAQ

## Q: What is GoCD ECS Elastic Agent Plugin?

This Elastic Agent Plugin for Amazon EC2 Container Service allows for flexible use of EC2 instances (with support for ECS Docker containers) thereby optimizing utilization and reducing the cost of your agent infrastructure. It will also take care of scaling up and scaling down of your container instances in best possible way.

## Q: Why should I use it?

This elastic agent plugin will help optimize the cost of running your builds on AWS by managing the cluster resources. It will create a container instance as and when required and will terminate the idle container instances based on termination policy that you provide. This will eliminate the need of constantly running AWS EC2 instances as GoCD agents.

## Q: How to install this plugin?
 
Refer to the installation section available [here](installation.md).

## Q:  How to configure this plugin?

Refer to the [pre-requisites](installation.md#prerequisites) and [configuration](installation.md#configuration) sections to configure this plugin.

## Q: Why does this plugin need AWS credentials?

In order to manage AWS ECS cluster, the plugin makes multiple API calls to AWS ECS on your behalf. Using the credentials, the plugin will authorize itself to AWS.

## Q: How should I configure the AWS credentials for the plugin?

You can configure AWS credentials in many ways. The plugin will look for the credentials in following order:

1. Environment variables
    - You can provide `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`, or `AWS_ACCESS_KEY` and `AWS_SECRET_KEY`   
2. Java system properties
    - You can provide `aws.accessKeyId` and `aws.secretKey`
3. If your GoCD server is running on an EC2 instance brought up with an IAMInstanceProfile, then, the IAMInstanceProfile will be picked as the credentials provider. 

We recommend that you provide AWS credentials using one of the above methods, but you can also configure `Access Key Id` and `Secret Key Id` on the ECS cluster profile settings page.

## Q: Is this plugin capable of running windows containers (agents)?

Running windows containers is supported from the plugin version `5.0.0` onwards.

## Q: Why memory reservation is not available for windows on elastic profile?

Currently, option is not supported by ECS API.

## Q: Is privileged mode supported on windows?

No, privileged mode is not supported for windows.

## Q: Is configuring multiple environments supported by this plugin?

Yes, configuring plugin for different environments is supported using cluster profiles from the plugin version `6.0.0` and onwards.

## Q. I am using Spot Instances, how do I check for my savings?

The plugin currently does not capture any data to calculate the savings. AWS provides with Spot Instance [Savings Summary](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/spot-savings.html) page to get this information. Alternatively, you can also subscribe the [Spot Instance Data Feed](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/spot-data-feeds.html) to get this information from AWS.
