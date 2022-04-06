package com.erhannis.lancopy;

import android.content.Intent;

import com.erhannis.lancopy.refactor.Advertisement;
import com.erhannis.lancopy.refactor.Comm;
import com.erhannis.lancopy.refactor2.CommChannel;
import com.erhannis.lancopy.ui.main.QRActivity;

import java.util.Objects;
import java.util.UUID;

public class AndroidQRComm extends Comm {
    public static final String TYPE = "QR";
    public static final String TOKEN_KEY = "QR_COMM_TOKEN";

    public AndroidQRComm(Advertisement owner) {
        super(owner, TYPE, 7);
    }

    private AndroidQRComm() {
        this(null);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) return false;
        AndroidQRComm o = (AndroidQRComm)obj;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }

    @Override
    public String toString() {
        return super.toString()+"{"+"}";
    }

    @Override
    public Comm copyToOwner(Advertisement owner) {
        return new AndroidQRComm(owner);
    }

    @Override
    public CommChannel connect(DataOwner dataOwner) throws Exception {
        //QRActivity qra = new QRActivity(dataOwner, this);
        //sqcf.setVisible(true);
        //return sqcf.channel;
        //DO //TODO Fix

        String token = UUID.randomUUID().toString();
        MyApplication.HORRIBLE_SINGLETON.put(token, (wait no) this); //TODO Clean up

        Intent intent = new Intent(MyApplication.getContext(), QRActivity.class);
        intent.putExtra(TOKEN_KEY, token);
        MyApplication.getContext().startActivity(intent);

        throw new RuntimeException("FIX");
    }
}
