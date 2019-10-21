package com.sjtuopennetwork.shareit.contact;

public class SearchResultContact {
    String address;
    String name;
    byte[] avatar;
    String avatarPath;

    public SearchResultContact(String address, String name, byte[] avatar,String avatarPath) {
        this.address = address;
        this.name = name;
        this.avatar = avatar;
        this.avatarPath=avatarPath;
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

    public byte[] getAvatar() {
        return avatar;
    }

    public void setAvatar(byte[] avatar) {
        this.avatar = avatar;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }
}
