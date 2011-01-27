package z80;

import static z80.Registers.F_C;
import static z80.Registers._A;
import static z80.Registers._PC;
import static z80.Registers.*;

import java.util.ArrayList;
import java.util.List;

public class Cpu {

	private Memory memory;
	private Registers registers;
	private long tStates = 0;

	private Handler[] baseHandlers = new Handler[256];

	public Cpu() {
		Handler nullHandler = new NullHandler();

		List<Handler> handlers = new ArrayList<Handler>();
		handlers.add(new Handler_LD());
		handlers.add(new Handler_ADD());
		handlers.add(new Handler_SUB());
		handlers.add(new Handler_NOP());
		handlers.add(new Handler_INC());
		handlers.add(new Handler_DEC());
		handlers.add(new Handler_PUSH());
		handlers.add(new Handler_POP());
		handlers.add(new Handler_SCF_CCF());
		handlers.add(new Handler_LD_N());

		for (int i = 0; i < 256; i++) {
			for (Handler handler : handlers) {
				if (handler.willHandle(i)) {
					if (baseHandlers[i] != null) {
						System.out.println("Warning: duplicate handler: " + i);
					}
					baseHandlers[i] = handler;
				}
			}
			if (baseHandlers[i] == null) {
				baseHandlers[i] = nullHandler;
			}
		}
	}

	public long getTStates() {
		return tStates;
	}

	public Memory getMemory() {
		return memory;
	}

	public void setMemory(Memory memory) {
		this.memory = memory;
	}

	public Registers getRegisters() {
		return registers;
	}

	public void setRegisters(Registers registers) {
		this.registers = registers;
	}

	private int readNextByte() {
		int val = memory.get8bit(registers.reg[_PC]);
		registers.reg[_PC] = (registers.reg[_PC] + 1) & 0xffff;
		return val;
	}

	public void execute() {
		Handler[] current = baseHandlers;
		int instr = readNextByte();
		current[instr].handle(instr);
	}

	interface Handler {
		boolean willHandle(int instr);

		void handle(int instr);
	}

	class NullHandler implements Handler {
		public void handle(int instr) {
			System.out.println("Unhandled: 0x" + Integer.toString(instr, 16));
		}

		public boolean willHandle(int instr) {
			return false;
		}
	}

