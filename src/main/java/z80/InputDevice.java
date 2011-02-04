package z80;

public class InputDevice {
	public int read(int addr) {
		if((addr & 0x0100) == 0 && (addr & 0xff) == 0xfe) {
			System.out.println("Z");
			return (System.currentTimeMillis() / 1000) % 2 == 0 ? 0xfd : 0xff;
		}
		return 0;
	}
}
