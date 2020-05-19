# Associate the job with an elastic agent profile

The Elastic Agent Profile is used to define the configuration of a docker container. It is used to configure the docker image, set memory limits, provide docker command and environment variables.

The following configuration associates a GoCD job with an Elastic Agent Profile. It effectively means that the plugin will run your job on a newly created docker container using configuration defined in that elastic agent profile.

### Steps to configure job to use ECS Elastic Agent:

* Click the gear icon on **_Pipeline_**

    ![Go To Pipeline Settings](images/configure-job/go_to_settings.png)

* Navigate to job

    ![Navigate to Job](images/configure-job/navigate_to_job.png)

* Click on **_Job Settings_**, and add profile id

    ![Configure Elastic Agent Profile](images/configure-job/configure_ea_profile_id.png)

* Save your changes
