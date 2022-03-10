package com.baymax.pvg2.values;

import java.math.BigDecimal;

/** @author xiao.hu create time:2022-03-10 21:49:59 */
public final class ValueEnumRange {

  public static final class student {
    public static enum float_card {
      NUMBER_3$11(new BigDecimal("3.11")),
      NUMBER_4000000000$123456(new BigDecimal("4000000000.123456"));

      private final BigDecimal value;

      float_card(final BigDecimal newValue) {
        value = newValue;
      }

      public BigDecimal val() {
        return value;
      }
    }
  }
}

