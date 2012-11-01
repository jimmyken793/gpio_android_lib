package tw.com.kingyoung.gpio.v1;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.util.Log;

public class Packet {
    public static final int MAX_PACKET_LENGTH = 14;

    public static final byte TYPE_UNDEFINED = 0x00;
    public static final byte TYPE_REG_READ = 0x01;
    public static final byte TYPE_REG_WRITE = 0x02;
    public static final byte TYPE_UNREG = 0x03;
    public static final byte TYPE_VALUE = 0x04;
    public static final byte TYPE_SET = 0x05;
    public static final byte TYPE_RESET = -0x01;

    public byte type;
    public byte length;
    public final byte[] data = new byte[MAX_PACKET_LENGTH];

    public Packet(FileInputStream is) throws IOException {
	read(is);
    }

    public Packet() {
    }

    public void read(FileInputStream is) throws IOException {
	if (is != null) {
	    synchronized (is) {
		type = (byte) is.read();
		if (type == -1) {
		    throw new IOException();
		}
		length = (byte) is.read();
		if (length == -1) {
		    throw new IOException();
		}
		    Log.d("Packet", String.format("Type: %d", type));
		    Log.d("Packet", String.format("Length: %d", length));
		int length_read = 0;
		while (length_read != length) {
		    length_read += is.read(data, length_read, length - length_read);
		}

		    Log.d("Packet", String.format("data: %d", data[0]));
	    }
	} else {
	    Log.e("Packet", "cannot read packet: disconnected device");
	}
    }

    public void send(FileOutputStream os) {
	if (os != null && type != Packet.TYPE_UNDEFINED) {
	    synchronized (os) {
		try {
		    os.write(type);
		    os.write(length);
		    os.write(data, 0, length);
		    os.flush();
		} catch (IOException e) {
		    Log.e("Packet", "write failed", e);
		}
	    }
	} else {
	    Log.e("Packet", "cannot write packet: bad packet or disconnected device");
	}
    }
}
