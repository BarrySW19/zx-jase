package z80;

import static z80.Registers._PC;
import static z80.Registers.*;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class CpuTests {

	private Cpu cpu;

	@Before
	public void setUP() {
		cpu = new Cpu();
		cpu.setMemory(new Memory());
		cpu.setRegisters(new Registers());
	}

	@Test
	public void test_PUSH_POP() {
		cpu.getRegisters().setSP(0x8000);
		cpu.getRegisters().setDE(0x1776);
		execute(0xd5);
		assertEquals(0x1776, cpu.getMemory().get16bit(0x7FFE));
		assertEquals(0x7FFE, cpu.getRegisters().getSP());
		assertEquals(11L, cpu.getTStates());

		cpu.getRegisters().setDE(0x0000);
		assertEquals(0, cpu.getRegisters().getDE());

		execute(0xd1);
		assertEquals(0x1776, cpu.getRegisters().getDE());
		assertEquals(0x8000, cpu.getRegisters().getSP());
		assertEquals(21L, cpu.getTStates());
	}

	@Test
	public void test_CF() {
		assertFalse(cpu.getRegisters().isFlag(F_C));
		execute(0x37);
		assertTrue(cpu.getRegisters().isFlag(F_C));

		execute(0x3f);
		assertFalse(cpu.getRegisters().isFlag(F_C));
		assertTrue(cpu.getRegisters().isFlag(F_H));

		execute(0x3f);
		assertTrue(cpu.getRegisters().isFlag(F_C));
		assertFalse(cpu.getRegisters().isFlag(F_H));

		assertEquals(12L, cpu.getTStates());
	}

	@Test
	public void test_NOP() {
		execute(0x00);
		assertEquals(4L, cpu.getTStates());
	}

	@Test
	public void test_LD_H_B() {
		cpu.getRegisters().reg[_B] = 27;
		assertEquals(0, cpu.getRegisters().reg[_H]);

		execute(0x60);
		assertEquals(27, cpu.getRegisters().reg[_H]);
		assertEquals(4L, cpu.getTStates());
	}

	@Test
	public void test_LD_L_C() {
		cpu.getRegisters().reg[_C] = 45;
		assertEquals(0, cpu.getRegisters().reg[_L]);

		execute(0x69);
		assertEquals(45, cpu.getRegisters().reg[_L]);
		assertEquals(4L, cpu.getTStates());
	}

	@Test
	public void test_LD_L_L() {
		cpu.getRegisters().reg[_L] = 14;

		execute(0x6D);
		assertEquals(14, cpu.getRegisters().reg[_L]);
		assertEquals(4L, cpu.getTStates());
	}

	@Test
	public void test_LD_A_E() {
		cpu.getRegisters().reg[_E] = 255;
		assertEquals(0, cpu.getRegisters().reg[_A]);

		execute(0x7B);
		assertEquals(255, cpu.getRegisters().reg[_A]);
		assertEquals(4L, cpu.getTStates());
	}

	@Test
	public void test_LD_C_HL() {
		cpu.getRegisters().setHL(5000);
		cpu.getMemory().set8bit(5000, 200);
		assertEquals(200, cpu.getMemory().get8bit(5000));
		assertEquals(0, cpu.getRegisters().reg[_C]);

		execute(0x4E);
		assertEquals(200, cpu.getRegisters().reg[_C]);
		assertEquals(7L, cpu.getTStates());
	}

	@Test
	public void test_LD_HL_A() {
		cpu.getRegisters().reg[_A] = 193;
		cpu.getRegisters().setHL(6000);
		assertEquals(0, cpu.getMemory().get8bit(cpu.getRegisters().getHL()));

		execute(0x77);
		assertEquals(193, cpu.getMemory().get8bit(cpu.getRegisters().getHL()));
		assertEquals(7L, cpu.getTStates());
	}

	@Test
	public void test_ADD_A_D() {
		cpu.getRegisters().reg[_A] = 193;
		cpu.getRegisters().reg[_D] = 7;

		execute(0x82);
		assertEquals(200, cpu.getRegisters().reg[_A]);
		assertEquals(4L, cpu.getTStates());
	}

	@Test
	public void test_ADC_A_D() {
		cpu.getRegisters().reg[_A] = 255;
		cpu.getRegisters().reg[_D] = 5;

		execute(0x8A);
		assertEquals(4, cpu.getRegisters().reg[_A]);
		assertTrue(cpu.getRegisters().isFlag(F_C));

		execute(0x8A);
		assertEquals(10, cpu.getRegisters().reg[_A]);
		assertEquals(8L, cpu.getTStates());
		assertFalse(cpu.getRegisters().isFlag(F_C));
	}

	@Test
	public void test_SUB_A_H() {
		cpu.getRegisters().reg[_A] = 193;
		cpu.getRegisters().reg[_H] = 7;

		execute(0x94);
		assertEquals(186, cpu.getRegisters().reg[_A]);
		assertEquals(4L, cpu.getTStates());
	}

	@Test
	public void testFlags_SUB() {
		cpu.getRegisters().reg[_A] = 127;
		cpu.getRegisters().reg[_H] = 0xc0;

		execute(0x94);
		assertEquals(191, cpu.getRegisters().reg[_A]);
		assertTrue(cpu.getRegisters().isFlag(F_PV));
		assertEquals(4L, cpu.getTStates());
		
		cpu.getRegisters().reg[_A] = 0x91;
		cpu.getRegisters().reg[_H] = 1;
		execute(0x94);
		assertFalse(cpu.getRegisters().isFlag(F_H));

		execute(0x94);
		assertTrue(cpu.getRegisters().isFlag(F_H));
	}

	@Test
	public void test_INC_DEC_A() {
		cpu.getRegisters().reg[_A] = 4;
		execute(0x3C);
		assertEquals(5, cpu.getRegisters().reg[_A]);

		execute(0x3D);
		assertEquals(4, cpu.getRegisters().reg[_A]);
		assertEquals(8L, cpu.getTStates());
	}

	@Test
	public void test_INC_DEC_HL() {
		cpu.getRegisters().setHL(0x8000);
		assertEquals(0, cpu.getMemory().get8bit(cpu.getRegisters().getHL()));

		execute(0x34);
		assertEquals(1, cpu.getMemory().get8bit(cpu.getRegisters().getHL()));

		execute(0x35);
		assertEquals(0, cpu.getMemory().get8bit(cpu.getRegisters().getHL()));
		assertEquals(22L, cpu.getTStates());
	}

	@Test
	public void test_SBC_A_D() {
		cpu.getRegisters().reg[_A] = 4;
		cpu.getRegisters().reg[_D] = 5;

		execute(0x9A);
		assertEquals(255, cpu.getRegisters().reg[_A]);
		assertTrue(cpu.getRegisters().isFlag(F_C));

		execute(0x9A);
		assertEquals(249, cpu.getRegisters().reg[_A]);
		assertEquals(8L, cpu.getTStates());
		assertFalse(cpu.getRegisters().isFlag(F_C));
	}

	@Test
	public void test_LD_N() {
		assertEquals(0, cpu.getRegisters().reg[_E]);
		execute(0x1e, 0x7f);
		assertEquals(0x7f, cpu.getRegisters().reg[_E]);
		assertEquals(7L, cpu.getTStates());

		cpu.getRegisters().setHL(0xA000);
		assertEquals(0, cpu.getMemory().get8bit(cpu.getRegisters().getHL()));
		execute(0x36, 0xfc);
		assertEquals(0xfc, cpu.getMemory().get8bit(cpu.getRegisters().getHL()));
		assertEquals(17L, cpu.getTStates());
	}
	
	@Test
	public void testFlag_Z() {
		assertFalse(cpu.getRegisters().isFlag(F_Z));
		cpu.getRegisters().reg[_A] = 254;
		cpu.getRegisters().reg[_B] = 2;
		execute(0x80);
		assertTrue(cpu.getRegisters().isFlag(F_Z));
		execute(0x80);
		assertFalse(cpu.getRegisters().isFlag(F_Z));
	}

	@Test
	public void testFlag_C() {
		assertFalse(cpu.getRegisters().isFlag(F_C));
		cpu.getRegisters().reg[_A] = 254;
		cpu.getRegisters().reg[_B] = 6;
		execute(0x80);
		assertTrue(cpu.getRegisters().isFlag(F_C));
		execute(0x80);
		assertFalse(cpu.getRegisters().isFlag(F_C));
	}

	private void execute(int... val) {
		for (int i = 0; i < val.length; i++) {
			cpu.getMemory().set8bit(cpu.getRegisters().reg[_PC] + i, val[i]);
		}
		int executed = 0;
		while(executed < val.length) {
			int before = cpu.getRegisters().reg[_PC];
			cpu.execute();
			executed += cpu.getRegisters().reg[_PC] - before;
		}
	}
}
