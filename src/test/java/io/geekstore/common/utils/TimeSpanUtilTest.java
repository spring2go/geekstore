/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.common.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class TimeSpanUtilTest {
    @Test
    public void testTimeSpanUtil() {
        long result = TimeSpanUtil.toMs("2 days");
        assertThat(result).isEqualTo(172800000);

        result = TimeSpanUtil.toMs("2 DAYS");
        assertThat(result).isEqualTo(172800000);

        result = TimeSpanUtil.toMs("1y");
        assertThat(result).isEqualTo(31536001024L);

        result = TimeSpanUtil.toMs("100");
        assertThat(result).isEqualTo(100);

        result = TimeSpanUtil.toMs("1m");
        assertThat(result).isEqualTo(60000);

        result = TimeSpanUtil.toMs("1h");
        assertThat(result).isEqualTo(3600000);

        result = TimeSpanUtil.toMs("2.5 hrs");
        assertThat(result).isEqualTo(9000000);

        result = TimeSpanUtil.toMs("2d");
        assertThat(result).isEqualTo(172800000);

        result = TimeSpanUtil.toMs("3w");
        assertThat(result).isEqualTo(1814400000);

        result = TimeSpanUtil.toMs("1s");
        assertThat(result).isEqualTo(1000);

        result = TimeSpanUtil.toMs("100ms");
        assertThat(result).isEqualTo(100);

        result = TimeSpanUtil.toMs("1.5h");
        assertThat(result).isEqualTo(5400000);

        result = TimeSpanUtil.toMs("1   s");
        assertThat(result).isEqualTo(1000);

        result = TimeSpanUtil.toMs("1.5H");
        assertThat(result).isEqualTo(5400000);

        result = TimeSpanUtil.toMs(".5ms");
        assertThat(result).isEqualTo(0);

        result = TimeSpanUtil.toMs("-100ms");
        assertThat(result).isEqualTo(-100);

        result = TimeSpanUtil.toMs("-1.5h");
        assertThat(result).isEqualTo(-5400000);

        result = TimeSpanUtil.toMs("-10.5h");
        assertThat(result).isEqualTo(-37800000);

        result = TimeSpanUtil.toMs("-.5h");
        assertThat(result).isEqualTo(-1800000);
    }
}
