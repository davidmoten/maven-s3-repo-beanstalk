# maven-s3-repo-beanstalk
Private maven repository backed by AWS S3 storage running on AWS Beanstalk

To deploy, call:

    ./deploy.sh <MODE> 
    
The application is accessible at https://<BEANSTALK_ENVIRONMENT_URL>/repo and is 
authenticated using basic authentication.
