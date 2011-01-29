package z80;

import java.io.IOException;
import java.io.InputStream;

public class SpectrumMemory extends Memory {
	
	public SpectrumMemory() {
		InputStream is = ClassLoader.getSystemResourceAsStream("original.rom");
		int i;
		int idx = 0;
		try {
			while((i = is.read()) != -1) {
				super.set8bit(idx++, i);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void set8bit(int addr, int val) {
		if((addr & ~0x3fff) != 0) {
			super.set8bit(addr, val);
		}
	}
}
