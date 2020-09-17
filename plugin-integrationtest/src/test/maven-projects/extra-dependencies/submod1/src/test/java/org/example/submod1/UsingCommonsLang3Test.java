package org.example.submod1;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

/**
 * Uses {@link org.apache.commons.lang3.StringUtils}.
 *
 * @author mickroll
 */
public class UsingCommonsLang3Test {

    @Test
    void testName() throws Exception {
        System.out.println(StringUtils.upperCase("used commons-lang3!"));
    }
}
