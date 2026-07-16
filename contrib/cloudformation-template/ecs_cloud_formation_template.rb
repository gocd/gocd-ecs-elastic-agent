#!/usr/bin/env ruby

require 'bundler/setup'
require 'cloudformation-ruby-dsl/cfntemplate'
require 'cloudformation-ruby-dsl/spotprice'
require 'cloudformation-ruby-dsl/table'

template do

  value AWSTemplateFormatVersion: '2010-09-09'

  parameter 'ClusterName',
            :Description => 'The name of the ECS cluster',
            :Type => 'String',
            :Default => 'GoCD',
            :UsePreviousValue => true

  parameter 'GoCDServerId',
            :Description => 'The unique ID of the GoCD server that may assume the plugin role, found in the serverId attribute within Admin > Config XML (cruise-config.xml)',
            :Type => 'String',
            :AllowedPattern => '[a-zA-Z0-9-]+',
            :ConstraintDescription => 'must be a GoCD server id',
            :UsePreviousValue => true

  output 'GoCDECSPluginRole',
         Value: get_att('GoCDECSPluginRole', 'Arn')

  output 'GoCDEC2OptimizedRole',
         Value: get_att('GoCDEC2OptimizedRole', 'Arn')

  output 'GoCDEC2OptimizedInstanceProfile',
         Value: get_att('GoCDEC2OptimizedInstanceProfile', 'Arn')

  resource 'GoCDECSCluster',
           Type: 'AWS::ECS::Cluster',
           Properties: {
             ClusterName: ref('ClusterName')
           }

  # The plugin does not need long-lived access keys; it assumes this role using whatever base credentials the GoCD
  # server already has (e.g. its EC2 instance profile). The trust policy allows any identity in this account that
  # has been granted sts:AssumeRole on this role AND presents this GoCD server's external id. Replace the account
  # root principal with the specific IAM role/user of the GoCD server to tighten it further.
  resource 'GoCDECSPluginRole',
           Type: 'AWS::IAM::Role',
           Properties: {
             Description: 'Assumed by the GoCD ECS elastic agent plugin to manage EC2 instances and ECS tasks',
             AssumeRolePolicyDocument: {
               Version: '2012-10-17',
               Statement: [
                 {
                   Effect: 'Allow',
                   Principal: {
                     AWS: join('', 'arn:aws:iam::', aws_account_id, ':root'),
                   },
                   Action: 'sts:AssumeRole',
                   Condition: {
                     StringEquals: {
                       'sts:ExternalId': join('', 'gocd:server-id:', ref('GoCDServerId')),
                     },
                   },
                 },
               ],
             },
             Policies: [
               {
                 PolicyName: 'ManageEC2Instances',
                 PolicyDocument: {
                   Version: '2012-10-17',
                   Statement: [
                     {
                       Effect: 'Allow',
                       Action: %w(
                        ec2:RunInstances
                        ec2:CreateTags
                        ec2:TerminateInstances
                        ec2:DescribeInstances
                        ec2:DescribeSubnets
                        ec2:CreateVolume
                        ec2:AttachVolume
                        ec2:StopInstances
                        ec2:StartInstances
                        ec2:RequestSpotInstances
                        ec2:DescribeSpotInstanceRequests
                        ec2:DeleteTags
                       ),
                       Resource: ['*'],
                     },
                     # PassRole is deliberately scoped to the container instance role and to EC2 only; an unscoped
                     # iam:PassRole with ec2:RunInstances would allow launching instances with any role in the account.
                     # If elastic agent profiles configure a Task Role ARN, an equivalent statement is needed for those
                     # roles with iam:PassedToService of ecs-tasks.amazonaws.com.
                     {
                       Effect: 'Allow',
                       Action: 'iam:PassRole',
                       Resource: [get_att('GoCDEC2OptimizedRole', 'Arn')],
                       Condition: {
                         StringEquals: {
                           'iam:PassedToService': 'ec2.amazonaws.com',
                         },
                       },
                     },
                   ],
                 },
               },
               {
                 PolicyName: 'ManageECSInstances',
                 PolicyDocument: {
                   Version: '2012-10-17',
                   Statement: [
                     {
                       Effect: 'Allow',
                       Action: %w(
                        ecs:DescribeClusters
                        ecs:DeregisterContainerInstance
                        ecs:DescribeContainerInstances
                        ecs:ListContainerInstances
                        ecs:DescribeTaskDefinition
                        ecs:RegisterTaskDefinition
                        ecs:DeregisterTaskDefinition
                        ecs:DeleteTaskDefinitions
                        ecs:StartTask
                        ecs:StopTask
                        ecs:ListTasks
                        ecs:DescribeTasks
                       ),
                       Resource: ['*'],
                     },
                   ],
                 },
               }
             ],
           }

  resource 'GoCDEC2OptimizedRole',
           Type: 'AWS::IAM::Role',
           Properties: {
             AssumeRolePolicyDocument: {
               Statement: {
                 Effect: 'Allow',
                 Principal: {
                   Service: [
                     'ec2.amazonaws.com'
                   ]
                 },
                 Action: [
                   'sts:AssumeRole'
                 ]
               }
             },
             Policies: [
               {
                 PolicyName: 'AllowECSAgentToManageContainers',
                 PolicyDocument: {
                   Version: '2012-10-17',
                   Statement: [
                     {
                       Effect: 'Allow',
                       Action:
                         %w(
                          ecs:DiscoverPollEndpoint
                          ecs:Poll
                          ecs:RegisterContainerInstance
                          ecs:DeregisterContainerInstance
                          ecs:StartTelemetrySession
                          ecs:SubmitContainerStateChange
                          ecs:SubmitTaskStateChange,
                          logs:PutLogEvents,
                          logs:CreateLogStream
                         ),
                       Resource: [
                         '*'
                       ]
                     }
                   ]
                 }
               }
             ]
           }

  resource 'GoCDEC2OptimizedInstanceProfile',
           Type: 'AWS::IAM::InstanceProfile',
           Properties: {
             Roles: [ref('GoCDEC2OptimizedRole')]
           }
end.exec!
