package z80;

import java.io.IOException;
import java.io.InputStream;

import static z80.Registers.*;

public class Z80Snapshot {

    private int[] image = new int[64 * 1024];

    public Z80Snapshot() throws IOException {
        InputStream is = ClassLoader.getSystemResourceAsStream("JSW.z80");

        int i;
        int idx = 0;
        while((i = is.read()) != -1) {
            image[idx++] = i;
        }
        is.close();
    }

    public void loadIntoCpu(Cpu cpu) {
        final boolean compressed = (image[12] & 0x20) != 0;

        cpu.getRegisters().reg[_A] = (image[0] & 0xff);
        cpu.getRegisters().reg[_F] = (image[1] & 0xff);
        cpu.getRegisters().reg[_C] = (image[2] & 0xff);
        cpu.getRegisters().reg[_B] = (image[3] & 0xff);
        cpu.getRegisters().reg[_L] = (image[4] & 0xff);
        cpu.getRegisters().reg[_H] = (image[5] & 0xff);

        System.out.println("PC=" + image[6] + " " + image[7] + compressed);
        System.out.println(String.format("flags=%02x", image[12]));
        cpu.getRegisters().setPC(word(6));
        cpu.getRegisters().setSP(word(8));

        cpu.getRegisters().reg[_I] = (image[10] & 0xff);
        cpu.getRegisters().reg[_R] = (image[11] & 0xff);

        // TODO 12
        cpu.getRegisters().reg[_R] = (cpu.getRegisters().reg[_R] & 0x7f) | ((image[12] & 0x01) << 7);

        cpu.getRegisters().reg[_E] = (image[13] & 0xff);
        cpu.getRegisters().reg[_D] = (image[14] & 0xff);

        cpu.getRegisters().reg[_XBC] = word(15);
        cpu.getRegisters().reg[_XDE] = word(17);
        cpu.getRegisters().reg[_XHL] = word(19);
        cpu.getRegisters().reg[_XAF] = word(21);
        cpu.getRegisters().reg[_IY] = word(23);
        cpu.getRegisters().reg[_IX] = word(25);

        cpu.getRegisters().iff1 = image[27] != 0;
        cpu.getRegisters().iff2 = image[28] != 0;
        switch (image[29] & 0x03) {
            case 0x00:
                cpu.getRegisters().im = IntMode.IM0;
                break;
            case 0x01:
                cpu.getRegisters().im = IntMode.IM1;
                break;
            case 0x02:
                cpu.getRegisters().im = IntMode.IM2;
                break;
        }

        int loaded = 0;
        int adr = 30;
        int mem = 16 * 1024;
        while (loaded < 48 * 1024) {
            if(image[adr] == 0xed && image[adr + 1] == 0xed) {
                for(int i = 0; i < image[adr + 2]; i++) {
                    cpu.getMemory().set8bit(mem++, image[adr + 3]);
                    loaded++;
                }
                adr += 4;
            } else {
                cpu.getMemory().set8bit(mem++, image[adr++]);
                loaded++;
            }
        }
        System.out.println("Loaded");
    }

    private int word(int idx) {
        return (image[idx] | (image[idx + 1] << 8)) & 0xffff;
    }
}
