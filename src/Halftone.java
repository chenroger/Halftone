import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


public class Halftone {

	final static int COLOUR_CYAN = 0, COLOUR_MAGENTA = 1, COLOUR_YELLOW = 2, COLOUR_BLACK = 3;
	final static int MAX_CMYK = 255;
	final static double[] ROTATE = {Math.PI/12, Math.PI/4, 0, 5*Math.PI / 12};
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int gridSize;
		String infile = args[1];
		String outfile = args[2];

		try {
			gridSize = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			System.out.println("1st argument must be a number");
			return;
		}
		System.out.println(gridSize + " " + infile + " " + outfile);
		BufferedImage tmp = null;
		try {
			tmp = ImageIO.read(new File(infile));
		} catch (IOException e) {
			System.out.println("IO Exception has occurred");
			System.exit(0);
		}

		// Convert image with smaller dimensions
		tmp = scale(gridSize/2, tmp);

		try { // output the halftone
			ImageIO.write(makeHalftone(tmp, gridSize), "png", new File(outfile));
			//ImageIO.write(grayimage, "png", new File(outfile)); // test statement, please ignore
		} catch (IOException e) {
			System.out.println("IO Exception has occurred");
		}
	}

	private static BufferedImage scale(int gridSize, BufferedImage image) {
		int smallx, smally;
		if ((image.getWidth() % gridSize) < (gridSize / 2)) {
			smallx = (image.getWidth() / gridSize);
		} else {
			smallx = (image.getWidth() / gridSize) + 1;
		}

		if ((image.getHeight() % gridSize) < (gridSize / 2)) {
			smally = (image.getHeight() / gridSize);
		} else {
			smally = (image.getHeight() / gridSize) + 1;
		}

		BufferedImage scaledImage = null;
		if (image != null) {
			scaledImage = new BufferedImage(image.getWidth()/gridSize, image.getHeight()/gridSize, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = scaledImage.createGraphics();
			g2.drawImage(image, 0, 0, smallx, smally, null);
		}
		return scaledImage;
	}

	private static BufferedImage makeHalftone(BufferedImage input, int gridSize) {
		int imagex = input.getWidth() * gridSize;
		int imagey = input.getHeight() * gridSize;
		BufferedImage output = new BufferedImage(imagex, imagey, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = output.createGraphics();
		g2.setColor(new Color(255, 255, 255)); // set the colour of the background (white)
		g2.fillRect(0, 0, imagex*2, imagey*2);

		for (int c = 0; c < 4; c++) {
			BufferedImage rotatedImage;
			//			int max = input.getHeight() + input.getWidth();
			int max;
			if (input.getWidth() >= input.getHeight()) { // get the largest dimension and make the new canvas double that, for height and width
				max = input.getWidth()*2;
			} else {
				max = input.getHeight()*2;
			}
			rotatedImage = new BufferedImage(max, max, BufferedImage.TYPE_INT_RGB);
			Graphics2D rotatedG2 = rotatedImage.createGraphics();
			rotatedG2.setColor(new Color(255, 255, 255)); // set the colour of the background (white)
			rotatedG2.fillRect(0, 0, max, max);
			rotatedG2.rotate(ROTATE[c], max/2, 0);
			rotatedG2.drawImage(input, max/2, 0, null);
			switch (c) {
			case COLOUR_CYAN:
				g2.setColor(new Color(0, 255, 255, 96));
				break;
			case COLOUR_MAGENTA:
				g2.setColor(new Color(255, 0, 255 , 64));
				break;
			case COLOUR_YELLOW:
				g2.setColor(new Color(255, 255, 0, 32));
				break;
			default:
				g2.setColor(new Color(0, 0, 0, 16));
				break;
			}
			g2.rotate(-ROTATE[c]);
			for (int y = 0; y < max; y++) {
				int endy = gridSize * y + gridSize; // find the last y value of the grid
				for (int x = 0; x < max; x++) {
					if (rotatedImage.getRGB(x,y) != 0xffffff) {
						int value = rgb2cmyk(rotatedImage.getRGB(x, y))[c];
						if (value > 0) {
							int toneSize = (int) Math.round(gridSize * (value/255f) * 4 * 1.41421356237f);
							int endx = gridSize * x + gridSize - (input.getWidth()*gridSize); // find the last y value of the grid
							g2.fillOval(endx - (gridSize / 2) - (toneSize/2), endy - (gridSize / 2) - (toneSize / 2), toneSize, toneSize);
						}
					}
				}
			}
			g2.rotate(ROTATE[c]);
		}
		return output;
	}

	private static int[] rgb2cmyk(int rgb) {
		Color colour = new Color(rgb);
		int r = colour.getRed();
		int g = colour.getGreen();
		int b = colour.getBlue();
		//		System.out.println(r + " " + g + " " + b);
		int[] cmyk = {0, 0, 0, MAX_CMYK};
		if (r == 0 && g == 0 && b == 0){// black
			return cmyk;
		}
		cmyk[COLOUR_CYAN] = MAX_CMYK - r;
		cmyk[COLOUR_MAGENTA] = MAX_CMYK - g;
		cmyk[COLOUR_YELLOW] = MAX_CMYK - b;
		cmyk[COLOUR_BLACK] = MAX_CMYK;
		if (cmyk[COLOUR_CYAN] < cmyk[COLOUR_BLACK]) {
			cmyk[COLOUR_BLACK] = cmyk[COLOUR_CYAN];
		}
		if (cmyk[COLOUR_MAGENTA] < cmyk[COLOUR_BLACK]) {
			cmyk[COLOUR_BLACK] = cmyk[COLOUR_MAGENTA];
		}
		if (cmyk[COLOUR_YELLOW] < cmyk[COLOUR_BLACK]) {
			cmyk[COLOUR_BLACK] = cmyk[COLOUR_YELLOW];
		}

		cmyk[COLOUR_CYAN] = (int) ((cmyk[COLOUR_CYAN] - cmyk[COLOUR_BLACK]) * (1f-1f*(cmyk[COLOUR_BLACK]/MAX_CMYK)));
		cmyk[COLOUR_MAGENTA] = (int) ((cmyk[COLOUR_MAGENTA] - cmyk[COLOUR_BLACK]) *(1f-1f*(cmyk[COLOUR_BLACK]/MAX_CMYK)));
		cmyk[COLOUR_YELLOW] = (int) ((cmyk[COLOUR_YELLOW] - cmyk[COLOUR_BLACK])*(1f-1f*(cmyk[COLOUR_BLACK]/MAX_CMYK)));
		return cmyk;
	}
}
