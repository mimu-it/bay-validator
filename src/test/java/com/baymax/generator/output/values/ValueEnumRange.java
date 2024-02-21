package com.baymax.generator.output.values;

import java.io.Serializable;
import java.lang.String;

/** @author xiao.hu create time:2024-02-21 12:03:38 */
public final class ValueEnumRange implements Serializable {

  public static final class student {
    public enum domain {
      STRING_bearing("bearing"),
      STRING_pad("pad"),
      STRING_rotor("rotor");

      private final String value;

      domain(final String newValue) {
        value = newValue;
      }

      public String val() {
        return value;
      }
    }
  }
}

