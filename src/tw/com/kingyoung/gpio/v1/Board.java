package tw.com.kingyoung.gpio.v1;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class Board {

    private static final String TAG = "Board";
    private ParcelFileDescriptor mFileDescriptor;
    private UsbAccessory mAccessory;
    private FileInputStream mInputStream;
    private FileOutputStream mOutputStream;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private OnValueUpdateListener mListener;

    public Board() {

    }

    public void setOnValueUpdateListener(OnValueUpdateListener listener) {
	mListener = listener;
    }

    public void connect(UsbManager usbManager, UsbAccessory accessory) {
	mAccessory = accessory;
	Log.d(TAG, "description: " + mAccessory.getDescription());
	mFileDescriptor = usbManager.openAccessory(mAccessory);
	if (mFileDescriptor != null) {
	    FileDescriptor fd = mFileDescriptor.getFileDescriptor();
	    Log.d(TAG, "valid:" + fd.valid());
	    mInputStream = new FileInputStream(fd);
	    mOutputStream = new FileOutputStream(fd);
	    mHandlerThread = new HandlerThread("handler_thread");
	    mHandlerThread.start();
	    mHandler = new Handler(mHandlerThread.getLooper());
	    mHandler.post(new Runnable() {
		public void run() {
		    try {
			Packet p = new Packet(mInputStream);
			if (mListener != null) {
			    mListener.onValueUpdate(p.data[0], p.data[1]);
			}
			mHandler.post(this);
		    } catch (IOException e) {

		    }
		}
	    });
	    Log.d(TAG, String.format("accessory opened: %s", mFileDescriptor.toString()));
	} else {
	    Log.d(TAG, "accessory open fail");
	}
    }

    public void disconnect() {
	Log.d(TAG, "accessory closed");
	if (mHandlerThread != null && mHandlerThread.isAlive()) {
	    mHandlerThread.quit();
	    mHandlerThread = null;
	    mHandler = null;
	}
	try {
	    if (mOutputStream != null) {
		mOutputStream.close();
	    }
	    if(mInputStream != null) {
		mInputStream.close();
	    }
	    if (mFileDescriptor != null) {
		mFileDescriptor.close();
		mFileDescriptor = null;
	    }
	} catch (IOException e) {
	} finally {
	    mOutputStream = null;
	    mInputStream = null;
	    mFileDescriptor = null;
	    mAccessory = null;
	}
    }

    public UsbAccessory getAccessory() {
	return mAccessory;
    }

    public void registerInputPin(int pin) {
	Packet p = new Packet();
	p.type = Packet.TYPE_REG_READ;
	p.length = 1;
	p.data[0] = (byte) pin;
	p.send(mOutputStream);
    }

    public void registerOutputPin(int pin) {
	Packet p = new Packet();
	p.type = Packet.TYPE_REG_WRITE;
	p.length = 1;
	p.data[0] = (byte) pin;
	p.send(mOutputStream);
    }

    public void writeOutputPin(int pin, int val) {
	Packet p = new Packet();
	p.type = Packet.TYPE_SET;
	p.length = 2;
	p.data[0] = (byte) pin;
	p.data[1] = (byte) val;
	p.send(mOutputStream);
    }
}
