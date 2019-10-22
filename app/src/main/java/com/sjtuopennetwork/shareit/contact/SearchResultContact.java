package com.sjtuopennetwork.shareit.contact;

public class SearchResultContact {
    String address;
    String name;
    String avatarhash;
    byte[] avatar;

    public SearchResultContact(String address, String name, String avatarhash,byte[] avatar) {
        this.address = address;
        this.name = name;
        this.avatarhash=avatarhash;
        this.avatar = avatar;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarhash() {
        return avatarhash;
    }

    public void setAvatarhash(String avatarhash) {
        this.avatarhash = avatarhash;
    }

    public byte[] getAvatar() {
        return avatar;
    }

    public void setAvatar(byte[] avatar) {
        this.avatar = avatar;
    }

}
