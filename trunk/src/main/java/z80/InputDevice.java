package z80;

public class InputDevice {
	public int read(int addr) {
		if((addr & 0x0100) == 0 && (addr & 0xff) == 0xfe) {
			int rv = (System.currentTimeMillis() / 1000) % 2 == 0 ? 0xfd : 0xff;
			System.out.println("key=" + Integer.toHexString(rv));
			return rv;
		}
		return 0xff;
	}
}
