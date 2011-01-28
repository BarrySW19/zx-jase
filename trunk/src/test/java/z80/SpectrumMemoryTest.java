package z80;

import org.junit.Test;
import static org.junit.Assert.*;

public class SpectrumMemoryTest {

	@Test
	public void testRomWrite() {
		SpectrumMemory mem = new SpectrumMemory();
		mem.set16bit(0, 0x7f7f);
		assertEquals(0, mem.get16bit(0));

		mem.set8bit(0, 0x7f);
		assertEquals(0, mem.get8bit(0));
	}

	@Test
	public void testRamWrite() {
		SpectrumMemory mem = new SpectrumMemory();
		mem.set16bit(0x8000, 0x7f7f);
		assertEquals(0x7f7f, mem.get16bit(0x8000));

		mem.set8bit(0x6000, 0xff);
		assertEquals(0xff, mem.get8bit(0x6000));
	}
}
