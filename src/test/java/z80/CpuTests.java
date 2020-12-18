package z80;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static z80.Registers._PC;
import static z80.Registers.*;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class CpuTests {

    private Cpu cpu;
    private Memory memory;
    private Registers registers;

    @Before
    public void setUP() {
        cpu = new Cpu();
        cpu.setMemory(memory = new Memory());
        cpu.setRegisters(registers = new Registers());
    }

    @Test // 0x00
    public void test_NOP() {
        int pc = cpu.getRegisters().getPC();
        execute(0x00);
        assertEquals(4L, cpu.getTStates());
        assertEquals(pc + 1, cpu.getRegisters().getPC());
    }

    @Test // 00dd0001 0x01, 0x11, 0x21, 0x31
    public void test_LD_dd_NN() {
        execute(0x01, 0x00, 0x10);
        assertEquals(0x1000, registers.getBC());

        execute(0x11, 0xa0, 0x20);
        assertEquals(0x20a0, registers.getDE());

        execute(0x21, 0xb0, 0x30);
        assertEquals(0x30b0, registers.getHL());

        execute(0x31, 0xc0, 0x40);
        assertEquals(0x40c0, registers.getSP());
    }

    @Test // 0x02 & 0x12
    public void test_LD_dd_A() {
        registers.setBC(0x1234);
        registers.reg[_A] = 0x7a;
        execute(0x02); // LD (BC),A
        assertEquals(0x7a, memory.get8bit(0x1234));

        registers.setDE(0x4321);
        registers.reg[_A] = 0xa7;
        execute(0x12); // LD (DE),A
        assertEquals(0xa7, memory.get8bit(0x4321));

        assertEquals(14, cpu.getTStates());
    }

    @Test // 0x03, 0x13, 0x23, 0x33
    public void test_INC_ss() {
        registers.setBC(0x0003);
        execute(0x03);
        assertEquals(0x0004, registers.getBC());

        registers.setDE(0x0013);
        execute(0x13);
        assertEquals(0x0014, registers.getDE());

        registers.setHL(0x0023);
        execute(0x23);
        assertEquals(0x0024, registers.getHL());

        registers.setSP(0x0033);
        execute(0x33);
        assertEquals(0x0034, registers.getSP());

        assertEquals(24, cpu.getTStates());
    }

    @Test // 0x04, 0x0c, 0x14, 0x1c, 0x24, 0x2c, 0x3c
    public void test_INC_r() {
        registers.reg[_B] = 0x04;
        execute(0x04);
        assertEquals(0x05, registers.reg[_B]);
        assertFlags(FALSE, FALSE, FALSE, FALSE, FALSE, null);

        registers.reg[_C] = 0x99;
        execute(0x0c);
        assertEquals(0x9a, registers.reg[_C]);
        assertFlags(TRUE, FALSE, FALSE, FALSE, FALSE, null);

        registers.reg[_D] = 0x7f;
        execute(0x14);
        assertEquals(0x80, registers.reg[_D]);
        assertFlags(TRUE, FALSE, TRUE, TRUE, FALSE, null);

        registers.reg[_E] = 0x1f;
        execute(0x1c);
        assertEquals(0x20, registers.reg[_E]);
        assertFlags(FALSE, FALSE, TRUE, FALSE, FALSE, null);

        registers.reg[_H] = 0xff;
        execute(0x24);
        assertEquals(0x00, registers.reg[_H]);
        assertFlags(FALSE, TRUE, TRUE, FALSE, FALSE, null);

        registers.reg[_L] = 0x00;
        execute(0x2c);
        assertEquals(0x01, registers.reg[_L]);
        assertFlags(FALSE, FALSE, FALSE, FALSE, FALSE, null);

        registers.reg[_A] = 0x3c;
        execute(0x3c);
        assertEquals(0x3d, registers.reg[_A]);
        assertFlags(FALSE, FALSE, FALSE, FALSE, FALSE, null);

        assertEquals(28, cpu.getTStates());
    }

    @Test // 0x05, 0x0d, 0x15, 0x1d, 0x25, 0x2d, 0x3d
    public void test_DEC_r() {
        registers.reg[_B] = 0x04;
        execute(0x05);
        assertEquals(0x03, registers.reg[_B]);
        assertFlags(FALSE, FALSE, FALSE, FALSE, TRUE, null);

        registers.reg[_C] = 0x99;
        execute(0x0d);
        assertEquals(0x98, registers.reg[_C]);
        assertFlags(TRUE, FALSE, FALSE, FALSE, TRUE, null);

        registers.reg[_D] = 0x80;
        execute(0x15);
        assertEquals(0x7f, registers.reg[_D]);
        assertFlags(FALSE, FALSE, TRUE, TRUE, TRUE, null);

        registers.reg[_E] = 0x20;
        execute(0x1d);
        assertEquals(0x1f, registers.reg[_E]);
        assertFlags(FALSE, FALSE, TRUE, FALSE, TRUE, null);

        registers.reg[_H] = 0x00;
        execute(0x25);
        assertEquals(0xff, registers.reg[_H]);
        assertFlags(TRUE, FALSE, TRUE, FALSE, TRUE, null);

        registers.reg[_L] = 0x01;
        execute(0x2d);
        assertEquals(0x00, registers.reg[_L]);
        assertFlags(FALSE, TRUE, FALSE, FALSE, TRUE, null);

        registers.reg[_A] = 0x3c;
        execute(0x3d);
        assertEquals(0x3b, registers.reg[_A]);
        assertFlags(FALSE, FALSE, FALSE, FALSE, TRUE, null);

        assertEquals(28, cpu.getTStates());
    }

    @Test // 0x06, 0x0e, 0x16, 0x1e, 0x26, 0x2e, 0x3e
    public void test_LD_r_N() {
        execute(0x06, 0x01);
        assertEquals(0x01, registers.reg[_B]);

        execute(0x0e, 0x98);
        assertEquals(0x98, registers.reg[_C]);

        execute(0x16, 0x7f);
        assertEquals(0x7f, registers.reg[_D]);

        execute(0x1e, 0x1f);
        assertEquals(0x1f, registers.reg[_E]);

        execute(0x26, 0xff);
        assertEquals(0xff, registers.reg[_H]);

        execute(0x2e, 0x00);
        assertEquals(0x00, registers.reg[_L]);

        execute(0x3e, 0x3b);
        assertEquals(0x3b, registers.reg[_A]);

        assertEquals(49, cpu.getTStates());
    }

    @Test // 0x07
    public void test_RLCA() {
        registers.reg[_A] = 0x88;
        execute(0x07);
        assertEquals(0x11, registers.reg[_A]);
        assertFlags(null, null, FALSE, null, FALSE, TRUE);

        execute(0x07);
        assertEquals(0x22, registers.reg[_A]);
        assertFlags(null, null, FALSE, null, FALSE, FALSE);

        assertEquals(8, cpu.getTStates());
    }

    @Test // 0x08
    public void test_EX_AF_AF() {
        registers.reg[_A] = 0x33;
        registers.reg[_F] = 0x44;
        registers.reg[_XAF] = 0x1234;

        execute(0x08);
        assertEquals(0x12, registers.reg[_A]);
        assertEquals(0x34, registers.reg[_F]);
        assertEquals(0x3344, registers.reg[_XAF]);

        execute(0x08);
        assertEquals(0x33, registers.reg[_A]);
        assertEquals(0x44, registers.reg[_F]);
        assertEquals(0x1234, registers.reg[_XAF]);

        assertEquals(8, cpu.getTStates());
    }

    @Test // 0x09 0x19 0x29 0x39
    public void test_ADD_HL_ss() {
        registers.setHL(0x100);
        registers.setBC(0x200);
        execute(0x09);
        assertEquals(0x300, registers.getHL());
        assertEquals(0x200, registers.getBC());
        assertFlags(null, null, FALSE, null, FALSE, FALSE);

        registers.setHL(0xfff);
        registers.setDE(0x001);
        execute(0x19);
        assertEquals(0x1000, registers.getHL());
        assertEquals(0x001, registers.getDE());
        assertFlags(null, null, TRUE, null, FALSE, FALSE);

        registers.setHL(0x8000);
        execute(0x29);
        assertEquals(0x000, registers.getHL());
        assertFlags(null, null, FALSE, null, FALSE, TRUE);

        registers.setHL(0x001);
        registers.setSP(0x001);
        execute(0x19);
        assertEquals(0x002, registers.getHL());
        assertFlags(null, null, FALSE, null, FALSE, FALSE);

        assertEquals(44, cpu.getTStates());
    }

    @Test  // 0x0a 0x1a
    public void test_LD_A_ss() {
        registers.setBC(0x100);
        registers.setDE(0x200);
        memory.set8bit(0x100, 0x12);
        memory.set8bit(0x200, 0x56);

        execute(0x0a);
        assertEquals(0x12, registers.reg[_A]);

        execute(0x1a);
        assertEquals(0x56, registers.reg[_A]);

        assertEquals(14, cpu.getTStates());
    }

    @Test // 0x0b, 0x1b, 0x2b, 0x3b
    public void test_DEC_ss() {
        registers.setBC(0x0003);
        execute(0x0b);
        assertEquals(0x0002, registers.getBC());

        registers.setDE(0x0013);
        execute(0x1b);
        assertEquals(0x0012, registers.getDE());

        registers.setHL(0x0023);
        execute(0x2b);
        assertEquals(0x0022, registers.getHL());

        registers.setSP(0x0033);
        execute(0x3b);
        assertEquals(0x0032, registers.getSP());

        assertEquals(24, cpu.getTStates());
    }

    @Test // 0x0f
    public void test_RRCA() {
        registers.reg[_A] = 0x11;
        execute(0x0f);
        assertEquals(0x88, registers.reg[_A]);
        assertFlags(null, null, FALSE, null, FALSE, TRUE);

        execute(0x0f);
        assertEquals(0x44, registers.reg[_A]);
        assertFlags(null, null, FALSE, null, FALSE, FALSE);

        assertEquals(8, cpu.getTStates());
    }

    @Test // 0x10
    public void test_DJNZ_dis() {
        registers.setPC(0x1000);
        registers.reg[_B] = 0x01;
        executeOne(0x10, 0x80);
        assertEquals(0x1002, registers.getPC());
        assertEquals(0x00, registers.reg[_B]);
        assertEquals(8, cpu.getTStates());

        executeOne(0x10, 0x10);
        assertEquals(0xff, registers.reg[_B]);
        assertEquals(0x1014, registers.getPC());
        assertEquals(21, cpu.getTStates());

        executeOne(0x10, 0xfc);
        assertEquals(0xfe, registers.reg[_B]);
        assertEquals(0x1012, registers.getPC());

        assertEquals(34, cpu.getTStates());
    }

    @Test // 0x17
    public void test_RLA() {
        registers.reg[_A] = 0x76;
        registers.setFlag(F_C);
        execute(0x17);
        assertEquals(0xed, registers.reg[_A]);
        assertFlags(null, null, FALSE, null, FALSE, FALSE);

        execute(0x17);
        assertEquals(0xda, registers.reg[_A]);
        assertFlags(null, null, FALSE, null, FALSE, TRUE);

        assertEquals(8, cpu.getTStates());
    }

    @Test // 0x18
    public void test_JR_dis() {
        registers.setPC(0x1000);
        executeOne(0x18, 0x05);
        assertEquals(0x1007, registers.getPC());

        executeOne(0x18, 0xfc);
        assertEquals(0x1005, registers.getPC());

        assertEquals(24, cpu.getTStates());
    }

    @Test // 0x1f
    public void test_RRA() {
        registers.reg[_A] = 0xe1;
        registers.clearFlag(F_C);
        execute(0x1f);
        assertEquals(0x70, registers.reg[_A]);
        assertFlags(null, null, FALSE, null, FALSE, TRUE);

        execute(0x1f);
        assertEquals(0xb8, registers.reg[_A]);
        assertFlags(null, null, FALSE, null, FALSE, FALSE);

        assertEquals(8, cpu.getTStates());
    }

    @Test // 0x20
    public void test_JR_NZ_dis() {
        registers.setPC(0x1000);
        registers.setFlag(F_Z);
        executeOne(0x20, 0x05);
        assertEquals(0x1002, registers.getPC());

        registers.clearFlag(F_Z);
        executeOne(0x20, 0x05);
        assertEquals(0x1009, registers.getPC());

        registers.clearFlag(F_Z);
        executeOne(0x20, 0xfc);
        assertEquals(0x1007, registers.getPC());

        assertEquals(31, cpu.getTStates());
    }

    @Test // 0x22
    public void test_LD_NN_HL() {
        registers.setHL(0x483a);
        executeOne(0x22, 0x00, 0x10);
        assertEquals(0x483a, memory.get16bit(0x1000));
        assertEquals(0x3a, memory.get8bit(0x1000));
        assertEquals(0x48, memory.get8bit(0x1001));

        assertEquals(16, cpu.getTStates());
    }

    @Test
    public void test_DAA() {
        registers.reg[_A] = 0x15;
        executeOne(0xc6, 0x27);
        assertEquals(0x3c, registers.reg[_A]);
        assertFlags(null, null, FALSE, null, null, FALSE);

        executeOne(0x27); // DAA
        assertEquals(0x42, registers.reg[_A]);
        assertFlags(null, null, FALSE, null, null, FALSE);

        assertEquals(4, cpu.getTStates());
    }

    @Test // 0x28
    public void test_JR_Z_dis() {
        registers.setPC(0x1000);
        registers.clearFlag(F_Z);
        executeOne(0x28, 0x05);
        assertEquals(0x1002, registers.getPC());

        registers.setFlag(F_Z);
        executeOne(0x28, 0x05);
        assertEquals(0x1009, registers.getPC());

        registers.setFlag(F_Z);
        executeOne(0x28, 0xfc);
        assertEquals(0x1007, registers.getPC());

        assertEquals(31, cpu.getTStates());
    }

    private void assertFlags(Boolean s, Boolean z, Boolean h, Boolean pv, Boolean n, Boolean c) {
        Optional.ofNullable(s).ifPresent(f -> assertEquals(f, registers.isFlag(F_S)));
        Optional.ofNullable(z).ifPresent(f -> assertEquals(f, registers.isFlag(F_Z)));
        Optional.ofNullable(h).ifPresent(f -> assertEquals(f, registers.isFlag(F_H)));
        Optional.ofNullable(pv).ifPresent(f -> assertEquals(f, registers.isFlag(F_PV)));
        Optional.ofNullable(n).ifPresent(f -> assertEquals(f, registers.isFlag(F_N)));
        Optional.ofNullable(c).ifPresent(f -> assertEquals(f, registers.isFlag(F_C)));
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

    @Test
    public void testLoadIX_NN() {
        cpu.getMemory().set16bit(0xabcd, 0x1234);
        execute(0xdd, 0x2a, 0xcd, 0xab);
        assertEquals(0x1234, cpu.getRegisters().reg[_IX]);

        execute(0xfd, 0x2a, 0xcd, 0xab);
        assertEquals(0x1234, cpu.getRegisters().reg[_IY]);
    }

    private void execute(int... val) {
        for (int i = 0; i < val.length; i++) {
            cpu.getMemory().set8bit(cpu.getRegisters().reg[_PC] + i, val[i]);
        }
        int executed = 0;
        while (executed < val.length) {
            int before = cpu.getRegisters().reg[_PC];
            cpu.execute();
            executed += cpu.getRegisters().reg[_PC] - before;
        }
    }

    private void executeOne(int... val) {
        for (int i = 0; i < val.length; i++) {
            cpu.getMemory().set8bit(cpu.getRegisters().reg[_PC] + i, val[i]);
        }
        cpu.execute();
    }
}
