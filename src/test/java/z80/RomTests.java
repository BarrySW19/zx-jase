package z80;

import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class RomTests {

	private Cpu cpu;
	private Display display;
	
	@Before
	public void setUP() {
		display = new Display();
		cpu = new Cpu();
		SpectrumMemory sm = new SpectrumMemory();
		
		for(int i = 0x11df; i <= 0x11df; i++) {
			System.out.println(Integer.toHexString(i) + " " + Integer.toHexString(sm.get8bit(i)));
		}
		
		sm.setListener(display);
		cpu.setMemory(sm);
		cpu.setRegisters(new Registers());
		cpu.setKeyboard(display);
	}

	/*
	@Test
	public void compare() {
		ILogger log = new ConsoleLogger();
		Z80 z80 = new Z80();
		Spectrum spectrum = new Spectrum();
		spectrum.init(z80, log);
		z80.init(spectrum, log);
		z80.stop();

		for(int i = 0; i < 1000000; i++) {
			System.out.println("Instr: " + i + ", pc=" + Integer.toHexString(
					cpu.getRegisters().reg[_PC]) + ", ts=" + cpu.getTStates());
			if(i == 639272) {
				i += 0;
			}
			z80.emulate();
			cpu.execute();
			checkInSync(z80);
		}
	}
	
	private void checkInSync(Z80 z80) {
		assertEquals("PC", cpu.getRegisters().reg[_PC], z80.m_pc16);
		assertEquals("SP", cpu.getRegisters().getSP(), z80.m_sp16);
		
		assertEquals("A", cpu.getRegisters().reg[_A], z80.m_a8);
		assertEquals("B", cpu.getRegisters().reg[_B], z80.m_b8);
		assertEquals("C", cpu.getRegisters().reg[_C], z80.m_c8);
		assertEquals("D", cpu.getRegisters().reg[_D], z80.m_d8);
		assertEquals("E", cpu.getRegisters().reg[_E], z80.m_e8);
		assertEquals("F", t8(cpu.getRegisters().reg[_F]), t8(z80.getF()));
		assertEquals("H", cpu.getRegisters().reg[_H], z80.m_h8);
		assertEquals("L", cpu.getRegisters().reg[_L], z80.m_l8);
	}*/

	@Test
	public void dumpRom() throws IOException {
		final InputStream is = ClassLoader.getSystemResourceAsStream("original.rom");
		int i;
		int idx = 0;
		byte[] rom = new byte[16384];
		while ((i = is.read()) != -1) {
			rom[idx++] = (byte) i;
		}

		PrintStream fs = new PrintStream(new FileOutputStream("/Users/barsmi/rom"));
		for(int j = 0; j < rom.length; j += 16) {
			fs.print(String.format("%04x", j));
			for(int k = 0; k < 16; k++) {
				fs.print(String.format(" %02x", rom[j+k]));
			}
			fs.print("\n");
		}
		fs.close();
	}

	@Test
	public void runFromStart() throws IOException {
		JFrame jf = new JFrame();
		jf.add(display);
		jf.pack();
		jf.setVisible(true);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.addKeyListener(display);

		Z80Snapshot snapshot = new Z80Snapshot();
		snapshot.loadIntoCpu(cpu);
		
		while(true) {
//			System.out.println(Integer.toString(cpu.getRegisters().reg[_PC], 16)
//					+ " " + Integer.toString(cpu.getMemory().get8bit(cpu.getRegisters().reg[_PC]), 16));
			cpu.executeToInterrupt();
			if(cpu.getRegisters().iff1) {
				cpu.maskableInterrupt();
			}
			display.interrupt();
		}
	}
	
	private String t8(int arg) {
		String rv = "00000000" + Integer.toBinaryString(arg);
		return rv.substring(rv.length() - 8);
	}
}
