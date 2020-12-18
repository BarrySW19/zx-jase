package z80;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import static z80.Registers.*;

public class Z80Snapshot {
    private static final Logger log = LoggerFactory.getLogger(Z80Snapshot.class);

    private int[] image;

    public Z80Snapshot() throws IOException {
        InputStream is = ClassLoader.getSystemResourceAsStream("Dam.z80");
        int[] data = new int[256 * 1024];

        int i;
        int idx = 0;
        while((i = is.read()) != -1) {
            data[idx++] = i;
        }
        is.close();

        image = new int[idx];
        System.arraycopy(data, 0, image, 0, idx);
        log.info("Loaded: {} bytes", image.length);
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

        if(cpu.getRegisters().getPC() == 0) {
            log.info("Additional block = {}", word(30));
            int nextBlock = 32 + word(30);
            while (nextBlock < image.length) {
                log.info("Block length = {}", word(nextBlock));
                int pageType = image[nextBlock + 2];
                log.info("Page no = {}", pageType);
                int location;
                switch (pageType) {
                    case 4:
                        location = 0x8000;
                        break;
                    case 5:
                        location = 0xc000;
                        break;
                    case 8:
                        location = 0x4000;
                        break;
                    default:
                        throw new RuntimeException("Unsupported page type: " + pageType);
                }
                readBlockFrom(cpu, nextBlock + 3, 16384, location);
                nextBlock = nextBlock + 3 + word(nextBlock);
            }
            cpu.getRegisters().setPC(word(32));
        } else {
            readBlockFrom(cpu, 30, 48 * 1024, 16 * 1024);
        }
        System.out.println("Loaded");
    }

    private void readBlockFrom(Cpu cpu, int start, int size, int location) {
        int loaded = 0;
        int adr = start;
        int mem = location;
        while (loaded < size) {
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
    }

    private int word(int idx) {
        return (image[idx] | (image[idx + 1] << 8)) & 0xffff;
    }
}
