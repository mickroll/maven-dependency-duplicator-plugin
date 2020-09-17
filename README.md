![Java CI with Maven](https://github.com/mickroll/dependency-duplicator-plugin/workflows/Java%20CI%20with%20Maven/badge.svg)
![Maven Package](https://github.com/mickroll/dependency-duplicator-plugin/workflows/Maven%20Package/badge.svg)

# dependency-duplicator-plugin

This Maven plugin duplicates existing dependencies, applying configured changes. Each project is treated separate, at the beginning of the reactor build.

## Usage

Modifying your maven build to include the dependency-duplicator-plugin is done in two steps, by registering it as a build extension and adding configuration properties.

### Build extension

Add the plugin to the maven build by registering it as build extension as follows:

    <build>
        <plugins>
            <plugin>
                <groupId>com.github.madprogger</groupId>
                <artifactId>dependency-duplicator-plugin</artifactId>
                <!-- hint: don't forget to define the following property with current plugin version -->
                <version>${dependency-duplicator-plugin.version}</version>
                <extensions>true</extensions>
            </plugin>
        </plugins>
    </build>

Now it is run in any build in the project, thus being able to modify the dependency tree. The plugin itself is provided via maven central: https://repo1.maven.org/maven2/com/github/madprogger/dependency-duplicator-plugin/

Hint: any subsequent build extension will also see the modified dependency tree. So if you use multiple extensions, be sure to configure the dependency-duplicator-plugin as the first extension.

### Configuration
  
Configure this plugin like any other maven plugin, for example in the root pom of your multi module maven project, as follows:

    <configuration>
        <duplications>
            <duplication>
                <source>...</source>
                <targetScope>...</targetScope>
                <targetType>...</targetType>
                <targetClassifier>...</targetClassifier>
                <addDownstream>...</addDownstream>
            </duplication>
            [...]
        </duplications>
    </configuration>

| property | default | description |
| ---      | ---     | ---         |
| `source` | - | dependencies to duplicate, as a comma separated list in the form: `groupId:artifactId:type[:classifier], groupId:artifactId:type[:classifier], groupId:artifactId:type[:classifier]` Each dependency definition is treated as a regular expression, being matched against each existing dependency. |
| `targetClassifier`  | same as original | defines the new `classifier` of the duplicated dependency |
| `targetScope` | same as original | defines the new `scope` of the duplicated dependency |
| `targetType`  | same as original | defines the new `type` of the duplicated dependency |
| `addDownstream` | `true` | Add duplicated dependencies also to downstream projects of the project they were found in. A downstream project is a project that directly or indirectly depends on the given project. |

Each dependency is treated independently, the first matching `duplication` wins.

The configuration is read separately for each project, so different configurations may be used within the same build.
 
## Example

If the following configuration is defined in the root pom.xml of a multi module maven project that uses the dependency-duplicator-plugin build extension:

    <configuration>
        <duplications>
            <duplication>
                <source>com.example:project-1:jar, com.example:sub-.*:jar</source>
                <targetScope>test</targetScope>
                <targetType>test-jar</targetType>
            </duplication>
        </duplications>
    </configuration>
  
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

