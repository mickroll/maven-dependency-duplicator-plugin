

## submod1

* src/main: not present
* src/test: contains a test that needs `commons-lang3` to be executed

This module creates a `test-jar` of its `src/test` contents.

## submod2

* src/main: not present
* src/test: not present

This module has a dependency to `submod1:jar`.

## parent

Contains configuration to automatically add a dependency `submod1:test-jar` with scope `test` where a `submod1:jar` dependency is present.

And the `surefire` configuration looks like this:

    <plugin>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
            <dependenciesToScan>
                <dependency>org.example.additional-dependencies:*:test-jar:tests</dependency>
            </dependenciesToScan>
        </configuration>
    </plugin>

Thus the test of `submod1-test` is also run in `submod2` without the need for an explicit dependency.

For the test to be able to run, an extra dependency to `commons-lang3` is also added.
