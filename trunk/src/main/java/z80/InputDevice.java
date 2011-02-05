package z80;

public class InputDevice {
	public int read(int addr) {
		System.out.println("addr=" + Integer.toHexString(addr));
		if(addr == 0xbffe) {
			int rv = (System.currentTimeMillis() / 1000) % 2 == 0 ? 0xf7 : 0xff;
			// System.out.println("key=" + Integer.toHexString(rv));
			return rv;
		}
		return 0xff;
	}
}
