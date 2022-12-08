package com.baymax.pvg2.values;

import java.math.BigDecimal;

<<<<<<< HEAD
/** @author xiao.hu create time:2022-12-07 19:30:34 */
=======
/** @author xiao.hu create time:2022-04-03 10:02:19 */
>>>>>>> 6abd7707bbcc777e32eb6a1aa1a12aedd9333838
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

