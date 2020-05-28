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

  output 'AccessKey',
         Value: ref('AccessKey')

  output 'SecretKey',
         Value: get_att('AccessKey', 'SecretAccessKey')

  output 'GoCDEC2OptimizedRole',
         Value: get_att('GoCDEC2OptimizedRole', 'Arn')

  output 'GoCDEC2OptimizedInstanceProfile',
         Value: get_att('GoCDEC2OptimizedInstanceProfile', 'Arn')

  resource 'GoCDECSCluster',
           Type: 'AWS::ECS::Cluster',
           Properties: {
             ClusterName: ref('ClusterName')
           }

  resource 'AccessKey',
           Type: 'AWS::IAM::AccessKey',
           Properties: {
             UserName: ref('GoCDECSPluginUser')
           }

  resource 'GoCDECSPluginUser',
           Type: 'AWS::IAM::User',
           Properties: {
             Policies: [
               {
                 PolicyName: 'ManageEC2Instances',
                 PolicyDocument: {
                   Version: '2012-10-17',
                   Statement: [
                     {
                       Effect: 'Allow',
                       Action: %w(
                        ec2:runInstances
                        ec2:createTags
                        ec2:terminateInstances
                        ec2:describeInstances
                        ec2:describeSubnets
                        ec2:createVolume
                        ec2:attachVolume
                        ec2:stopInstances
                        ec2:startInstances
                        ec2:requestSpotInstances
                        ec2:describeSpotInstanceRequests
                        ec2:deleteTags
                        iam:PassRole
                        iam:GetRole
                       ),
                       Resource: ['*'],
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
                        ecs:describeClusters
                        ecs:deregisterContainerInstance
                        ecs:describeContainerInstances
                        ecs:listContainerInstances
                        ecs:registerTaskDefinition
                        ecs:deregisterTaskDefinition
                        ecs:startTask
                        ecs:stopTask
                        ecs:listTasks
                        ecs:describeTasks
                        ecs:describeTaskDefinition
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
                          ecs:describeClusters
                          ecs:discoverPollEndpoint
                          ecs:registerContainerInstance
                          ecs:deregisterContainerInstance
                          ecs:poll
                          ecs:startTelemetrySession
                          ecs:submitContainerStateChange
                          ecs:submitTaskStateChange
                          logs:CreateLogStream
                          logs:PutLogEvents
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
