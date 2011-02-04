package z80;

import static z80.Registers._PC;

import javax.swing.JFrame;

import org.junit.Before;
import org.junit.Test;

public class RomTests {

	private Cpu cpu;
	private Display display;
	
	@Before
	public void setUP() {
		display = new Display();
		cpu = new Cpu();
		SpectrumMemory sm = new SpectrumMemory();
		sm.setListener(display);
		cpu.setMemory(sm);
		cpu.setRegisters(new Registers());
	}
	
	@Test
	public void runFromStart() {
		JFrame jf = new JFrame();
		jf.add(display);
		jf.pack();
		jf.setVisible(true);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		while(true) {
//			System.out.println(Integer.toString(cpu.getRegisters().reg[_PC], 16)
//					+ " " + Integer.toString(cpu.getMemory().get8bit(cpu.getRegisters().reg[_PC]), 16));
			cpu.executeToInterrupt();
			if(cpu.getRegisters().iff1) {
				cpu.maskableInterrupt();
			}
		}
	}
}
