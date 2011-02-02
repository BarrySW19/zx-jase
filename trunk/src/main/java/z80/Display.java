package z80;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

public class Display extends JPanel implements SpectrumMemory.ScreenBufListener {

	private static final long serialVersionUID = -3960099300040985910L;
	
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
	
	public Display() {
		Dimension d = new Dimension(256, 192);
		this.setSize(d);
		this.setMaximumSize(d);
		this.setMinimumSize(d);
		this.setPreferredSize(d);
	}

	public void update(int addr, int val) {
		System.out.println("SCR: 0x" + Integer.toHexString(addr)
				+ " 0x" + Integer.toHexString(val));
		screen[addr & 0x3fff] = val;
		repaint();
	}
	
	@Override
	public void paint(Graphics g) {
		for(int i = 0; i < 0x300; i++) {
			g.setColor(colors[(screen[i+0x1800] & 0x38)>>3]);
			g.fillRect((i%32)*8, (i/32)*8, 8, 8);
		}
		
		for(int i = 0; i < 0x1800; i++) {
			int x = i % 32;
			int blk = i / 0x800;
			int pixrow = (i % 0x800) / 0x100;
			int row = (i / 32) % 8;
			int y = pixrow + row*8 + blk*64;
			int colorLoc = y / 8 * 32 + x;
			if(screen[i] != 0) {
				g.setColor(colors[screen[colorLoc+0x1800] & 0x7]);
				for(int j = 0; j < 8; j++) {
					if((screen[i] & (1<<j)) != 0) {
						g.drawLine(x*8 + (7-j), y, x*8 + (7-j), y);
					}
				}
			}
		}
	}
}
