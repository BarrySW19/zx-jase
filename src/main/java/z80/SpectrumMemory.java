package z80;

import java.io.IOException;
import java.io.InputStream;

public class SpectrumMemory extends Memory {
	
	private ScreenBufListener listener;
	
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
		
		for(i = 0x1b2c; i <= 0x1b2f; i++) {
			System.out.println("X " + Integer.toHexString(get8bit(i)));
		}
	}
	
	@Override
	public void set8bit(int addr, int val) {
		if((addr & ~0x3fff) != 0) {
			super.set8bit(addr, val);
		}
		if(addr >= 0x4000 && addr < 0x5B00) {
//			System.out.println("SCR: 0x" + Integer.toHexString(addr)
//					+ " 0x" + Integer.toHexString(val));
			listener.update(addr, val);
		}
	}
	
	public void setListener(ScreenBufListener l) {
		this.listener = l;
	}
	
	interface ScreenBufListener {
		public void update(int addr, int val);
	}
}
