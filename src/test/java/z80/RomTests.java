package z80;

import org.junit.Before;
import org.junit.Test;
import static z80.Registers.*;

public class RomTests {

	private Cpu cpu;
	
	@Before
	public void setUP() {
		cpu = new Cpu();
		cpu.setMemory(new SpectrumMemory());
		cpu.setRegisters(new Registers());
	}
	
	@Test
	public void runFromStart() {
		for(int i = 0; i < 50; i++) {
			System.out.println(Integer.toString(cpu.getRegisters().reg[_PC], 16)
			+ " " + Integer.toString(cpu.getMemory().get8bit(cpu.getRegisters().reg[_PC]), 16));
			cpu.execute();
		}
	}
}
