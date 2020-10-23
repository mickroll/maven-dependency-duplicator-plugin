![Java CI with Maven](https://github.com/mickroll/dependency-duplicator-plugin/workflows/Java%20CI%20with%20Maven/badge.svg)
![Maven Package](https://github.com/mickroll/dependency-duplicator-plugin/workflows/Maven%20Package/badge.svg)

# dependency-duplicator-plugin

This Maven plugin duplicates existing dependencies, applying configured changes. Each project is treated separate, at the beginning of the reactor build.

## Use Case / Motivation

Imagine a bunch of maven modules that provide components for different things of your project:
 * base classes for your entities
 * validation annotations and their validators
 * interceptors to be used for different things

Now you want to be sure, that each of those is used in the according way:
 * all your entities should extend the defined base classes
 * data objects should use your validation annotations
 * endpoints should be annotated with your interceptors

To enforce these rules, you may write tests (eg. using [ArchUnit](https://github.com/TNG/ArchUnit)). But those tests need the modules in their classpath where your base components are used. Now you may be tempted to create a testing-module with dependencies to all the relevant maven modules (that is about the whole project) and write compliance tests there. But these tests would now be executed at the very end of your build reactor. Thats probably minutes away from the code smell! And after fixing the code smell, you need to both compile the fixed module and run the test-module again, checking compliance of your whole project instead of that one changed module!

Or you create a test-jar for your compliance tests and use the dependency-duplicator-plugin to automatically duplicate an existing dependency to a base module and modify it to reference the test-jar. A bit of config to the surefire-plugin and your tests are executed wherever they are needed: if a moduleX has a dependency to baseModuleY, tests of baseModuleY are executed in moduleX. Simple as that. So now **the build of a module will fail, if it violates the rules that are defined in one of its dependencies**.

Now one may argue, that not only moduleX may use the base components, but also its dependent projects. Thinking transicitve dependencies. And those may introduce violating usages of base components, too. Thats right. Use the `addDownstream=true` config to also execute the tests there and have those covered as well.

There is no need to explicitly use the `test-jar`, you may use a custom classifier for this. After all, that's just an example use case.

## Usage

Modifying your maven build to include the dependency-duplicator-plugin is done in two steps, by registering it as a build extension and adding configuration.

### Build extension

Add the plugin to the maven build by registering it as build extension as follows:

    <build>
        <plugins>
            <plugin>
                <groupId>com.github.madprogger</groupId>
                <artifactId>dependency-duplicator-plugin</artifactId>
                <!-- hint: don't forget to define the following property with current plugin version -->
                <version>${dependency-duplicator-plugin.version}</version>
                <!-- this is important, so the plugin is effectively used as a build extension -->
                <extensions>true</extensions>
            </plugin>
        </plugins>
    </build>

Now it is run in any build in the project, thus being able to augment the dependency tree. The plugin itself is provided via maven central: https://repo1.maven.org/maven2/com/github/madprogger/dependency-duplicator-plugin/

Hint: any subsequent build extension will also see the modified dependency tree. So if you use multiple extensions, be sure to configure the dependency-duplicator-plugin as the first extension.

### Configuration
  
Configure this plugin like any other maven plugin, for example in the root pom of your multi module maven project, as follows:

    <configuration>
        <duplications>
            <duplication>
                <dependencyKeys>
                    <dependencyKey>...</dependencyKey>
                    [...]
                </dependencyKeys>
                <targetScope>...</targetScope>
                <targetType>...</targetType>
                <targetClassifier>...</targetClassifier>
                <addDownstream>...</addDownstream>
                <additionalDependencies>
                    <additionalDependency>
                        <groupId>...</groupId>
                        <artifactId>...</artifactId>
                        <version>...</version>
                        [...]
                    </additionalDependency>
                    [...]
                </additionalDependencies>
            </duplication>
            [...]
        </duplications>
    </configuration>

| key | default&nbsp;value | description |
| ---      | ---     | ---         |
| `dependencyKeys` | - | dependencies to duplicate, in the form: `groupId:artifactId:type[:classifier]` Each dependency definition is treated as a regular expression, being matched against each existing dependency. |
| `targetClassifier`  | same as original | defines the new `classifier` of the duplicated dependency |
| `targetScope` | same as original | defines the new `scope` of the duplicated dependency |
| `targetType`  | same as original | defines the new `type` of the duplicated dependency |
| `addDownstream` | `true` | Add duplicated dependencies also to downstream projects of the project they were found in. A downstream project is a project that directly or indirectly depends on the given project. |
| `additionalDependencies` | empty | additional dependencies to add, if `dependencyKeys` matched a dependency |

Each dependency is treated independently, the first matching `duplication` wins.

The configuration is read separately for each project, so different configurations may be used within the same build reactor.
 
## Example

If the following configuration is defined in the root pom.xml of a multi module maven project that uses the dependency-duplicator-plugin build extension:

    <configuration>
        <duplications>
            <duplication>
                <dependencyKeys>
                    <dependencyKey>org.example:project-1:jar</dependencyKey>
                    <dependencyKey>org.example:sub-.*:jar</dependencyKey>
                </dependencyKeys>
                <targetScope>test</targetScope>
                <targetType>test-jar</targetType>
            </duplication>
        </duplications>
    </configuration>
  
Now if a module in this maven project has the following dependencies:

    <dependency>
        <groupId>org.example</groupId>
        <artifactId>project-1</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.example</groupId>
        <artifactId>sub-five</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>

Then the maven build of this module will run, as if the following dependencies were present in the pom.xml:
    
    <dependency>
        <groupId>org.example</groupId>
        <artifactId>project-1</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.example</groupId>
        <artifactId>sub-five</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.example</groupId>
        <artifactId>project-1</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <type>test-jar</type>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.example</groupId>
        <artifactId>sub-five</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <type>test-jar</type>
        <scope>test</scope>
    </dependency>
 
Combined with a `maven-surefire-plugin` configuration as follows, this will execute the tests from the `test-jar` artifacts:
 
    <plugin>
        <artifactId>maven-surefire-plugin</artifactId>
        <executions>
            <execution>
                <id>run-shared-tests-from-test-jars</id>
                <goals>
                    <goal>test</goal>
                </goals>
                <phase>test</phase>
                <configuration>
                    <dependenciesToScan>
                        <dependency>org.example:*:test-jar</dependency>
                    </dependenciesToScan>
                </configuration>
            </execution>
        </executions>
    </plugin>

That's the setup you need for the scenario described in the beginning.
And if you don't know how to create that test-jar of a module in the first place:
*snip*

    <plugin>
        <artifactId>maven-jar-plugin</artifactId>
        <executions>
            <execution>
                <goals>
                    <goal>test-jar</goal>
                </goals>
            </execution>
        </executions>
    </plugin>

The integration tests show further example projects: [plugin-integrationtest/src/test/maven-projects](plugin-integrationtest/src/test/maven-projects)

