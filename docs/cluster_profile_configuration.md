# Configure cluster profile

* Navigate to **_Admin > Elastic Profiles_** in the main menu.
* Click the **_Add Cluster Profile_** button to create a new cluster profile.
* Select `GoCD Elastic Agent Plugin for Amazon ECS` value for plugin Id.   

![Alt text](images/cluster-profiles/new_cluster_profile_button.png "Admin Dropdown")

![Alt text](images/cluster-profiles/new_cluster_profile_popup.png "Plugin Dropdown")

**_Note:_** *Configuration marked with (\*) are mandatory*

## Cluster configuration

![Alt text](images/cluster-profiles/initial_cluster_configuration.png "Elastic container configuration")
 
1. **Cluster Id\*:** UUID of the newly defined cluster profile. 

2. **GoCD Server URL\*:** This is used by container to register with GoCD server. Server hostname must resolve in your container. Don't use `localhost`.

3. **Container auto register time-out\*:** If an agent running on a container created by this plugin does not register with this server within the specified timeout period (specified in minutes), the plugin will assume that the container failed to startup and will be terminated.

## Advance Container Configuration

![Alt text](images/cluster-profiles/advance_container_configuration.png "Elastic container configuration")

3. **Environment Variables:** These variables will be passed onto the container when it is started up. Read more about [ENV](https://docs.docker.com/engine/reference/builder/#env).
   
    For example
  
    ```text
    TZ=PST
    JAVA_HOME=/opt/java
    ```

4. **Container data volume size:** Maximum volume size in GB that container can use to store container data. Defaults to `10G`.

## AWS Credentials

Optionally, specify `Access Key` and `Secret Access Key` of AWS account. These are used by plugin to make API calls. Specified API keys must have appropriate privileges to access aws resources. Please refer [pre-requisites](installation.md#prerequisites) for more information.

![Alt text](images/cluster-profiles/aws_credentials.png "AWS Credentials")

If not specified, plugin will try to detect it in following order:

1. **Environment Variables -** `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` (RECOMMENDED since they are recognized by all the AWS SDKs and CLI except for .NET), or `AWS_ACCESS_KEY` and `AWS_SECRET_KEY` (only recognized by Java SDK)

2. **Java System Properties -** `aws.accessKeyId` and `aws.secretKey`

3. **Instance profile -** Instance profile credentials delivered through the Amazon EC2 metadata service

Read more about API keys [here](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys).  

## EC2 instance settings

These settings are applied to all ec2 instances launched by plugin irrespective of platform.

![Alt text](images/cluster-profiles/ec2_instance_settings.png "EC2 instance settings")

1. **AWS keypair name:** The name of the key pair that you may use to SSH or RDP into the EC2 instance. User can override this from elastic profile. Read more about [Key Pairs](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html)

2. **Subnet id(s):** Enter comma separated subnet ids. If multiple subnet ids are specified, the subnet having the least number of EC2 instances will be used to spin up a new EC2 instance. If left unspecified or the specified subnet ids are not available at the time of launching the EC2 instance, AWS will choose a default subnet from your default VPC for you. User can override this from elastic profile. Read more about [VPCs & Subnets](https://docs.aws.amazon.com/vpc/latest/userguide/VPC_Subnets.html)  

3. **Security Group Id(s):**  Enter comma separated security group ids. EC2 instances will be assigned the security groups(s) specified here. User can override this from elastic profile. Read more about [SecurityGroup](https://docs.aws.amazon.com/vpc/latest/userguide/VPC_SecurityGroups.html)

4. **IAM profile for EC2 instance\*:** The name of the IAM profile that will allow the ECS agent to make API calls to AWS on your behalf. Please refer [pre-requisites](../prerequisites/) for the bare minimum privileges your profile must have to allow plugin to make API calls. User can override this from elastic profile.

## EC2 instance settings for Linux

This is to configure linux specific defaults for EC2 instance. It will be used to launch new EC2 instance. However, user can override few of the defaults from an elastic profile.

![Alt text](images/cluster-profiles/linux_instance_settings.png "EC2 instance settings for Linux")
                                     
1. **AMI ID:** The AMI ID that will be used when an instance is spun up. The ECS agent will run on this ECS optimized EC2 instance. We recommend using an [Amazon ECS-Optimized AMI](http://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs-optimized_AMI.html). ECS optimized Linux AMIs are available [here](http://docs.aws.amazon.com/AmazonECS/latest/developerguide/launch_container_instance.html). User can override this from elastic profile.

2. **Instance type:** This instance type will be used to spin up EC2 instances that will run docker containers with this profile. Read more about [Instance Type](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/instance-types.html)
                       
3. **Operating system volume type:** Allows to override default operating system volume. This is used to store operating system and container volumes. It will get deleted on instance termination. Defaults to `8G`. User may have to increase size of the volume if containers are generating too much persistent data. See [docker volumes](https://docs.docker.com/storage/volumes/) for more information.

4. **Docker Volume type:** Allows to override default storage with specified type and size. This is used to store docker images and metadata. It will get deleted on ec2 instance termination. Defaults to `22G`. User may have to increase size of th`e volume if docker images size is too big to fit in 22G of default volume. See [docker storage](https://docs.docker.com/storage/storagedriver/) for more information.

5. **Instance creation timeout:** If an EC2 instance created by this plugin does not register with the container service within this timeout period, the plugin will assume that the instance has failed to startup and will be terminated. Defaults to `5 minutes`.  

6. **Minimum instance required in cluster:** Minimum linux instances you'd like to have running at any point of time. Defaults to `0`.

7. **Maximum instances allowed:** Restricts maximum number of linux instances in the cluster. Plugin will not launch new linux instance if the cluster is already running specified number of instances. Defaults to `5`.

8. **Instance stop policy\*:** Plugin will stop ec2 instances in the ECS cluster based on specified stop policy.
    - **Stop Idle Instance:** Plugin stops the instance which is idle for more than the specified idle timeout. Defaults to `10 minutes`.
    - **Stop Oldest Instance:** Plugin stops the oldest instance in the group. This option is useful when you're upgrading the instances in the cluster to a new EC2 instance type, so you can gradually replace instances of the old type with instances of the new type.

9. **Terminate stopped instance after\*:** The plugin terminates the instance which is in `stopped` state for more than specified period. Defaults to `5 minutes`.
    
10. **Spot Instance Configuration**
    - **Maximum Spot Instances Allowed in Cluster:** Restricts the maximum number of linux Spot Instances allowed in the cluster. Plugin will not launch new Spot Instance if the cluster is already running specified number of instances or has Spot Instance requests pending. Defaults to 10.
    - **Terminate Idle Spot Instances after(in minutes):** The plugin terminates a Spot Instance which is idle for more than specified period. Defaults to `30 minutes`.
                                      
11. **Userdata script:**  This allows user to execute command on startup of ec2 instance. Read more about [userdata script](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/user-data.html).
                         
    For example
    
    ```bash
    yum update -y
    yum install -y subversion git
    ```

## EC2 instance settings for Windows

This is to configure windows specific defaults for EC2 instance. It will be used to launch new EC2 instance. However, user can override few of the defaults from an elastic profile.

![Alt text](images/cluster-profiles/windows_instance_settings.png "EC2 instance settings for Windows")
                                     
1. **AMI ID:** The AMI ID that will be used when an instance is spun up. The ECS agent will run on this ECS optimized EC2 instance. We recommend using an [Amazon ECS-Optimized AMI](http://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs-optimized_AMI.html). ECS optimized Windows AMIs are available [here](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/ECS_Windows_getting_started.html#launch_windows_container_instance). User can override this from elastic profile.

2. **Instance type:** This instance type will be used to spin up EC2 instances that will run docker containers with this profile. Read more about [Instance Type](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/instance-types.html)
                       
3. **Operating system volume type:** Allows to override default operating system volume. This is used to store all persistent data including docker. It will get deleted on instance termination. Defaults to `50G`.  

5. **Instance creation timeout:** If an EC2 instance created by this plugin does not register with the container service within this timeout period, the plugin will assume that the instance has failed to startup and will be terminated. Defaults to `15 minutes`.  

6. **Minimum instance required in cluster:** Minimum linux instances you'd like to have running at any point of time. Defaults to `0`.

7. **Maximum instances allowed:** Restricts maximum number of linux instances in the cluster. Plugin will not launch new linux instance if the cluster is already running specified number of instances. Defaults to `5`.
    
8. **Instance stop policy\*:** Plugin will stop ec2 instances in the ECS cluster based on specified stop policy.
    - **Stop Idle Instance:** Plugin stops the instance which is idle for more than the specified idle timeout. Defaults to `10 minutes`.
    - **Stop Oldest Instance:** Plugin stops the oldest instance in the group. This option is useful when you're upgrading the instances in the cluster to a new EC2 instance type, so you can gradually replace instances of the old type with instances of the new type.

9. **Terminate stopped instance after\*:** The plugin terminates the instance which is in `stopped` state for more than specified period. Defaults to `5 minutes`.
                        
10. **Spot Instance Configuration**
    - **Maximum Spot Instances Allowed in Cluster:** Restricts the maximum number of linux Spot Instances allowed in the cluster. Plugin will not launch new Spot Instance if the cluster is already running specified number of instances or has Spot Instance requests pending. Defaults to 10.
    - **Terminate Idle Spot Instances after(in minutes):** The plugin terminates a Spot Instance which is idle for more than specified period. Defaults to `30 minutes`.                              

11. **Userdata script:**  This allows user to execute powershell commands on startup of ec2 instance. Do not use `<powershell>` or `<script>` tags in script. Read more about [userdata script](https://docs.aws.amazon.com/AWSEC2/latest/WindowsGuide/ec2-windows-user-data.html).
                         
    For example
    
    ```
    $file = $env:SystemRoot + "\Temp\" + (Get-Date).ToString("MM-dd-yy-hh-mm")
    New-Item $file -ItemType file
    ``` 
    
## AWS Cluster configuration

![Alt text](images/cluster-profiles/aws_cluster_configuration.png "AWS Cluster configuration")

1. **AWS Cluster Name\*:** Name of the [ECS cluster](http://docs.aws.amazon.com/AmazonECS/latest/developerguide/ECS_clusters.html) that will be managed by the plugin. This cluster must already exist.

2. **AWS Region:** If you don't specify an [Availability Zone](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-regions-availability-zones.html), AWS will choose one for you.


## Log configuration

![Alt text](images/cluster-profiles/log_configuration.png "Log configuration")

1. **Log driver name:** Read more about [enabling the awslogs](http://docs.aws.amazon.com/AmazonECS/latest/developerguide/using_awslogs.html#enable_awslogs) on your Container Instances. Supported logging drivers are listed [here](https://docs.docker.com/engine/admin/logging/overview/#supported-logging-drivers).

2. **Log options:** Log options are used to filter log for the container instance. Enter one variable per line. Read more about [log options](https://docs.docker.com/config/containers/logging/awslogs/)
    
    For example
    
    ```
    awslogs-group=awslogs-mysql
    awslogs-region=ap-northeast-1
    ```
    
## Docker Registry
    
This is to override default docker registry settings. Default registry is [hub.docker.com](https://hub.docker.com)

![Alt text](images/cluster-profiles/docker_registry.png "Docker registry configuration")

### Authenticate using auth token
    
1. **Private docker registry url\*:** Specify docker registry URL here
2. **Email\*:** Specify email of the user. This settings is already deprecated by docker. This may get removed in subsequent releases.
3. **Docker registry auth token\*:** Authentication token for the user.

### Authenticate using username and password

1. **Private docker registry url\*:** Specify docker registry URL here
2. **Email\*:** Specify email of the user. This settings is already deprecated by docker. This may get removed in subsequent releases.
3. **Docker registry username\*:** Username of the private docker registry user. 
4. **Docker registry password\*:** Password associated with the above specified user.

## EFS 

![Alt text](images/cluster-profiles/efs.png "EFS configuration")

**EFS IP address or DNS:** Plugin will use provided [EFS](https://docs.aws.amazon.com/efs/latest/ug/whatisefs.html) volume and mount it at /efs in ec2 instances and in docker containers. Multiple Amazon EC2 instances can access an Amazon EFS file system, so it can be used to store credentials for docker container.

After configuring settings **save your changes**

<aside class="info">
    <strong>Note:</strong>
    <ol>
        <li>EFS settings is supported only on <code>RPM</code> based linux operating system. This may get removed in subsequent releases.</li>
        <li>Configuration marked with (*) are mandatory.</li>
    </ol>
</aside>
