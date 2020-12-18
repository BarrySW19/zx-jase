package z80;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static z80.Registers.*;

public class CpuTestsAdc {
    private Cpu cpu;
    private Registers registers;

    @Before
    public void setUP() {
        cpu = new Cpu();
        cpu.setMemory(new Memory());
        cpu.setRegisters(registers = new Registers());
    }

    @Test
    public void test_ADC_A_N_carry_clear() {
        registers.reg[_A] = 0x10;
        registers.clearFlag(F_C);
        execute(0xce, 0x08);
        assertEquals(0x18, registers.reg[_A]);

        assertFalse(registers.isFlag(F_S));
        assertFalse(registers.isFlag(F_Z));
        assertFalse(registers.isFlag(F_H));
        assertFalse(registers.isFlag(F_PV));
        assertFalse(registers.isFlag(F_N));
        assertFalse(registers.isFlag(F_C));
    }

    @Test
    public void test_ADC_A_N2_carry_set() {
        registers.reg[_A] = 0x10;
        registers.setFlag(F_C);
        execute(0xce, 0x08);
        assertEquals(0x19, registers.reg[_A]);

        assertFalse(registers.isFlag(F_S));
        assertFalse(registers.isFlag(F_Z));
        assertFalse(registers.isFlag(F_H));
        assertFalse(registers.isFlag(F_PV));
        assertFalse(registers.isFlag(F_N));
        assertFalse(registers.isFlag(F_C));
    }

    @Test
    public void test_ADC_A_N2_carry() {
        registers.reg[_A] = 0xff;
        registers.setFlag(F_C);
        execute(0xce, 0x08);
        assertEquals(0x8, registers.reg[_A]);

        assertFalse(registers.isFlag(F_S));
        assertFalse(registers.isFlag(F_Z));
        assertTrue(registers.isFlag(F_H));
        assertFalse(registers.isFlag(F_PV));
        assertFalse(registers.isFlag(F_N));
        assertTrue(registers.isFlag(F_C));
    }

    @Test
    public void test_ADC_A_N2_sum_to_zero() {
        registers.reg[_A] = 0xff;
        registers.setFlag(F_C);
        execute(0xce, 0x00);
        assertEquals(0x0, registers.reg[_A]);

        assertFalse(registers.isFlag(F_S));
        assertTrue(registers.isFlag(F_Z));
        assertTrue(registers.isFlag(F_H));
        assertFalse(registers.isFlag(F_PV));
        assertFalse(registers.isFlag(F_N));
        assertTrue(registers.isFlag(F_C));
    }

    @Test
    public void test_ADC_A_N2_half_carry() {
        registers.reg[_A] = 0x0f;
        registers.clearFlag(F_C);
        execute(0xce, 0x01);
        assertEquals(0x10, registers.reg[_A]);

        assertFalse(registers.isFlag(F_S));
        assertFalse(registers.isFlag(F_Z));
        assertTrue(registers.isFlag(F_H));
        assertFalse(registers.isFlag(F_PV));
        assertFalse(registers.isFlag(F_N));
        assertFalse(registers.isFlag(F_C));
    }

    @Test
    public void test_ADC_A_N2_overflow() {
        registers.reg[_A] = 0x7f;
        registers.clearFlag(F_C);
        execute(0xce, 0x01);
        assertEquals(0x80, registers.reg[_A]);

        assertTrue(registers.isFlag(F_S));
        assertFalse(registers.isFlag(F_Z));
        assertTrue(registers.isFlag(F_H));
        assertTrue(registers.isFlag(F_PV));
        assertFalse(registers.isFlag(F_N));
        assertFalse(registers.isFlag(F_C));
    }

    @Test
    public void test_ADC_HL_DE_no_carry() {
        registers.setHL(0x1234);
        registers.setDE(0x2222);
        registers.clearFlag(F_C);
        execute(0xed, 0x5a);
        assertEquals(0x3456, registers.getHL());
        assertFalse(registers.isFlag(F_C));
    }

    @Test
    public void test_ADC_HL_SP_carry() {
        registers.setHL(0x1234);
        registers.setSP(0x2222);
        registers.setFlag(F_C);
        execute(0xed, 0x7a);
        assertEquals(0x3457, registers.getHL());
        assertFalse(registers.isFlag(F_C));
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
