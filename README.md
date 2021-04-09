# keycloak-laravel-user-federation

[Keycloak](https://www.keycloak.org/) user federation extension to connect to a MySQL database containing users (password is hashing by laravel). For creating it I followed [this guide](https://www.keycloak.org/docs/latest/server_development/index.html#_user-storage-spi) from the official documentation.

## Installation

[Download Jar file here] (https://github.com/vantienvnn/keycloak-laravel-user-federation/tree/master/resources/)


or Clone the repository:

`git clone https://github.com/vantienvnn/keycloak-laravel-user-federation.git`

Build the fat jar:
`chmod +x ./gradlew`
`./gradlew shadowJar`

Instruction for installing gradle build system can be found in the [official docs](https://gradle.org/).

I am using the [Gradle Shadown Plugin](https://imperceptiblethoughts.com/shadow/) to generate a fat jar containing all dependencies inside the jar file so that they are bundled together when deployed to keycloak.

Have a Keycloak running server. I am using docker here:

`docker run -p 8080:8080 --name keycloak_sso -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin quay.io/keycloak/keycloak:10.0.1`

Copy the jar file inside the standalone folder (was using windows when I wrote this :) ):

`docker cp build\libs\keycloak-mysql-user-federation-all.jar keycloak_sso:/opt/jboss/keycloak/standalone/deployments/keycloak-mysql-user-federation.jar`

Keycloak should load the jar:

![](docs/installation1.png)

Go to user federation and choose the User Federation:

![](docs/installation2.png)

Add the following properties:

- The database connection
- The table containing the users
- The column with the username
- The column with the password
- The hash algorithms: Bcrypt - for laravel hash pasword, or MD5 or SHA1 for other application

![](docs/installation3.png)

Save and test.
