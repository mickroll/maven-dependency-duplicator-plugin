![Java CI with Maven](https://github.com/mickroll/maven-dependency-duplicator-plugin/workflows/Java%20CI%20with%20Maven/badge.svg)
![Maven Package](https://github.com/mickroll/maven-dependency-duplicator-plugin/workflows/Maven%20Package/badge.svg)

# maven-dependency-duplicator-plugin

This Maven plugin duplicates existing dependencies, applying configured changes. Each project is treated separate, at the beginning of the reactor build.

## Usage

Modifying your maven build to include the maven-dependency-duplicator-plugin is done in two steps, by registering it as a build extension and adding configuration properties.

### Build extension

Add the plugin to the maven build by registering it as build extension as follows:

    <build>
        <extensions>
            <extension>
                <groupId>com.github.madprogger</groupId>
                <artifactId>maven-dependency-duplicator-plugin</artifactId>
                <!-- hint: don't forget to define the following property with current plugin version -->
                <version>${maven-dependency-duplicator-plugin.version}</version>
            </extension>
        </extensions>
    </build>

Now it is run in any build in the project, thus being able to modify the dependency tree.

Hint: any following build extension will also see the modified dependency tree. So if you use multiple extensions, be sure to configure the maven-dependency-duplicator-plugin as the first extension.

### Configuration
  
Configure this plugin via properties, for example in the root pom of your multi module maven project.

| property | default | description |
| ---      | ---     | ---         |
| `ddp.sourceDependencies` | - | dependencies to duplicate, as a comma separated list in the form: `groupId:artifactId:type[:classifier], groupId:artifactId:type[:classifier], groupId:artifactId:type[:classifier]` Each dependency definition is treated as a regular expression, being matched against each existing dependency. |
| `ddp.targetClassifier`  | same as original | defines the new `classifier` of the duplicated dependency |
| `ddp.targetScope` | same as original | defines the new `scope` of the duplicated dependency |
| `ddp.targetType`  | same as original | defines the new `type` of the duplicated dependency |
| `ddp.addDependenciesDownstream` | `true` | Add duplicated dependencies also to downstream projects of the project they were found in. A downstream project is a project that directly or indirectly depends on the given project. |

The property configuration is read separately for each project, so different configurations may be used within the same build.
 
## Example

If the following properties are defined in the root pom.xml of a multi module maven project that uses the maven-dependency-duplicator-plugin build extension:

    <properties>
        <ddp.sourceDependencies>com.example:project-1:jar, com.example:sub-.*:jar</ddp.sourceDependencies>
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

