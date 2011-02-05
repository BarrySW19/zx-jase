package z80;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

public class Display extends JPanel implements SpectrumMemory.ScreenBufListener {

	private static final long serialVersionUID = -3960099300040985910L;
	private static int MAG = 3;
	private int intCount = 0;
	
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
}
