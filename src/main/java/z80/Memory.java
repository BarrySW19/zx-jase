package z80;

public class Memory {
	private int[] ram = new int[65536];

	public int get8bit(int addr) {
		return ram[addr & 0xffff];
	}

	public int get16bit(int addr) {
		return get8bit(addr) | (get8bit(addr + 1) << 8);
	}

	public void set8bit(int addr, int val) {
		ram[addr & 0xffff] = (val & 0xff);
	}

	public void set16bit(int addr, int val) {
		set8bit(addr, (byte) (val & 0xff));
		set8bit(addr + 1, (byte) ((val & 0xff00) >> 8));
	}
}
