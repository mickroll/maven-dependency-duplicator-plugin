

## submod1

* src/main: contains an interface `ExampleInterface`
* src/test: contains a test that prints all class names that implement the interface `ExampleInterface`

This module creates a `test-jar` of its `src/test` contents.

## submod2

* src/main: contains an implementation of `ExampleInterface`
* src/test: not present

This module has a dependency to `submod1:jar`.

## parent

Contains configuration to automatically add a dependency `submod1:test-jar` with scope `test` where a `submod1:jar` dependency is present.

And the `surefire` configuration looks like this:

    <plugin>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
            <dependenciesToScan>
                <dependency>org.example.multimodule:*:test-jar:tests</dependency>
            </dependenciesToScan>
        </configuration>
    </plugin>

Thus the test of `submod1-test` is also run in `submod2` without the need for an explicit dependency.

