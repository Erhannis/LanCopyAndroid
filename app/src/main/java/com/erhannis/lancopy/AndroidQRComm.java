package com.erhannis.lancopy;

import com.erhannis.lancopy.refactor.Advertisement;
import com.erhannis.lancopy.refactor.Comm;
import com.erhannis.lancopy.refactor2.CommChannel;

import java.util.Objects;

public class AndroidQRComm extends Comm {
    public static final String TYPE = "QR";
    
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
        SwingQRCommFrame sqcf = new SwingQRCommFrame(dataOwner, this);
        sqcf.setVisible(true);
        return sqcf.channel;
    }
}
