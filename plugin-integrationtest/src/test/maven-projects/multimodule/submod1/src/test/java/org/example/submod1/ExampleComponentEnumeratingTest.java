package org.example.submod1;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.ReflectionSupport;

/**
 * Finds all implementations of {@link ExampleInterface} and prints their names to stdout.
 *
 * @author mickroll
 */
public class ExampleComponentEnumeratingTest {

    @Test
    void testName() throws Exception {
        final List<Class<?>> classes = ReflectionSupport.findAllClassesInPackage(
                "org.example",
                c -> ExampleInterface.class.isAssignableFrom(c) && !ExampleInterface.class.equals(c),
                c -> true);

        classes.stream().map(Class::getName).map(name -> "found class: " + name).forEach(System.out::println);
    }
}
