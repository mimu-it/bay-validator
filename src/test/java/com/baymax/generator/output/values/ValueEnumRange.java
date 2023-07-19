package com.baymax.generator.output.values;

import java.io.Serializable;
import java.math.BigInteger;

/** @author xiao.hu create time:2023-03-09 11:21:34 */
public final class ValueEnumRange implements Serializable {

  public static final class order {
    public static enum status {
      unpaid_0(new BigInteger("0")),
      cancel_1(new BigInteger("1")),
      paid_2(new BigInteger("2")),
      refund_3(new BigInteger("3")),
      received_4(new BigInteger("4")),
      refund_return_5(new BigInteger("5")),
      reject_6(new BigInteger("6"));

      private final BigInteger value;

      status(final BigInteger newValue) {
        value = newValue;
      }

      public BigInteger val() {
        return value;
      }
    }
  }
}

