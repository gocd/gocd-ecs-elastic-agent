# Changelog

# 5.0.0 - 2018-12-03

### Added
- Support for windows container.
- Removed termination policy `ClosestToNextInstanceHour`.
    - Plugin will use `TerminateIdleInstance` instead of `ClosestToNextInstanceHour` as default policy.
- Introduced new termination policy `StopIdleInstance`
    - Plugin stops the instance which is idle for more than specified idle timeout. Defaults to `5 minutes`.
    - It will starts it back when it is needed. The policy will be useful when launch time of EC2 instance is high(e.g. windows instance)
    
- Support for userdata script for EC2 instance.   
- Improved status report UI.  

### Bug fixes
- Do not create task if one is already scheduled
- Support new ARNs and Id format see https://aws.amazon.com/blogs/compute/migrating-your-amazon-ecs-deployment-to-the-new-arn-and-resource-id-format-2/ for more information.

### Breaking changes
Plugin now requires additional permissions on to support new termination policy

```yaml
- ec2:stopInstances
- ec2:startInstances
- ec2:deleteTags
```

Refer to [prerequisites](https://github.com/gocd/gocd-ecs-elastic-agent/blob/master/docs/installation.md#prerequisites) for more information.

**_Note:_** *Requires GoCD version 18.10.0 or higher. Plugin will not work with the older version of GoCD.*

## 4.1.0 - 2018-07-19

### Added
- The plugin status report will now render all container instances expanded to make it easier to search for things.
- **_Container data volume size_**
  * Added ability to configure the container data volume size through Plugin Settings. `defaults to 10G`.

### Bug Fixes
- **_Storage Configuration_**
  * Ability to override default storage option to store docker images and metadata through Plugin Settings. `defaults to 22G`
  * Read more about [storage configuration](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs-ami-storage-config.html).

## 4.0.0 - 2018-03-09

### Added
- Support for agent status report and plugin status report.
- Plugin now uses's job identifier to tag the docker services. This allows the plugin to decide to which agent it should assign work.
- Support to mount docker.sock to container.
- Start container in privileged mode.

**_Note:_** *Requires GoCD version 18.2.0 or higher. Plugin will not work with the older version of GoCD.*

## 3.0.0 - 2017-12-21

### Added

* Support for scaling based on CPU limits


## 2.0.0 - 2017-09-05

### Added

 * UI improvements for plugin status report
    - Status report has pending task count
    - Print uptime as a 0 minutes while container instance(ec2 instance) is booting up
    
 * Implemented elastic agent `get-capabilities` v2 endpoint and exposes the status_report capability.
 
 **_Note:_** This requires GoCD version 17.9.0

## 1.0.0 - 2017-08-02

 
### Added

 * Support for `Plugin Status Report` which contains information about the cluster and lists errors and warnings if any.
 * Support for EFS Volume mount for ECS Container.
 * Support for Auto Detection of AWS Credentials.


## 0.0.3 - 2017-05-22


### Added

* Support for **Termination Policy** to terminate ec2 instance
* Support to attach additional ebs volume to container instance 

### Improvements

* Allows user to configure `MinEC2InstanceCount` to 0
* Clean containers data as soon as it exit to optimize volume usage

### Removed

* Support for `Maximum containers per EC2 instance` and `Terminate idle EC2 instance`


## 0.0.2 - 2017-04-18
    

### Improvements

* Added support for private docker registries
* Allows users to provide multiple subnet ids in plugin settings and elastic profile
    * EC2 instances will be evenly distributed across all subnets
* Allows users to skip configuration for AWS keypair in plugin settings

### Breaking Changes

* Users will have to reconfigure the subnet Id fields in plugin settings and elastic profiles
* If you are using an IAM user with restricted permissions, please be sure to allow the user `ec2:describeSubnets` Permissions.


## 0.0.1 - 2017-04-10


### Improvements

* Added log statement that prints the exact version of the plugin that is installed.
* Improved an error message when a plugin is not configured.
* Provided a cloudformation template in `contrib/cloudformation-template` that users may use to setup their builds on AWS using cloudformation.
* Added help text on the plugin settings page to assist users in setting up the plugin.
* Added support to configure logging on the container
* Added validation on container max memory and reserved memory, the memory parameters will require a suffix to indicate the unit of memory (MB, GB, etc...)

### Breaking Changes

* Renamed the plugin id. Users will have to 
  * edit the `cruise-config.xml` file from *Admin>Config XML* and replace all occurrence of `cd.go.contrib.elastic-agent.ecs` with `com.thoughtworks.gocd.elastic-agent.ecs` 
  * re-define all plugin settings by going to *Admin>Plugins* on their GoCD server.


