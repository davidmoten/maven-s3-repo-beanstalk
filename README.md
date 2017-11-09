# maven-s3-repo-beanstalk
Private maven repository backed by AWS S3 storage running on AWS Beanstalk

## How to deploy
To deploy, call:

    ./deploy.sh <MODE> 

If you are behind a proxy:

    ./deploy.sh <MODE> -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080
    
## How to access
The application is accessible at https://<BEANSTALK_ENVIRONMENT_URL>/repo and is 
authenticated using basic authentication. The default credentials are user=me, password=changeit.

## How to set credentials
When the application starts up the file `/authentication.properties` is loaded from the classpath with the authentication details. The password is not stored there, just an HMAC hash. 

To regenerate `authentication.properties` with new authentication details, run this:

    mvn test -Duser=<USER> -Dpassword=<PASSWORD>

The file `src/main/resources/authentication.properties` will be overwritten and you should commit that to source control and then redeploy the application.

 
