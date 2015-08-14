package org.wordpress.android.wallet;

/**
 * Created by nick on 1/3/15.
 */

public class Payment {

    private String id;
    private String message;

    public int getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(int confirmed) {
        this.confirmed = confirmed;
    }

    private int confirmed;

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }

    private String receipt;

    public String getPost() {
        return post;
    }

    public void setPost(String post) {
        this.post = post;
    }

    private String post;

    public void setId(String _id){this.id=_id;}
    public void setMessage(String _message){this.message=_message;}

    public String getId(){return this.id;}
    public String getMessage(){return this.message;}

}
