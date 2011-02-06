package z80;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

public class Display extends JPanel
		implements SpectrumMemory.ScreenBufListener, KeyListener, InputDevice {

	private static final long serialVersionUID = -3960099300040985910L;
	private static int MAG = 3;
	private int intCount = 0;
	
	private Map<Integer, Integer> keyValues = new HashMap<Integer, Integer>();
	private static Map<Integer, Integer> keyEvents = new HashMap<Integer, Integer>();
	
	private int[] screen = new int[0x1B00];
	private static int NORM = 224;
	private static Color[] colors = new Color[] {
			new Color(0, 0, 0),
			new Color(0, 0, NORM),
			new Color(NORM, 0, 0),
			new Color(NORM, 0, NORM),
			new Color(0, NORM, 0),
			new Color(0, NORM, NORM),
			new Color(NORM, NORM, 0),
			new Color(NORM, NORM, NORM),
	};
	
	static {
		keyEvents.put(KeyEvent.VK_0, 0xeffe01);
		keyEvents.put(KeyEvent.VK_9, 0xeffe02);
		keyEvents.put(KeyEvent.VK_8, 0xeffe04);
		keyEvents.put(KeyEvent.VK_7, 0xeffe08);
		keyEvents.put(KeyEvent.VK_6, 0xeffe10);

		keyEvents.put(KeyEvent.VK_1, 0xf7fe01);
		keyEvents.put(KeyEvent.VK_2, 0xf7fe02);
		keyEvents.put(KeyEvent.VK_3, 0xf7fe04);
		keyEvents.put(KeyEvent.VK_4, 0xf7fe08);
		keyEvents.put(KeyEvent.VK_5, 0xf7fe10);

		keyEvents.put(KeyEvent.VK_Q, 0xfbfe01);
		keyEvents.put(KeyEvent.VK_W, 0xfbfe02);
		keyEvents.put(KeyEvent.VK_E, 0xfbfe04);
		keyEvents.put(KeyEvent.VK_R, 0xfbfe08);
		keyEvents.put(KeyEvent.VK_T, 0xfbfe10);

		keyEvents.put(KeyEvent.VK_P, 0xdffe01);
		keyEvents.put(KeyEvent.VK_O, 0xdffe02);
		keyEvents.put(KeyEvent.VK_I, 0xdffe04);
		keyEvents.put(KeyEvent.VK_U, 0xdffe08);
		keyEvents.put(KeyEvent.VK_Y, 0xdffe10);

		keyEvents.put(KeyEvent.VK_A, 0xfdfe01);
		keyEvents.put(KeyEvent.VK_S, 0xfdfe02);
		keyEvents.put(KeyEvent.VK_D, 0xfdfe04);
		keyEvents.put(KeyEvent.VK_F, 0xfdfe08);
		keyEvents.put(KeyEvent.VK_G, 0xfdfe10);

		keyEvents.put(KeyEvent.VK_ENTER, 0xbffe01);
		keyEvents.put(KeyEvent.VK_L, 0xbffe02);
		keyEvents.put(KeyEvent.VK_K, 0xbffe04);
		keyEvents.put(KeyEvent.VK_J, 0xbffe08);
		keyEvents.put(KeyEvent.VK_H, 0xbffe10);

		keyEvents.put(KeyEvent.VK_SHIFT, 0xfefe01);
		keyEvents.put(KeyEvent.VK_Z, 0xfefe02);
		keyEvents.put(KeyEvent.VK_X, 0xfefe04);
		keyEvents.put(KeyEvent.VK_C, 0xfefe08);
		keyEvents.put(KeyEvent.VK_V, 0xfefe10);

		keyEvents.put(KeyEvent.VK_CONTROL, 0x7ffe01);
		keyEvents.put(KeyEvent.VK_SLASH, 0x7ffe02);
		keyEvents.put(KeyEvent.VK_M, 0x7ffe04);
		keyEvents.put(KeyEvent.VK_N, 0x7ffe08);
		keyEvents.put(KeyEvent.VK_B, 0x7ffe10);
	}
	
	public Display() {
		keyValues.put(0xfefe, 0xff); // SHIFT - V
		keyValues.put(0xfdfe, 0xff); // A - G
		keyValues.put(0xfbfe, 0xff); // Q - T
		keyValues.put(0xf7fe, 0xff); // 1 - 5
		keyValues.put(0xeffe, 0xff); // 6 - 0
		keyValues.put(0xdffe, 0xff); // Y - P
		keyValues.put(0xbffe, 0xff); // H - ENTER
		keyValues.put(0x7ffe, 0xff); // B - SPACE
		
		Dimension d = new Dimension(256*MAG, 192*MAG);
		this.setSize(d);
		this.setMaximumSize(d);
		this.setMinimumSize(d);
		this.setPreferredSize(d);
	}
	
	public void interrupt() {
		intCount++;
		if(intCount == 50) {
			intCount = 0;
		}
		if(intCount == 0 || intCount == 25) {
			repaint();
		}
	}

	public void update(int addr, int val) {
		screen[addr & 0x3fff] = val;
		repaint();
	}
	
	@Override
	public void paint(Graphics g) {
		for(int i = 0; i < 0x300; i++) {
			int colorAttr = screen[i+0x1800];
			Color bg = colors[(colorAttr&0x38)>>3];
			if(intCount >= 25 && (colorAttr & 0x80) != 0) {
				bg = colors[intCount >= 25 ? (colorAttr&0x07) : (colorAttr&0x38)>>3];
			}
			
			g.setColor(bg);
			g.fillRect((i%32)*8*MAG, (i/32)*8*MAG, 8*MAG, 8*MAG);
		}
		
		for(int i = 0; i < 0x1800; i++) {
			int x = i % 32;
			int blk = i / 0x800;
			int pixrow = (i % 0x800) / 0x100;
			int row = (i / 32) % 8;
			int y = pixrow + row*8 + blk*64;
			int colorLoc = y / 8 * 32 + x;
			if(screen[i] != 0) {
				int colorAttr = screen[colorLoc+0x1800];
				Color fg = colors[colorAttr & 0x7];
				if(intCount >= 25 && (colorAttr & 0x80) != 0) {
					fg = colors[intCount < 25 ? (colorAttr&0x07) : (colorAttr&0x38)>>3];
				}
				g.setColor(fg);
				for(int j = 0; j < 8; j++) {
					if((screen[i] & (1<<j)) != 0) {
						g.fillRect((x*8 + (7-j))*MAG, y*MAG, MAG, MAG);
					}
				}
			}
		}
	}

	public void keyPressed(KeyEvent ke) {
		if(keyEvents.get(ke.getKeyCode()) != null) {
			int val = keyEvents.get(ke.getKeyCode());
			int port = val >> 8;
			int cval = keyValues.get(port);
			int bit = val & 0xff;
			cval &= ~bit;
			keyValues.put(port, cval);
		}
		System.out.println(ke.getKeyChar() + " down");
	}

	public void keyReleased(KeyEvent ke) {
		if(keyEvents.get(ke.getKeyCode()) != null) {
			int val = keyEvents.get(ke.getKeyCode());
			int port = val >> 8;
			int cval = keyValues.get(port);
			int bit = val & 0xff;
			cval |= bit;
			keyValues.put(port, cval);
		}
	}

	public int read(int addr) {
//		System.out.println("addr=" + Integer.toHexString(addr));
		return keyValues.get(addr);
	}

	public void keyTyped(KeyEvent ke) {	}
}
