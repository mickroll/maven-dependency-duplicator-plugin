# maven-dependency-duplicator-plugin

This Maven plugin duplicates existing dependencies. Each project is treated separate, at the beginning of the reactor build.
  
Configure this plugin via properties, for example in the root pom of your multi module maven project.

- `ddp.sourceDependencies`: dependencies to duplicate, as a comma separated list in the form: `groupId:artifactId:type, groupId:artifactId:type, groupId:artifactId:type`
    Each dependency definition is treated as a regular expression, being matched against each existing dependency.
- `ddp.targetScope`: defines the new `scope` of the duplicated dependency
- `ddp.targetType`: defines the new `type` of the duplicated dependency
  </ul>
  
## Example

If the following properties are defined in the root pom.xml of a multi module maven project:

    <properties>
        <ddp.sourceDependencies>com.example:project-1:jar, com.example:sub-.:jar</ddp.sourceDependencies>
        <ddp.targetType>test-jar</ddp.targetType>
        <ddp.targetScope>test</ddp.targetScope>
    </properties> 

  
Now if a module in this maven project has the following dependencies:

    <dependency>
        <groupId>com.example</groupId>
        <artifactId>project-1</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>sub-five</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>

Then the maven build of this module will run, as if the following dependencies were present in the pom.xml:
    
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>project-1</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>sub-five</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>project-1</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <type>test-jar</type>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>sub-five</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <type>test-jar</type>
        <scope>test</scope>
    </dependency>

