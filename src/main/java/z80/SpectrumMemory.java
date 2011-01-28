package z80;

public class SpectrumMemory extends Memory {
	
	@Override
	public void set8bit(int addr, int val) {
		if((addr & ~0x3fff) != 0) {
			super.set8bit(addr, val);
		}
	}
}
