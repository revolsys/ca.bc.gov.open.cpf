## Maven

The CPF is developed using the [Apache Maven build tool](https://maven.apache.org/). All CPF plug-ins
must be developed using Maven as it manages the dependencies between the CPF components and external
libraries. Developers must have an understanding of developing applications using Maven.

Maven 3.0.x is required and can be downloaded from the following sites.

* [Maven Command line tools](https://maven.apache.org/)
* [Eclipse Maven plug-in](https://eclipse.org/m2e/)

See [Maven: The Complete Reference](https://www.sonatype.com/books/mvnref-book/reference/)
for details on developing with Maven.

### Dependency Version Properties

The following code example shows how to include a dependency to the CPF client API using a property
to specify the version. In a multi-module project the property should be put in the parent pom.xml.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project
  xmlns="https://maven.apache.org/POM/4.0.0"
  xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="https://maven.apache.org/POM/4.0.0 https://maven.apache.org/maven-v4_0_0.xsd"
>
  :
  <dependencies>
  :
    <dependency>
    <groupId>ca.bc.gov.open.cpf</groupId>
    <artifactId>cpf-api-client</artifactId>
    <version>${ca.bc.gov.open.cpf.version}</version>
    </dependency>
  </dependencies>
  
  <properties>
    <ca.bc.gov.open.cpf.version>${project.version}</ca.bc.gov.open.cpf.version>
  </properties>
</project>
```

<b>NOTE:</b> A property is used to define the CPF version. In the -SNAPSHOT suffix can be appended
to the version to indicate that the most recent development snapshot is to be used. This is required
if a new feature of the CPF client API is required that has not yet been released. During a release
process a .RC* (release candidate) version might be used.

In these cases Include the following in the pom.xml for each version property. Replace the text
<b>VERSION</b> with the version without the -SNAPSHOT or .RC* suffix.

```xml
<project>
  :
  <build>
  :
    <plugins>
  :
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>versions-maven-plugin</artifactId>
        <version>1.3.1</version>
        <configuration>
          <properties>
            <property>
              <name>ca.bc.gov.open.cpf.version</name>
              <version>[VERSION,),[VERSION.RC,)</version>
            </property>
          </properties>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

The [CodeHaus Versions Maven Plugin](https://mojo.codehaus.org/versions-maven-plugin/update-properties-mojo.html)
can update the -SNAPSHOT or .RC* dependency to the release version.
  
mvn org.codehaus.mojo:versions-maven-plugin:2.0:use-releases -DgenerateBackupPoms=false
