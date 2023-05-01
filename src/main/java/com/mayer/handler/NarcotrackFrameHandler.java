package com.mayer.handler;

import com.mayer.NarcotrackListener;
import com.mayer.NarcotrackPackageType;

import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class NarcotrackFrameHandler implements NarcotrackFrameHandlerInterface {


    protected final NarcotrackListener narcotrackListener;
    protected final NarcotrackPackageType packageType;
    private final byte  startByte = (byte)0xFF;
    protected final byte[] raw;
    protected PreparedStatement statement;
    public NarcotrackFrameHandler(NarcotrackListener narcotracklistener, NarcotrackPackageType packageType, String rawStatement, boolean returnGeneratedKeys) throws SQLException {
        this.narcotrackListener = narcotracklistener;
        if (returnGeneratedKeys) {
            statement = narcotracklistener.getDatabaseConnection().prepareStatement(rawStatement, Statement.RETURN_GENERATED_KEYS);
        } else {
            statement = narcotracklistener.getDatabaseConnection().prepareStatement(rawStatement);
        }
        this.packageType = packageType;
        raw = new byte[packageType.getLength()];
    }

    public boolean detected(ByteBuffer buffer) {
        if(buffer.position() < packageType.getLength()) return false;
        if(buffer.get(buffer.position() - packageType.getLength()) != startByte) return false;
        if(buffer.get(buffer.position() - packageType.getLength() + 3) != packageType.getIdentifier()) return false;
        return true;
    }

    protected int getTimeDifference() {
        return Math.toIntExact(System.currentTimeMillis() - narcotrackListener.getStartTime());
    }

    protected void handleRemains(ByteBuffer buffer) {
        if (buffer.position() - packageType.getLength() > 0) {
            narcotrackListener.moveBufferToRemains(packageType.getLength());
        } else {
            buffer.clear();
            System.out.println("Cleared buffer");
        }
    }


}