	class Handler_SCF_CCF implements Handler {
		public void handle(int instr) {
			int flags = registers.reg[_F];
			switch (instr) {
			case 0x37:  // SCF
				flags = adjustFlag(flags, F_H, false);
				flags = adjustFlag(flags, F_C, true);
				break;
			case 0x3f:  // CCF
				flags = adjustFlag(flags, F_H, (flags & (1<<F_C)) != 0);
				flags = adjustFlag(flags, F_C, (flags & (1<<F_C)) == 0);
				break;
			}
			flags = adjustFlag(flags, F_N, false);
			registers.reg[_F] = flags;
			tStates += 4;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xf7) == 0x37;
		}
	}

	class Handler_POP implements Handler {

		public boolean willHandle(int instr) {
			return (instr & 0xCF) == 0xC1;
		}

		public void handle(int instr) {
			int val = memory.get16bit(registers.reg[_SP]);
			switch ((instr & 0x30) >> 4) {
			case 0:
				registers.setBC(val);
				break;
			case 1:
				registers.setDE(val);
				break;
			case 2:
				registers.setHL(val);
				break;
			case 3:
				registers.setAF(val);
				break;
			}
			registers.reg[_SP] = (registers.reg[_SP] + 2) & 0xffff;
			tStates += 10;
		}
	}

	class Handler_PUSH implements Handler {

		public boolean willHandle(int instr) {
			return (instr & 0xCF) == 0xC5;
		}

		public void handle(int instr) {
			registers.reg[_SP] = (registers.reg[_SP] - 2) & 0xffff;
			int val = 0;
			switch ((instr & 0x30) >> 4) {
			case 0:
				val = registers.getBC();
				break;
			case 1:
				val = registers.getDE();
				break;
			case 2:
				val = registers.getHL();
				break;
			case 3:
				val = registers.getAF();
				break;
			}
			memory.set16bit(registers.reg[_SP], val);
			tStates += 11;
		}
	}

	class Handler_NOP implements Handler {
		public void handle(int instr) {
			tStates += 4;
		}

		public boolean willHandle(int instr) {
			return instr == 0;
		}
	}

	class Handler_LD implements Handler {
		public void handle(int instr) {
			int src = getRegisterValue(instr);

			int dest = (instr & 0x38) >> 3;
			if (dest == 0x06) {
				memory.set8bit(registers.getHL(), src);
				tStates += 3;
			} else {
				registers.reg[dest] = src;
			}
			tStates += 4;
		}

		public boolean willHandle(int instr) {
			return ((instr & 0xc0) == 0x40) && instr != 0x76;
		}
	}

	class Handler_ADD implements Handler {
		public void handle(int instr) {
			int src = getRegisterValue(instr);

			if ((instr & 0x08) == 0x08) {
				src += registers.getFlag(F_C);
			}

			int before = registers.reg[_A];
			int after = registers.reg[_A] + src;
			registers.reg[_A] = (after & 0xff);

			int flags = registers.reg[_F];
			flags = adjustFlag(flags, F_S, (registers.reg[_A] & 0x80) == 0x80);
			flags = adjustFlag(flags, F_Z, registers.reg[_A] == 0);
			flags = adjustFlag(flags, F_H,
					((before & 0x0f) + (src & 0x0f)) > 0x0f);
			flags = adjustFlag(flags, F_PV, (before & 0x80) == (src & 0x80)
					&& (before & 0x80) != (after & 0x80));
			flags = adjustFlag(flags, F_N, false);
			flags = adjustFlag(flags, F_C, (after & ~0xff) != 0);
			registers.reg[_F] = flags;

			tStates += 4;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xf0) == 0x80;
		}
	}

	class Handler_SUB implements Handler {
		public void handle(int instr) {
			int src = getRegisterValue(instr);

			if ((instr & 0x08) == 0x08) {
				src += registers.getFlag(F_C);
			}

			int before = registers.reg[_A];
			int after = registers.reg[_A] - src;
			registers.reg[_A] = (after & 0xff);

			int flags = registers.reg[_F];
			flags = adjustFlag(flags, F_S, (registers.reg[_A] & 0x80) == 0x80);
			flags = adjustFlag(flags, F_Z, registers.reg[_A] == 0);
			flags = adjustFlag(flags, F_H,
					((before & 0x0f) - (src & 0x0f)) < 0);
			flags = adjustFlag(flags, F_PV, (before & 0x80) != (src & 0x80)
					&& (before & 0x80) != (after & 0x80));
			flags = adjustFlag(flags, F_N, true);
			flags = adjustFlag(flags, F_C, (after & ~0xff) != 0);
			registers.reg[_F] = flags;
			
			tStates += 4;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xf0) == 0x90;
		}
	}

	class Handler_LD_N implements Handler {
		public void handle(int instr) {
			int val = readNextByte();
			int reg = (instr & 0x38) >> 3;
			if (reg == 6) {
				memory.set8bit(registers.getHL(), val);
				tStates += 10;
			} else {
				registers.reg[reg] = val;
				tStates += 7;
			}
		}

		public boolean willHandle(int instr) {
			return ((instr & 0xC7) == 0x06);
		}
	}

	class Handler_INC implements Handler {
		public void handle(int instr) {
			int reg = (instr & 0x38) >> 3;
			int res;
			if (reg == 6) {
				memory.set8bit(registers.getHL(),
						memory.get8bit(registers.getHL()) + 1);
				res = memory.get8bit(registers.getHL());
				tStates += 11;
			} else {
				registers.reg[reg]++;
				res = registers.reg[reg];
				tStates += 4;
			}

			int flags = registers.reg[_F];
			flags = adjustFlag(flags, F_S, (res & 0x80) == 0x80);
			flags = adjustFlag(flags, F_Z, res == 0);
			flags = adjustFlag(flags, F_H, (res & 0x0f) == 0);
			flags = adjustFlag(flags, F_PV, res == 0x80);
			flags = adjustFlag(flags, F_N, false);
			registers.reg[_F] = flags;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xc7) == 0x04;
		}
	}

	class Handler_DEC implements Handler {
		public void handle(int instr) {
			int reg = (instr & 0x38) >> 3;
			if (reg == 6) {
				memory.set8bit(registers.getHL(),
						memory.get8bit(registers.getHL()) - 1);
				tStates += 11;
			} else {
				registers.reg[reg]--;
				tStates += 4;
			}
		}

		public boolean willHandle(int instr) {
			return (instr & 0xc7) == 0x05;
		}
	}

	private int getRegisterValue(int instr) {
		int src = instr & 0x07;
		if (src == 0x06) {
			tStates += 3;
			return memory.get8bit(registers.getHL());
		} else {
			return registers.reg[src];
		}
	}

	private int adjustFlag(int flags, int bit, boolean val) {
		if (val) {
			return flags | (1 << bit);
		} else {
			return flags &= ~(1 << bit);
		}
	}
}
