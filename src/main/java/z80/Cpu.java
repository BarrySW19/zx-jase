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
	private Handler[] extended_CB = new Handler[256];
	private Handler[] extended_DD = new Handler[256];
	private Handler[] extended_ED = new Handler[256];
	private Handler[] extended_FD = new Handler[256];
	private Handler[] current = baseHandlers;
	private OutputDevice[] outputs = new OutputDevice[256];
	
	private boolean enableInt = false;

	public Cpu() {
		LoadableHandler nullHandler = new NullHandler();

		List<LoadableHandler> handlers = new ArrayList<LoadableHandler>();
		handlers.add(new Handler_ADD());
		handlers.add(new Handler_ADD_HL());
		handlers.add(new Handler_AND());
		handlers.add(new Handler_CP());
		handlers.add(new Handler_DEC());
		handlers.add(new Handler_EX_DE_HL());
		handlers.add(new Handler_INC());
		handlers.add(new Handler_INC_DEC_RR());
		handlers.add(new Handler_JP());
		handlers.add(new Handler_JR());
		handlers.add(new Handler_LD());
		handlers.add(new Handler_LD_N());
		handlers.add(new Handler_LD_RR());
		handlers.add(new Handler_LD_HL_NN());
		handlers.add(new Handler_OUT());
		handlers.add(new Handler_POP());
		handlers.add(new Handler_PUSH());
		handlers.add(new Handler_RST());
		handlers.add(new Handler_SCF_CCF());
		handlers.add(new Handler_SUB());
		handlers.add(new Handler_XOR());

		handlers.add(new ShiftHandler());

		loadSimpleHandlers();
		
		for (int i = 0; i < 256; i++) {
			for (LoadableHandler handler : handlers) {
				if (handler.willHandle(i)) {
					if (baseHandlers[i] != null) {
						System.out.println("Warning: duplicate handler: " + i);
					}
					baseHandlers[i] = handler;
				}
			}
		}
		
		handlers.clear();
		handlers.add(new Handler_ED_LD_I_A());
		handlers.add(new Handler_SBC_HL());
		handlers.add(new Handler_LD_NN_RR());
		handlers.add(new Handler_LDDR());
				
		for (int i = 0; i < 256; i++) {
			for (LoadableHandler handler : handlers) {
				if (handler.willHandle(i)) {
					if (extended_ED[i] != null) {
						System.out.println("Warning: duplicate ED handler: " + i);
					}
					extended_ED[i] = handler;
				}
			}
		}
		
		for(int i = 0; i < 256; i++) {
			baseHandlers[i] = (baseHandlers[i] == null ? nullHandler : baseHandlers[i]);
			extended_CB[i] = (extended_CB[i] == null ? nullHandler : extended_CB[i]);
			extended_DD[i] = (extended_DD[i] == null ? nullHandler : extended_DD[i]);
			extended_ED[i] = (extended_ED[i] == null ? nullHandler : extended_ED[i]);
			extended_FD[i] = (extended_FD[i] == null ? nullHandler : extended_FD[i]);
		}
	}
	
	private void loadSimpleHandlers() {
		// EXX
		baseHandlers[0xD9] = new Handler() {
			public void handle(int instr) {
				registers.exx();
				tStates += 4;
			}
		};
		// NOP
		baseHandlers[0x00] = new Handler() {
			public void handle(int instr) {
				tStates += 4;
			}
		};
		// LD (nn),HL
		baseHandlers[0x22] = new Handler() {
			public void handle(int instr) {
				memory.set16bit(readNextWord(), registers.getHL());
				tStates += 16;
			}
		};
		// DI
		baseHandlers[0xf3] = new Handler() {
			public void handle(int instr) {
				registers.iff1 = registers.iff2 = false;
				tStates += 4;
			}
		};
		// EI
		baseHandlers[0xfb] = new Handler() {
			public void handle(int instr) {
				enableInt = true;
				tStates += 4;
			}
		};
		// LD SP,HL
		baseHandlers[0xf9] = new Handler() {
			public void handle(int instr) {
				registers.reg[_SP] = registers.getHL();
				tStates += 6;
			}
		};
		
		// IM 1
		extended_ED[0x56] = new Handler() {
			public void handle(int instr) {
				registers.im = IntMode.IM1;
				tStates += 8;
			}
		};

		// LD IY,NN
		extended_FD[0x21] = new Handler() {
			public void handle(int instr) {
				registers.reg[_IY] = readNextWord();
				tStates += 14;
			}
		};
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

	private int readNextWord() {
		int val = memory.get16bit(registers.reg[_PC]);
		registers.reg[_PC] = (registers.reg[_PC] + 2) & 0xffff;
		return val;
	}

	public void execute() {
		int instr = readNextByte();
		System.out.println("i " + Integer.toHexString(instr));
		current[instr].handle(instr);
		if(enableInt) {
			enableInt = false;
			registers.iff1 = registers.iff2 = true;
		}
	}

	class NullHandler implements LoadableHandler {
		public void handle(int instr) {
			System.out.println("Unhandled: 0x" + Integer.toString(instr, 16));
			throw new RuntimeException("Unfinished CPU at " + tStates);
		}

		public boolean willHandle(int instr) {
			return false;
		}
	}

	class Handler_LD_HL_NN implements LoadableHandler {
		public void handle(int instr) {
			registers.setHL(memory.get16bit(readNextWord()));
			tStates += 16;
		}

		public boolean willHandle(int instr) {
			return instr == 0x2a;
		}
	}

	class Handler_SCF_CCF implements LoadableHandler {
		public void handle(int instr) {
			int flags = registers.reg[_F];
			switch (instr) {
			case 0x37: // SCF
				flags = adjustFlag(flags, F_H, false);
				flags = adjustFlag(flags, F_C, true);
				break;
			case 0x3f: // CCF
				flags = adjustFlag(flags, F_H, (flags & (1 << F_C)) != 0);
				flags = adjustFlag(flags, F_C, (flags & (1 << F_C)) == 0);
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

	class Handler_POP implements LoadableHandler {

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

	class Handler_PUSH implements LoadableHandler {

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

	class Handler_LD implements LoadableHandler {
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

	class Handler_ADD_HL implements LoadableHandler {
		public void handle(int instr) {
			int preHL = registers.getHL();
			int arg = get16bitRegister((instr & 0x30) >> 4);
			registers.setHL((preHL + arg) & 0xffff);
			int res = registers.getHL();
			
			int flags = registers.reg[_F];
			flags = adjustFlag(flags, F_H, ((preHL & 0x0fff) + (arg & 0x0fff)) > 0x0fff);
			flags = adjustFlag(flags, F_N, false);
			flags = adjustFlag(flags, F_C, (res & ~0xffff) != 0);
			registers.reg[_F] = flags;

			tStates += 11;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xCF) == 0x09;
		}
	}

	class Handler_ADD implements LoadableHandler {
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

	class Handler_SUB implements LoadableHandler {
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
			flags = adjustFlag(flags, F_H, ((before & 0x0f) - (src & 0x0f)) < 0);
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

	class Handler_LD_N implements LoadableHandler {
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

	class Handler_INC implements LoadableHandler {
		public void handle(int instr) {
			int reg = (instr & 0x38) >> 3;
			int res;
			if (reg == 6) {
				memory.set8bit(registers.getHL(),
						memory.get8bit(registers.getHL()) + 1);
				res = memory.get8bit(registers.getHL());
				tStates += 11;
			} else {
				registers.reg[reg] = (registers.reg[reg] + 1) & 0xff;
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

	class Handler_DEC implements LoadableHandler {
		public void handle(int instr) {
			int reg = (instr & 0x38) >> 3;
			int res;
			if (reg == 6) {
				memory.set8bit(registers.getHL(),
						memory.get8bit(registers.getHL()) - 1);
				res = memory.get8bit(registers.getHL());
				tStates += 11;
			} else {
				registers.reg[reg] = (registers.reg[reg] - 1) & 0xff;
				res = registers.reg[reg];
				tStates += 4;
			}

			int flags = registers.reg[_F];
			flags = adjustFlag(flags, F_S, (res & 0x80) == 0x80);
			flags = adjustFlag(flags, F_Z, res == 0);
			flags = adjustFlag(flags, F_H, (res & 0x0f) == 0x0f);
			flags = adjustFlag(flags, F_PV, res == 0x7f);
			flags = adjustFlag(flags, F_N, true);
			registers.reg[_F] = flags;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xc7) == 0x05;
		}
	}

	class Handler_XOR implements LoadableHandler {
		public void handle(int instr) {
			int arg;
			if (instr == 0xEE) {
				arg = readNextByte();
				tStates += 3;
			} else {
				arg = getRegisterValue(instr & 0x07);
			}

			registers.reg[_A] = registers.reg[_A] ^ arg;
			int res = registers.reg[_A];

			int flags = registers.reg[_F];
			flags = adjustFlag(flags, F_S, (res & 0x80) == 0x80);
			flags = adjustFlag(flags, F_Z, res == 0);
			flags = adjustFlag(flags, F_H, false);
			flags = adjustFlag(flags, F_PV, Integer.bitCount(res) % 2 == 0);
			flags = adjustFlag(flags, F_N, false);
			flags = adjustFlag(flags, F_C, false);
			registers.reg[_F] = flags;

			tStates += 4;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xf8) == 0xa8 || instr == 0xEE;
		}
	}

	class Handler_AND implements LoadableHandler {
		public void handle(int instr) {
			int arg;
			if (instr == 0xE6) {
				arg = readNextByte();
				tStates += 3;
			} else {
				arg = getRegisterValue(instr & 0x07);
			}

			registers.reg[_A] = registers.reg[_A] & arg;
			int res = registers.reg[_A];

			int flags = registers.reg[_F];
			flags = adjustFlag(flags, F_S, (res & 0x80) == 0x80);
			flags = adjustFlag(flags, F_Z, res == 0);
			flags = adjustFlag(flags, F_H, true);
//			flags = adjustFlag(flags, F_PV, Integer.bitCount(res) % 2 == 0);
			flags = adjustFlag(flags, F_N, false);
			flags = adjustFlag(flags, F_C, false);
			registers.reg[_F] = flags;

			tStates += 4;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xf8) == 0xa0 || instr == 0xE6;
		}
	}

	class Handler_CP implements LoadableHandler {
		public void handle(int instr) {
			int arg;
			if (instr == 0xFE) {
				arg = readNextByte();
				tStates += 3;
			} else {
				arg = getRegisterValue(instr & 0x07);
			}

			int res = registers.reg[_A];

			int flags = registers.reg[_F];
			flags = adjustFlag(flags, F_S, ((res-arg) & 0x80) == 0x80);
			flags = adjustFlag(flags, F_Z, res == arg);
//	 TODO		flags = adjustFlag(flags, F_H, false);
			flags = adjustFlag(flags, F_PV, (res < arg) ? ! ((res & 0x80) == 0x80) : (res & 0x80) == 0x80);
			flags = adjustFlag(flags, F_N, true);
			flags = adjustFlag(flags, F_C, res < arg);
			registers.reg[_F] = flags;

			tStates += 4;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xf8) == 0xb8 || instr == 0xFE;
		}
	}

	class Handler_RST implements LoadableHandler {
		public void handle(int instr) {
			registers.reg[_SP] = (registers.reg[_SP] - 2) & 0xffff;
			memory.set16bit(registers.reg[_SP], registers.reg[_PC]);
			registers.reg[_PC] = (instr & 0x38);
		}

		public boolean willHandle(int instr) {
			return (instr & 0xC7) == 0xC7;
		}
	}

	class Handler_LD_RR implements LoadableHandler {
		public void handle(int instr) {
			int val = readNextWord();
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
				registers.reg[_SP] = val;
				break;
			}
			tStates += 10;
		}

		public boolean willHandle(int instr) {
			return ((instr & 0xCF) == 0x01);
		}
	}

	class Handler_JP implements LoadableHandler {
		public void handle(int instr) {
			registers.reg[_PC] = readNextWord();
			tStates += 10;
		}

		public boolean willHandle(int instr) {
			return instr == 0xc3;
		}
	}

	class Handler_JR implements LoadableHandler {
		public void handle(int instr) {
			boolean test = false;
			switch(instr) {
			case 0x20:
				test = ! registers.isFlag(F_Z);
				break;
			case 0x28:
				test = registers.isFlag(F_Z);
				break;
			case 0x30:
				test = ! registers.isFlag(F_C);
				break;
			case 0x38:
				test = registers.isFlag(F_C);
				break;
			}
			
			int dist = readNextByte();

			if(test) {
				if(dist >= 128) {
					dist = -(256 - dist);
					System.out.println("" + registers.reg[_A] + " " + registers.getHL());
				}
				registers.reg[_PC] = (registers.reg[_PC] + dist) & 0xffff;
				tStates += 5;
			}
			tStates += 7;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xE7) == 0x20;
		}
	}

	class Handler_INC_DEC_RR implements LoadableHandler {
		public void handle(int instr) {
			int adj = (instr & 0x08) == 0 ? 1 : -1;
			switch((instr & 0x30) >> 4) {
			case 0:
				registers.setBC(registers.getBC() + adj);
				break;
			case 1:
				registers.setDE(registers.getDE() + adj);
				break;
			case 2:
				registers.setHL(registers.getHL() + adj);
				break;
			case 3:
				registers.reg[_SP] = (registers.reg[_SP] + adj) & 0xff;
				break;
			}
			
			// 16 bit inc/dec does not affect flags
			tStates += 6;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xc7) == 0x03;
		}
	}

	class Handler_OUT implements LoadableHandler {
		public void handle(int instr) {
			int addr = readNextByte();
			if(outputs[addr] != null) {
				outputs[addr].event(registers.reg[_A]);
			}
			tStates += 11;
		}

		public boolean willHandle(int instr) {
			return instr == 0xd3;
		}
	}

	class ShiftHandler implements LoadableHandler {
		public void handle(int instr) {
			switch(instr) {
			case 0xCB:
				current = extended_CB;
				break;
			case 0xDD:
				current = extended_DD;
				break;
			case 0xED:
				current = extended_ED;
				break;
			case 0xFD:
				current = extended_FD;
				break;
			}
			execute();
			current = baseHandlers;
		}

		public boolean willHandle(int instr) {
			return instr == 0xED || instr == 0xFD || instr == 0xDD || instr == 0xCB;
		}
	}
	
	class Handler_EX_DE_HL implements LoadableHandler {
		public void handle(int instr) {
			int val = registers.getHL();
			registers.setHL(registers.getDE());
			registers.setDE(val);
			tStates += 4;
		}

		public boolean willHandle(int instr) {
			return instr == 0xEB;
		}
	}

	// ================ 0xED.. =====================
	
	class Handler_ED_LD_I_A implements LoadableHandler {
		public void handle(int instr) {
			registers.reg[_I] = registers.reg[_A];
			tStates += 9;
		}

		public boolean willHandle(int instr) {
			return instr == 0x47;
		}
	}

	class Handler_LD_NN_RR implements LoadableHandler {
		public void handle(int instr) {
			int arg = readNextWord();
			int val = get16bitRegister((instr & 0x30) >> 4);
			memory.set16bit(arg, val);
			tStates += 20;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xCF) == 0x43 && instr != 0x63;
		}
	}

	class Handler_LDDR implements LoadableHandler {
		public void handle(int instr) {
			while(true) {
				memory.set8bit(registers.getDE(), memory.get8bit(registers.getHL()));
				registers.setHL(registers.getHL() - 1);
				registers.setDE(registers.getDE() - 1);
				registers.setBC(registers.getBC() - 1);
				if(registers.getBC() == 0) {
					break;
				}
				tStates += 21;
			}
			tStates += 16;
		}

		public boolean willHandle(int instr) {
			return instr == 0xB8;
		}
	}

	class Handler_SBC_HL implements LoadableHandler {
		public void handle(int instr) {
			int preHL = registers.getHL();
			int arg = get16bitRegister((instr & 0x30) >> 4);
			arg += registers.getFlag(F_C);
			registers.setHL((preHL - arg) & 0xffff);
			int res = registers.getHL();
			
			int flags = registers.reg[_F];
			flags = adjustFlag(flags, F_S, (res & 0x80) == 0x80);
			flags = adjustFlag(flags, F_Z, res == 0);
			flags = adjustFlag(flags, F_H, ((preHL & 0x0fff) < (arg & 0x0fff)));
			flags = adjustFlag(flags, F_PV, (preHL & 0x80) != (arg & 0x8000)
					&& (preHL & 0x8000) != (res & 0x8000));
			flags = adjustFlag(flags, F_N, true);
			flags = adjustFlag(flags, F_C, (res & ~0xffff) != 0);
			registers.reg[_F] = flags;

			tStates += 15;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xCF) == 0x42;
		}
	}
	
	private int get16bitRegister(int arg) {
		switch(arg) {
		case 0:
			return registers.getBC();
		case 1:
			return registers.getDE();
		case 2:
			return registers.getHL();
		case 3:
			return registers.reg[_SP];
		}
		throw new RuntimeException("Invalid 16 bit register request: " + arg);
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

	interface Handler {
		void handle(int instr);
	}
	
	interface LoadableHandler extends Handler {
		boolean willHandle(int instr);
	}
}
