package z80;

import static z80.Registers.*;
import static z80.Registers.F_H;
import static z80.Registers.F_N;
import static z80.Registers.F_PV;
import static z80.Registers.F_S;
import static z80.Registers.F_Z;
import static z80.Registers._A;
import static z80.Registers._B;
import static z80.Registers._C;
import static z80.Registers._F;
import static z80.Registers._I;
import static z80.Registers._IY;
import static z80.Registers._IX;
import static z80.Registers._PC;
import static z80.Registers._XAF;

import java.util.HashMap;
import java.util.Map;

import z80.Registers.IntMode;

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
	private InputDevice[] inputs = new InputDevice[256];

	private static Map<Integer, String> labels = new HashMap<Integer, String>();
	static {
//		labels.put(0x028e, "KEY-SCAN");
//		labels.put(0x0296, "KEY-LINE");
//		labels.put(0x029f, "KEY-3KEYS");
//		labels.put(0x02a1, "KEY-BITS");
//		labels.put(0x02ab, "KEY-DONE");
//		labels.put(0x02bf, "KEYBOARD");
//		labels.put(0x02c6, "K-ST-LOOP");
//		labels.put(0x02F1, "K-NEW");
//		labels.put(0x0308, "K-END");
//		labels.put(0x0333, "K-DECODE");
//		labels.put(0x0341, "K-E-LET");
//		labels.put(0x034a, "K-LOOK-UP");
//		labels.put(0x034f, "K-KLC-LET");
//		labels.put(0x0364, "K-TOKENS");
//		labels.put(0x0367, "K-DIGIT");
//		labels.put(0x0382, "K-8-&-9");
//		labels.put(0x10A8, "KEY-INPUT");
//		labels.put(0x10A8, "KEY-M&CL");
//		labels.put(0x111B, "KEY-DONE");
//		labels.put(0x0F81, "ADD-CHAR");
	}

	private boolean enableInt = false;

	private void initialiseHandlers(Handler[] array,
			LoadableHandler... handlers) {
		for (int i = 0; i < 256; i++) {
			for (LoadableHandler handler : handlers) {
				if (handler.willHandle(i)) {
					if (array[i] != null) {
						System.out.println("Warning: duplicate handler: " + Integer.toHexString(i));
					}
					array[i] = handler;
				}
			}
		}
	}

	public Cpu() {

		loadSimpleHandlers();
		initialiseHandlers(baseHandlers, new Handler_ADD(),
				new Handler_ADD_HL(), new Handler_AND(), new Handler_CP(),
				new Handler_DEC(), new Handler_EX_DE_HL(), new Handler_INC(),
				new Handler_INC_DEC_RR(), new Handler_JPCD(), new Handler_JR(),
				new Handler_LD(), new Handler_LD_N(), new Handler_LD_RR(),
				new Handler_LD_HL_NN(), new Handler_OR(), new Handler_OUT(),
				new Handler_POP(), new Handler_PUSH(), new Handler_RET_C(),
				new Handler_RST(), new Handler_SCF_CCF(), new Handler_SUB(),
				new Handler_XOR(), new ShiftHandler());

		initialiseHandlers(extended_CB, new Handler_CB_SRL(),
				new Handler_CB_RLC(), new Handler_CB_BIT(),
				new Handler_CB_SET(),
				new Handler_CB_RES());
		
		initialiseHandlers(extended_ED, new Handler_ED_LD_I_A(),
				new Handler_SBC_HL(), new Handler_LD_NN_RR(),
				new Handler_LDDR());

		initialiseHandlers(extended_FD, new Handler_LD_IY_R(),
				new Handler_LD_R_IY());

		Handler nullHandler = new NullHandler();
		for (int i = 0; i < 256; i++) {
			baseHandlers[i] = (baseHandlers[i] == null ? nullHandler
					: baseHandlers[i]);
			extended_CB[i] = (extended_CB[i] == null ? nullHandler
					: extended_CB[i]);
			extended_DD[i] = (extended_DD[i] == null ? nullHandler
					: extended_DD[i]);
			extended_ED[i] = (extended_ED[i] == null ? nullHandler
					: extended_ED[i]);
			extended_FD[i] = (extended_FD[i] == null ? nullHandler
					: extended_FD[i]);
		}
	}
	
	public void setKeyboard(InputDevice inputDevice) {
		inputs[0xfe] = inputDevice;
	}

	private void loadSimpleHandlers() {
		// NOP
		baseHandlers[0x00] = new Handler() {
			public void handle(int instr) {
				tStates += 4;
			}
		};
		// RLCA
		baseHandlers[0x07] = new Handler() {
			public void handle(int instr) {
				int bit7 = registers.reg[_A] & 0x80;
				registers.reg[_A] = ((registers.reg[_A] << 1) | (bit7>>7)) & 0xff;
				adjustFlag(F_H, false);
				adjustFlag(F_N, false);
				adjustFlag(F_C, bit7 == 0x80);
				tStates += 4;
			}
		};
		// RRCA
		baseHandlers[0x0F] = new Handler() {
			public void handle(int instr) {
				int bit0 = registers.reg[_A] & 0x01;
				registers.reg[_A] = (registers.reg[_A] >> 1) | (bit0 << 7);
				adjustFlag(F_H, false);
				adjustFlag(F_N, false);
				adjustFlag(F_C, bit0 == 0x01);
				tStates += 4;
			}
		};
		// DJNZ
		baseHandlers[0x10] = new Handler() {
			public void handle(int instr) {
				int dist = readByteOffset();
				registers.reg[_B] = (registers.reg[_B] - 1) & 0xff;
				if (registers.reg[_B] != 0) {
					registers.reg[_PC] = (registers.reg[_PC] + dist) & 0xffff;
					tStates += 13;
				} else {
					tStates += 8;
				}
			}
		};
		// RLA
		baseHandlers[0x17] = new Handler() {
			public void handle(int instr) {
				int bit7 = registers.reg[_A] & 0x80;
				registers.reg[_A] = ((registers.reg[_A] << 1) | registers.getFlag(F_C)) & 0xff;
				adjustFlag(F_H, false);
				adjustFlag(F_N, false);
				adjustFlag(F_C, bit7 == 0x80);
				tStates += 4;
			}
		};
		baseHandlers[0x1F] = new Handler() {
			public void handle(int instr) {
				int bit0 = registers.reg[_A] & 0x01;
				registers.reg[_A] = (registers.reg[_A] >> 1) | ((registers.getFlag(F_C) << 7));
				adjustFlag(F_H, false);
				adjustFlag(F_N, false);
				adjustFlag(F_C, bit0 == 0x01);
				tStates += 4;
			}
		};
		// EX AF,AF'
		baseHandlers[0x08] = new Handler() {
			public void handle(int instr) {
				int tmp = registers.getAF();
				registers.setAF(registers.reg[_XAF]);
				registers.reg[_XAF] = tmp;
				tStates += 4;
			}
		};
		// LD (DE),A
		baseHandlers[0x12] = new Handler() {
			public void handle(int instr) {
				memory.set8bit(registers.getDE(), registers.reg[_A]);
				tStates += 7;
			}
		};
		// LD (nn),HL
		baseHandlers[0x22] = new Handler() {
			public void handle(int instr) {
				memory.set16bit(readNextWord(), registers.getHL());
				tStates += 16;
			}
		};
		// CPL
		baseHandlers[0x2F] = new Handler() {
			public void handle(int instr) {
				registers.reg[_A] ^= 0xff;
				adjustFlag(F_H, true);
				adjustFlag(F_N, true);
				tStates += 4;
			}
		};
		// LD (nn),HL
		baseHandlers[0x32] = new Handler() {
			public void handle(int instr) {
				memory.set8bit(readNextWord(), registers.reg[_A]);
				tStates += 13;
			}
		};
		// LD A,(nn)
		baseHandlers[0x3A] = new Handler() {
			public void handle(int instr) {
				registers.reg[_A] = memory.get8bit(readNextWord());
				tStates += 13;
			}
		};
		// EXX
		baseHandlers[0xD9] = new Handler() {
			public void handle(int instr) {
				registers.exx();
				tStates += 4;
			}
		};
		// JP (HL)
		baseHandlers[0xE9] = new Handler() {
			public void handle(int instr) {
				registers.reg[_PC] = registers.getHL();
				tStates += 4;
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
				registers.setSP(registers.getHL());
				tStates += 6;
			}
		};
		// ADD A,N
		baseHandlers[0xc6] = new Handler() {
			public void handle(int instr) {
				int arg = readNextByte();
				set8bitAddFlags(registers.reg[_A], registers.reg[_A] + arg);
				registers.reg[_A] = (registers.reg[_A] + arg) & 0xff;
				tStates += 7;
			}
		};
		// RET
		baseHandlers[0xc9] = new Handler() {
			public void handle(int instr) {
				registers.reg[_PC] = pop();
				tStates += 10;
			}
		};
		// EX (SP),HL              
		baseHandlers[0xe3] = new Handler() {
			public void handle(int instr) {
				int tmp = registers.getHL();
				registers.setHL(memory.get16bit(registers.getSP()));
				memory.set16bit(registers.getSP(), tmp);
				tStates += 19;
			}
		};
		// LD A,(DE)              
		baseHandlers[0x1a] = new Handler() {
			public void handle(int instr) {
				registers.reg[_A] = memory.get8bit(registers.getDE());
				tStates += 7;
			}
		};

		// LD IX,nn
		extended_DD[0x21] = new Handler() {
			public void handle(int instr) {
				registers.reg[_IX] = readNextWord();
				tStates += 14;
			}
		};
		// ADD IX,BC
		extended_DD[0x09] = new Handler() {
			public void handle(int instr) {
				registers.reg[_IX] = add16bit(registers.reg[_IX], registers.getBC());
				tStates += 15;
			}
		};
		// JP (IX)
		extended_DD[0xE9] = new Handler() {
			public void handle(int instr) {
				registers.reg[_PC] = registers.reg[_IX];
				tStates += 8;
			}
		};

		// IM 1
		extended_ED[0x56] = new Handler() {
			public void handle(int instr) {
				registers.im = IntMode.IM1;
				tStates += 8;
			}
		};
		// IN A,(C)
		extended_ED[0x78] = new Handler() {
			public void handle(int instr) {
				if(inputs[registers.reg[_C]] != null) {
					registers.reg[_A] = inputs[registers.reg[_C]].read(registers.getBC());
				} else {
					registers.reg[_A] = 0;
				}
				tStates += 8;
			}
		};
		// NEG
		extended_ED[0x44] = new Handler() {
			public void handle(int instr) {
				int res = 0 - registers.reg[_A];
				int preA = registers.reg[_A];
				registers.reg[_A] = (res & 0xff);
				
				adjustFlag(F_S, (res & 0x80) != 0);
				adjustFlag(F_Z, registers.reg[_A] == 0);
				// adjustFlag(F_H, false);
				adjustFlag(F_PV, preA == 0x80);
				adjustFlag(F_N, true);
				adjustFlag(F_C, preA != 0x00);
				
				tStates += 8;
			}
		};
		// LD BC,(nn)
		extended_ED[0x4b] = new Handler() {
			public void handle(int instr) {
				registers.setBC(memory.get16bit(readNextWord()));
				tStates += 20;
			}
		};
		// LD DE,(nn)
		extended_ED[0x5b] = new Handler() {
			public void handle(int instr) {
				registers.setDE(memory.get16bit(readNextWord()));
				tStates += 20;
			}
		};
		// LD HL,(nn)
		extended_ED[0x6b] = new Handler() {
			public void handle(int instr) {
				registers.setHL(memory.get16bit(readNextWord()));
				tStates += 20;
			}
		};
		// LD SP,(nn)
		extended_ED[0x7b] = new Handler() {
			public void handle(int instr) {
				registers.setSP(memory.get16bit(readNextWord()));
				tStates += 20;
			}
		};

		// LD IY,NN
		extended_FD[0x21] = new Handler() {
			public void handle(int instr) {
				registers.reg[_IY] = readNextWord();
				tStates += 14;
			}
		};
		// DEC (IY+d)
		extended_FD[0x35] = new Handler() {
			public void handle(int instr) {
				int addr = registers.reg[_IY] + readNextByte();
				memory.set8bit(addr, memory.get8bit(addr) - 1);
				postDecrementFlagAdjust(memory.get8bit(addr));
				tStates += 23;
			}
		};
		// LD (IY+d),n
		extended_FD[0x36] = new Handler() {
			public void handle(int instr) {
				int addr = registers.reg[_IY] + readNextByte();
				memory.set8bit(addr, readNextByte());
				tStates += 19;
			}
		};
		// ADD A,(IY+d)
		extended_FD[0x86] = new Handler() {
			public void handle(int instr) {
				int addr = registers.reg[_IY] + readNextByte();
				int before = memory.get8bit(addr);
				int after = before + registers.reg[_A];
				registers.reg[_A] = (after & 0xff);
				set8bitAddFlags(before, after);
				tStates += 19;
			}
		};
		// SUB,(IY+d)
		extended_FD[0x96] = new Handler() {
			public void handle(int instr) {
				int addr = registers.reg[_IY] + readNextByte();
				int before = registers.reg[_A];
				int after = registers.reg[_A] - memory.get8bit(addr);
				registers.reg[_A] = (after & 0xff);
				set8bitSubFlags(before, after);
				tStates += 19;
			}
		};
		extended_FD[0xCB] = new Handler() {
			public void handle(int instr) {
				int addr = registers.reg[_IY] + readNextByte();
				handleCB(addr, readNextByte());
			}
		};
	}
	
	private int add16bit(int target, int arg) {
		int result = target + arg;
		adjustFlag(F_H, (target&0xfff)+(arg&0xfff) > 0xfff);
		adjustFlag(F_N, false);
		adjustFlag(F_C, (result & ~0xffff) != 0);
		return result & 0xffff;
	}

	protected void set8bitAddFlags(int before, int after) {
		int src = after - before;
		int flags = registers.reg[_F];
		flags = adjustFlag(flags, F_S, (after & 0x80) == 0x80);
		flags = adjustFlag(flags, F_Z, (after & 0xff) == 0);
		flags = adjustFlag(flags, F_H, ((before & 0x0f) + (src & 0x0f)) > 0x0f);
		flags = adjustFlag(flags, F_PV, (before & 0x80) == (src & 0x80)
				&& (before & 0x80) != (after & 0x80));
		flags = adjustFlag(flags, F_N, false);
		flags = adjustFlag(flags, F_C, (after & ~0xff) != 0);
		registers.reg[_F] = flags;
	}
	
	private void set8bitSubFlags(int before, int after) {
		int arg = before - after;
		adjustFlag(F_S, (after & 0x80) == 0x80);
		adjustFlag(F_Z, after == 0);
		adjustFlag(F_H, ((before & 0x0f) - (arg & 0x0f)) < 0);
		adjustFlag(F_PV, (before & 0x80) != (arg & 0x80)
				&& (before & 0x80) != (after & 0x80));
		adjustFlag(F_N, true);
		adjustFlag(F_C, (after & ~0xff) != 0);
	}

	private void handleCB(int addr, int instr) {
		if ((instr & 0xC7) == 0xC6) {
			int bit = (instr & 0x38) >> 3;
			memory.set8bit(addr, memory.get8bit(addr) | (1 << bit));
			tStates += 23;
		} else if ((instr & 0xC7) == 0x46) {
			int bit = (instr & 0x38) >> 3;
			adjustFlag(F_Z, (memory.get8bit(addr) & (1 << bit)) == 0);
			adjustFlag(F_H, true);
			adjustFlag(F_N, false);
			tStates += 20;
		} else if ((instr & 0xC7) == 0x86) {
			// RES
			int bit = (instr & 0x38) >> 3;
			memory.set8bit(addr, memory.get8bit(addr) & ~(1 << bit));
			tStates += 15;
		} else {
			throw new RuntimeException("Unhandled(CB): "
					+ Integer.toHexString(instr));
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

	private int readByteOffset() {
		int rv = readNextByte();
		if (rv >= 128) {
			rv = -256 + rv;
		}
		return rv;
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
//		if(registers.reg[_PC] == 0x02c2) {
//			System.out.println("de " + Integer.toHexString(
//					registers.getDE()) + " " + registers.isFlag(F_Z));
//		}
		
		if(labels.get(registers.reg[_PC]) != null) {
			System.out.println(labels.get(registers.reg[_PC]));
		}
		int instr = readNextByte();
		// System.out.println("i " + Integer.toHexString(instr));
		current[instr].handle(instr);
		if (enableInt) {
			enableInt = false;
			registers.iff1 = registers.iff2 = true;
		}
	}

	class NullHandler implements Handler {
		public void handle(int instr) {
			System.out.println("Unhandled: 0x"
					+ Integer.toString(memory.get8bit(registers.reg[_PC-1]), 16)
					+ " 0x" + Integer.toString(instr, 16)
					+ " @ 0x" + Integer.toHexString(registers.reg[_PC]));
			throw new RuntimeException("Unfinished CPU at " + tStates);
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

	class Handler_CB_BIT implements LoadableHandler {
		public void handle(int instr) {
			int idx = instr & 0x07;
			int bit = (instr & 0x38)>>3;
			int data;
			if(idx == 0x06) {
				data = memory.get8bit(registers.getHL());
				tStates += 12;
			} else {
				data = registers.reg[idx];
				tStates += 8;
			}
			int res = data & (1<<bit);
			adjustFlag(F_Z, res == 0);
			adjustFlag(F_H, true);
			adjustFlag(F_N, false);
		}

		public boolean willHandle(int instr) {
			return (instr & 0xC0) == 0x40;
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
			int val = pop();
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
			tStates += 10;
		}
	}

	class Handler_PUSH implements LoadableHandler {

		public boolean willHandle(int instr) {
			return (instr & 0xCF) == 0xC5;
		}

		public void handle(int instr) {
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
			push(val);
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
			int res = preHL + arg;
			registers.setHL(res & 0xffff);

			adjustFlag(F_H,
					((preHL & 0x0fff) + (arg & 0x0fff)) > 0x0fff);
			adjustFlag(F_N, false);
			adjustFlag(F_C, (res & ~0xffff) != 0);
			copy35Bits(registers.reg[_H]);

			tStates += 11;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xCF) == 0x09;
		}
	}

	class Handler_RET_C implements LoadableHandler {
		public void handle(int instr) {
			boolean test = false;
			switch ((instr & 0x38) >> 3) {
			case 0:
			case 1:
				test = registers.isFlag(F_Z);
				break;
			case 2:
			case 3:
				test = registers.isFlag(F_C);
				break;
			case 4:
			case 5:
				test = registers.isFlag(F_PV);
				break;
			case 6:
			case 7:
				test = registers.isFlag(F_S);
				break;
			}
			test = (instr & 0x08) == 0 ? !test : test;
			if (test) {
				registers.reg[_PC] = pop();
				tStates += 11;
			} else {
				tStates += 5;
			}
		}

		public boolean willHandle(int instr) {
			return (instr & 0xc7) == 0xc0;
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

			set8bitAddFlags(before, after);

			tStates += 4;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xf0) == 0x80;
		}
	}

	class Handler_SUB implements LoadableHandler {
		public void handle(int instr) {
			int src;
			if (instr == 0xd6) {
				src = readNextByte();
			} else {
				src = getRegisterValue(instr);
			}

			if ((instr & 0x08) == 0x08) {
				src += registers.getFlag(F_C);
			}

			int before = registers.reg[_A];
			int after = registers.reg[_A] - src;
			registers.reg[_A] = (after & 0xff);

			set8bitSubFlags(before, after);

			tStates += 4;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xf0) == 0x90 || instr == 0xd6;
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

			adjustFlag(F_S, (res & 0x80) == 0x80);
			adjustFlag(F_Z, res == 0);
			adjustFlag(F_H, (res & 0x0f) == 0);
			adjustFlag(F_PV, res == 0x80);
			adjustFlag(F_N, false);
			copy35Bits(res);
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

			postDecrementFlagAdjust(res);
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

			adjustFlag(F_S, (res & 0x80) == 0x80);
			adjustFlag(F_Z, res == 0);
			adjustFlag(F_H, false);
			adjustFlag(F_PV, Integer.bitCount(res) % 2 == 0);
			adjustFlag(F_N, false);
			adjustFlag(F_C, false);
			copy35Bits(res);

			tStates += 4;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xf8) == 0xa8 || instr == 0xEE;
		}
	}

	class Handler_OR implements LoadableHandler {
		public void handle(int instr) {
			int arg;
			if (instr == 0xF6) {
				arg = readNextByte();
				tStates += 3;
			} else {
				arg = getRegisterValue(instr & 0x07);
			}

			registers.reg[_A] = registers.reg[_A] | arg;
			int res = registers.reg[_A];

			int flags = registers.reg[_F];
			flags = adjustFlag(flags, F_S, (res & 0x80) == 0x80);
			flags = adjustFlag(flags, F_Z, res == 0);
			flags = adjustFlag(flags, F_H, false);
			// flags = adjustFlag(flags, F_PV, Integer.bitCount(res) % 2 == 0);
			flags = adjustFlag(flags, F_N, false);
			flags = adjustFlag(flags, F_C, false);
			registers.reg[_F] = flags;

			tStates += 4;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xf8) == 0xb0 || instr == 0xF6;
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

			adjustFlag(F_S, (res & 0x80) == 0x80);
			adjustFlag(F_Z, res == 0);
			adjustFlag(F_H, true);
			adjustFlag(F_PV, Integer.bitCount(res) % 2 == 0);
			adjustFlag(F_N, false);
			adjustFlag(F_C, false);
			copy35Bits(res);

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
			int calc = res - arg;

			adjustFlag(F_S, (calc & 0x80) == 0x80);
			adjustFlag(F_Z, res == arg);
			// TODO flags = adjustFlag(flags, F_H, false);
			adjustFlag(F_PV, (res&0x80) != (arg&0x80) && (res&0x80) != (calc&0x80));
			adjustFlag(F_N, true);
			adjustFlag(F_C, (calc & ~0xff) != 0);
			copy35Bits(arg);
			
			tStates += 4;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xf8) == 0xb8 || instr == 0xFE;
		}
	}
	
	private void copy35Bits(int arg) {
		adjustFlag(F_5, (arg & 0x20) != 0);
		adjustFlag(F_3, (arg & 0x08) != 0);
	}

	class Handler_RST implements LoadableHandler {
		public void handle(int instr) {
			push(registers.reg[_PC]);
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
				registers.setSP(val);
				break;
			}
			tStates += 10;
		}

		public boolean willHandle(int instr) {
			return ((instr & 0xCF) == 0x01);
		}
	}

	class Handler_JPCD implements LoadableHandler {
		public void handle(int instr) {
			boolean test;
			if(instr == 0xc3 || instr == 0xcd) {
				test = true;
			} else {
				test = testFlag((instr & 0x38) >> 3);
			}
			int addr = readNextWord();
			if (test) {
				if(((instr & 0xc7) == 0xc4) || instr == 0xcd) {
					push(registers.reg[_PC]);
				}
				registers.reg[_PC] = addr;
				tStates += 7;
			}
			tStates += 10;
		}

		public boolean willHandle(int instr) {
			return ((instr & 0xc7) == 0xc2) || ((instr & 0xc7) == 0xc4)
				|| instr == 0xc3 || instr == 0xcd;
		}
	}

	class Handler_JR implements LoadableHandler {
		public void handle(int instr) {
			boolean test = false;
			switch (instr) {
			case 0x18:
				test = true;
				break;
			case 0x20:
				test = !registers.isFlag(F_Z);
				break;
			case 0x28:
				test = registers.isFlag(F_Z);
				break;
			case 0x30:
				test = !registers.isFlag(F_C);
				break;
			case 0x38:
				test = registers.isFlag(F_C);
				break;
			}

			int dist = readNextByte();

			if (test) {
				if (dist >= 128) {
					dist = -(256 - dist);
				}
				registers.reg[_PC] = (registers.reg[_PC] + dist) & 0xffff;
				tStates += 5;
			}
			tStates += 7;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xE7) == 0x20 || instr == 0x18;
		}
	}

	class Handler_INC_DEC_RR implements LoadableHandler {
		public void handle(int instr) {
			int adj = (instr & 0x08) == 0 ? 1 : -1;
			switch ((instr & 0x30) >> 4) {
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
				registers.setSP(registers.getSP() + adj);
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
			if (outputs[addr] != null) {
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
			switch (instr) {
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
			return instr == 0xED || instr == 0xFD || instr == 0xDD
					|| instr == 0xCB;
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

	class Handler_LD_IY_R implements LoadableHandler {
		public void handle(int instr) {
			int addr = registers.reg[_IY] + readNextByte();
			memory.set8bit(addr, registers.reg[instr & 0x07]);
			tStates += 19;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xF8) == 0x70 && instr != 0x76;
		}
	}

	class Handler_LD_R_IY implements LoadableHandler {
		public void handle(int instr) {
			int addr = registers.reg[_IY] + readNextByte();
			registers.reg[(instr & 0x38) >> 3] = memory.get8bit(addr);
			tStates += 19;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xC7) == 0x46 && instr != 0x76;
		}
	}

	class Handler_CB_SRL implements LoadableHandler {
		public void handle(int instr) {
			int idx = instr & 0x07;
			if(idx == 6) {
				memory.set8bit(registers.getHL(), bit_SRL(memory.get8bit(registers.getHL())));
				tStates += 15;
			} else {
				registers.reg[idx] = bit_SRL(registers.reg[idx]);
				tStates += 8;
			}
		}

		public boolean willHandle(int instr) {
			return (instr & 0xF8) == 0x38;
		}
	}

	class Handler_CB_SET implements LoadableHandler {
		public void handle(int instr) {
			int bit = (instr & 0x38)>>3;
			int idx = (instr & 0x07);
			if(idx == 6) {
				memory.set8bit(registers.getHL(), memory.get8bit(registers.getHL()) | (1<<bit));
				tStates += 15;
			} else {
				registers.reg[idx] |= (1<<bit);
				tStates += 8;
			}
		}

		public boolean willHandle(int instr) {
			return (instr & 0xC0) == 0xC0;
		}
	}

	class Handler_CB_RES implements LoadableHandler {
		public void handle(int instr) {
			int bit = (instr & 0x38)>>3;
			int idx = (instr & 0x07);
			if(idx == 6) {
				memory.set8bit(registers.getHL(), memory.get8bit(registers.getHL()) & ~(1<<bit));
				tStates += 15;
			} else {
				registers.reg[idx] &= ~(1<<bit);
				tStates += 8;
			}
		}

		public boolean willHandle(int instr) {
			return (instr & 0xC0) == 0x80;
		}
	}

	class Handler_CB_RLC implements LoadableHandler {
		public void handle(int instr) {
			int idx = instr & 0x07;
			if(idx == 6) {
				memory.set8bit(registers.getHL(), bit_RLC(memory.get8bit(registers.getHL())));
				tStates += 15;
			} else {
				registers.reg[idx] = bit_RLC(registers.reg[idx]);
				tStates += 8;
			}
		}

		public boolean willHandle(int instr) {
			return (instr & 0xF8) == 0x00;
		}
	}

	class Handler_LDDR implements LoadableHandler {
		public void handle(int instr) {
			int dir = (instr & 0x08) == 0 ? 1 : -1;
			int data = memory.get8bit(registers.getHL()) + registers.reg[_A];
			memory.set8bit(registers.getDE(), memory.get8bit(registers.getHL()));
			registers.setHL(registers.getHL() + dir);
			registers.setDE(registers.getDE() + dir);
			registers.setBC(registers.getBC() - 1);
			if (registers.getBC() != 0) {
				tStates += 5;
				registers.reg[_PC] = (registers.reg[_PC] - 2) & 0xffff;
			}
			adjustFlag(F_H, false);
			adjustFlag(F_PV, registers.getBC() != 0);
			adjustFlag(F_N, false);
			adjustFlag(F_3, (data & 0x08) != 0);
			adjustFlag(F_5, (data & 0x01) != 0);
			tStates += 16;
		}

		public boolean willHandle(int instr) {
			return instr == 0xB8 || instr == 0xB0;
		}
	}

	class Handler_SBC_HL implements LoadableHandler {
		public void handle(int instr) {
			int preHL = registers.getHL();
			int arg = get16bitRegister((instr & 0x30) >> 4);
			arg += registers.getFlag(F_C);
			int res = preHL - arg;
			registers.setHL(res & 0xffff);

			adjustFlag(F_S, (registers.getHL() & 0x8000) == 0x8000);
			adjustFlag(F_Z, registers.getHL() == 0);
			adjustFlag(F_H, ((preHL & 0x0fff) < (arg & 0x0fff)));
			adjustFlag(F_PV, (preHL & 0x8000) != (arg & 0x8000)
					&& (preHL & 0x8000) != (res & 0x8000));
			adjustFlag(F_N, true);
			adjustFlag(F_C, (res & ~0xffff) != 0);
			copy35Bits(registers.reg[_H]);

			tStates += 15;
		}

		public boolean willHandle(int instr) {
			return (instr & 0xCF) == 0x42;
		}
	}

	private int get16bitRegister(int arg) {
		switch (arg) {
		case 0:
			return registers.getBC();
		case 1:
			return registers.getDE();
		case 2:
			return registers.getHL();
		case 3:
			return registers.getSP();
		}
		throw new RuntimeException("Invalid 16 bit register request: " + arg);
	}

	private void postDecrementFlagAdjust(int result) {
		adjustFlag(F_S, (result & 0x80) == 0x80);
		adjustFlag(F_Z, result == 0);
		adjustFlag(F_H, (result & 0x0f) == 0x0f);
		adjustFlag(F_PV, result == 0x7f);
		adjustFlag(F_N, true);
		copy35Bits(result);
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

	@Deprecated
	private int adjustFlag(int flags, int bit, boolean val) {
		if (val) {
			return flags | (1 << bit);
		} else {
			return flags &= ~(1 << bit);
		}
	}

	private void adjustFlag(int bit, boolean val) {
		if (val) {
			registers.reg[_F] |= (1 << bit);
		} else {
			registers.reg[_F] &= ~(1 << bit);
		}
	}
	
	private void push(int val) {
		registers.setSP(registers.getSP() - 2);
		memory.set16bit(registers.getSP(), val);
//		System.out.println("PUSH: "
//				+ Integer.toHexString(memory.get16bit(registers.getSP()))
//				+ " @ " + Integer.toHexString(registers.getSP()));
	}

	private int pop() {
		int val = memory.get16bit(registers.getSP());
		registers.setSP(registers.getSP() + 2);
//		System.out.println("POP: " + Integer.toHexString(val));
		return val;
	}
	
	private boolean testFlag(int option) {
		boolean test = false;
		switch(option) {
		case 0:
			test = !registers.isFlag(F_Z);
			break;
		case 1:
			test = registers.isFlag(F_Z);
			break;
		case 2:
			test = !registers.isFlag(F_C);
			break;
		case 3:
			test = registers.isFlag(F_C);
			break;
		case 4:
			test = !registers.isFlag(F_PV);
			break;
		case 5:
			test = registers.isFlag(F_PV);
			break;
		case 6:
			test = !registers.isFlag(F_S);
			break;
		case 7:
			test = registers.isFlag(F_S);
			break;
		}
		return test;
	}
	
	public int bit_SRL(int arg) {
		int rv = arg>>1;
		adjustFlag(F_S, false);
		adjustFlag(F_Z, rv == 0);
		adjustFlag(F_H, false);
		adjustFlag(F_PV, Integer.bitCount(rv) % 2 == 0);
		adjustFlag(F_N, false);
		adjustFlag(F_C, (arg & 0x01) == 1);
		return rv;
	}

	public int bit_RLC(int arg) {
		int rv = ((arg << 1) | (arg >> 7)) & 0xff;
		adjustFlag(F_S, (rv & 0x80) != 0);
		adjustFlag(F_Z, rv == 0);
		adjustFlag(F_H, false);
		adjustFlag(F_PV, Integer.bitCount(rv) % 2 == 0);
		adjustFlag(F_N, false);
		adjustFlag(F_C, (arg & 0x80) != 0);
		return rv;
	}

	public void executeToInterrupt() {
		long timeNow = System.currentTimeMillis();
		long nextInt = timeNow + (20 - timeNow % 20);
		
		long startT = getTStates();
		do {
			execute();
			if(getTStates() - startT >= 70000) {
				try {
					Thread.sleep(Math.max(0, nextInt - System.currentTimeMillis()));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} while(System.currentTimeMillis() < nextInt);
	}

	public void maskableInterrupt() {
		if( ! registers.iff1) {
			return;
		}
		switch(registers.im) {
		case IM1:
			registers.iff1 = registers.iff2 = false;
			push(registers.reg[_PC]);
			registers.reg[_PC] = 0x38;
			break;
		default:
			throw new UnsupportedOperationException("IM not supported");
		}
	}

	interface Handler {
		void handle(int instr);
	}

	interface LoadableHandler extends Handler {
		boolean willHandle(int instr);
	}
}
