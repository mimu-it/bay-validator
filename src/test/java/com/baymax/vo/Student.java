package com.baymax.vo;

/**
 * @author xiao.hu
 * @date 2022-03-31
 * @apiNote
 */
public class Student extends BaseVO{

    private Integer largeNumber;
    private String phoneNumber;

    public Integer getLargeNumber() {
        return largeNumber;
    }

    public void setLargeNumber(Integer largeNumber) {
        this.largeNumber = largeNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
