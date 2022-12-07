package com.carbongators.myfootprint;

public class FriendModel {
    String name;
    String email;
    String friendCode;
    int image;

    public FriendModel(String name, String email, String friendCode, int image) {
        this.name = name;
        this.email = email;
        this.friendCode = friendCode;
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getFriendCode() {
        return friendCode;
    }

    public int getImage() {
        return image;
    }
}
