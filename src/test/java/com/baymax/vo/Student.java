package com.baymax.vo;

import java.math.BigDecimal;

/**
 * @author xiao.hu
 * @date 2022-03-31
 * @apiNote
 */
public class Student extends BaseVO {

    private Integer largeNumber;
    private String phoneNumber;
    private BigDecimal money;
    private String gender;
    private Long gameLongCard;
    private float floatCard;

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

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Long getGameLongCard() {
        return gameLongCard;
    }

    public void setGameLongCard(Long gameLongCard) {
        this.gameLongCard = gameLongCard;
    }

    public float getFloatCard() {
        return floatCard;
    }

    public void setFloatCard(float floatCard) {
        this.floatCard = floatCard;
    }
}
