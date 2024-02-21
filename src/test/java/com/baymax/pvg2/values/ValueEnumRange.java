package com.baymax.pvg2.values;

import java.io.Serializable;
import java.math.BigDecimal;

/** @author xiao.hu create time:2024-02-21 12:03:49 */
public final class ValueEnumRange implements Serializable {

  public static final class student {
    public enum float_card {
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

