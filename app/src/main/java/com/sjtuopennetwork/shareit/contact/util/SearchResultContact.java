package com.sjtuopennetwork.shareit.contact.util;

public class SearchResultContact {
    public String address;
    public String name;
    public String avatarhash;
    public byte[] avatar;

    public SearchResultContact(String address, String name, String avatarhash,byte[] avatar) {
        this.address = address;
        this.name = name;
        this.avatarhash=avatarhash;
        this.avatar = avatar;
    }
}
