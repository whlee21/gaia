package gaia.utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class DeepCopy {
	public static <T> T copy(Object orig) {
		T obj = null;
		try {
			SingleThreadByteArrayOutputStream baos = new SingleThreadByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(baos);
			out.writeObject(orig);
			out.flush();
			out.close();
			ObjectInputStream in = new ObjectInputStream(baos.getInputStream());
			obj = (T) in.readObject();
			in.close();
			baos.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e2) {
			throw new RuntimeException(e2);
		}
		return obj;
	}
}
