package z80;

public class Registers {
	
	static enum IntMode { IM0, IM1, IM2 };
	
	int[] reg = new int[20];
	boolean iff1, iff2;
	IntMode im = IntMode.IM0;
	
	static int F_S = 7;
	static int F_Z = 6;
	static int F_5 = 5;
	static int F_H = 4;
	static int F_3 = 3;
	static int F_PV = 2;
	static int F_N = 1;
	static int F_C = 0;
	
	static int _B = 0;
	static int _C = 1;
	static int _D = 2;
	static int _E = 3;
	static int _H = 4;
	static int _L = 5;
	// 6 unused
	static int _A = 7;
	
	static int _F = 8;
	
	static int _IX = 9;
	static int _IY = 10;
	static int _I = 11;
	static int _R = 12;
	
	static int _XAF = 13;
	static int _XBC = 14;
	static int _XDE = 15;
	static int _XHL = 16;
	
	static int _S = 17;
	private static int _SP = 18;
	static int _PC = 19;
	
	public void exx() {
		int v = getBC();
		setBC(reg[_XBC]);
		reg[_XBC] = v;
		
		v = getDE();
		setDE(reg[_XDE]);
		reg[_XDE] = v;
		
		v = getHL();
		setHL(reg[_XHL]);
		reg[_XHL] = v;
	}
	
	public int getFlag(int flag) {
		return (reg[_F] & (1<<flag))>>flag;
	}
	
	public boolean isFlag(int flag) {
		return (reg[_F] & (1<<flag)) != 0;
	}
	
	public int getAF() {
		return reg[_F] << 8 | reg[_A];
	}
	
	public void setAF(int val) {
		reg[_F] = (val & 0xff00) >> 8;
		reg[_A] = (val & 0xff);
	}
	
	public int getBC() {
		return reg[_B] << 8 | reg[_C];
	}
	
	public void setBC(int val) {
		reg[_B] = (val & 0xff00) >> 8;
		reg[_C] = (val & 0xff);
	}
	
	public int getDE() {
		return reg[_D] << 8 | reg[_E];
	}
	
	public void setDE(int val) {
		reg[_D] = (val & 0xff00) >> 8;
		reg[_E] = (val & 0xff);
	}
	
	public int getHL() {
		return reg[_H] << 8 | reg[_L];
	}
	
	public void setHL(int val) {
		reg[_H] = (val & 0xff00) >> 8;
		reg[_L] = (val & 0xff);
	}
	
	public void setSP(int val) {
		reg[_SP] = (val & 0xffff);
	}
	
	public int getSP() {
		return reg[_SP];
	}
	
	public void incPC() {
		reg[_PC] = (0xffff & (reg[_PC] + 1));
	}
	
	public void intPC(int dist) {
		reg[_PC] = (0xffff & (reg[_PC] + dist));
	}

	public void setCarry(int result) {
		if(result > 255 || result < 0) {
			reg[_F] |= 1<<F_C;
		} else {
			reg[_F] &= ~(1<<F_C);
		}
	}
}
