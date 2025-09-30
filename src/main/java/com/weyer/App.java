package com.weyer;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class App {
    public static final int QUAL_SCALE = 1;
    public static final int ROW_RES = 10 * QUAL_SCALE;
    public static final int COL_RES = 20 * QUAL_SCALE;
    public static final int BINS = 5;
    public static final ArrayList<Color> BASE_COLORS = createBaseColList();

    private static ArrayList<Color> createBaseColList() {
        ArrayList<Color> list = new ArrayList<Color>();
        list.add(new Color(255, 0, 0));
        list.add(new Color(0, 255, 0));
        list.add(new Color(0, 0, 255));
        list.add(new Color(255, 255, 0));
        return list;
    }

    private static class Pic extends Frame {
        Color[][] colors;

        public Pic(Color[][] colors) {
            this.colors = colors;
            setVisible(true);
            setSize(colors[0].length, colors.length);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
        }

        public void paint(Graphics g) {
            BufferedImage img = new BufferedImage(colors[0].length, colors.length, 2);
            for (int i = 0; i < colors.length; i++) {
                for (int j = 0; j < colors[0].length; j++) {
                    img.setRGB(j, i, colors[i][j].getRGB());
                }
            }
            g.drawImage(img, 0, 0, null);
        }
    }

    public static void main(String[] args) {
        File dir = new File("images");
        ArrayList<String> rows = new ArrayList<String>();
        for (File f : dir.listFiles()) {
            Color[][] pic = superSample(f);
            Color[][] samp = sample(f);
            pic = reduce(pic, BASE_COLORS);
            samp = reduce(samp, BASE_COLORS);
            // new Pic(pic);
            String data = toCategoricalData(samp);
            rows.add(data);
        }

        try {
            File output = new File("output.csv");
            if (output.createNewFile()) {
                System.out.println("File created: " + output.getName());
            } else {
                System.out.println("File already exists.");
            }

            FileWriter writer = new FileWriter(output);
            writer.write(colorWiseHeader());
            for (String s : rows) {
                writer.write(s);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String pixelWiseHeader() {
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < ROW_RES; i++) {
            for (int j = 0; j < COL_RES; j++) {
                header.append(((i * COL_RES) + j + 1) + ",");
            }
        }
        header.deleteCharAt(header.length() - 1);
        return header.toString();
    }

    private static String colorWiseHeader() {
        return "Predominant,Red,Green,Blue,Yellow\n";
    }

    private static String toPixelWiseData(Color[][] sample) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ROW_RES; i++) {
            for (int j = 0; j < COL_RES; j++) {
                sb.append(toColInd(sample[i][j]) + ",");
            }
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString() + "\n";
    }

    private static String toGrayData(Color[][] sample) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ROW_RES; i++) {
            for (int j = 0; j < COL_RES; j++) {
                sb.append(sample[i][j].getRed() + ",");
            }
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString() + "\n";
    }

    private static String toColorWiseData(Color[][] sample) {
        int[] counts = getCounts(sample);
        return toLabel(counts) + "," + counts[0] + "," + counts[1] + "," + counts[2] + "," + counts[3] + "\n";
    }

    private static String toCategoricalData(Color[][] sample) {
        int[] counts = getCounts(sample);
        int maxVal = sample.length * sample[0].length;
        return toLabel(counts) + "," + bin(counts[0], BINS, maxVal) + "," + bin(counts[1], BINS, maxVal) + ","
                + bin(counts[2], BINS, maxVal) + "," + bin(counts[3], BINS, maxVal) + "\n";
    }

    private static int bin(int freq, int bins, int maxVal) {
        int[] cutoffs = new int[bins - 1];
        double cut = (double) maxVal / 2.0;
        for (int i = bins - 2; i >= 0; i--) {
            cutoffs[i] = (int) cut;
            cut = cut / 2.0;
        }
        return findInCuts(cutoffs, freq);
    }

    private static int findInCuts(int[] cutoffs, int toBin) {
        for (int i = 0; i < cutoffs.length; i++) {
            if (toBin < cutoffs[i]) {
                return i;
            }
        }
        return cutoffs.length;
    }

    private static int[] getCounts(Color[][] sample) {
        int[] counts = new int[4];
        for (int i = 0; i < ROW_RES; i++) {
            for (int j = 0; j < COL_RES; j++) {
                int colInd = toColInd(sample[i][j]);
                counts[colInd]++;
            }
        }
        return counts;
    }

    private static String toLabel(int[] counts) {
        int max1 = 0;
        int max2 = 0;
        for (int i = 1; i < counts.length; i++) {
            if (counts[i] > counts[max1]) {
                max1 = i;
            }
        }
        if (max1 == 0)
            max2 = 1;
        for (int i = 0; i < counts.length; i++) {
            if (i == max1)
                continue;
            if (counts[i] > counts[max2]) {
                max2 = i;
            }
        }
        if (max1 > max2) {
            int temp = max1;
            max1 = max2;
            max2 = temp;
        }
        String label = "";
        label = label + toSymb(max1);
        label = label + toSymb(max2);
        return label;
    }

    private static char toSymb(int ind) {
        switch (ind) {
            case 0:
                return 'R';
            case 1:
                return 'G';
            case 2:
                return 'B';
            default:
                return 'Y';
        }
    }

    private static int toColInd(Color col) {
        int red = BASE_COLORS.get(0).getRGB();
        int green = BASE_COLORS.get(1).getRGB();
        int blue = BASE_COLORS.get(2).getRGB();
        if (col.getRGB() == red) {
            return 0;
        }
        if (col.getRGB() == green) {
            return 1;
        }
        if (col.getRGB() == blue) {
            return 2;
        }
        return 3;
    }

    private static Color[][] grayscale(Color[][] colors) {
        for (int i = 0; i < colors.length; i++) {
            for (int j = 0; j < colors[0].length; j++) {
                colors[i][j] = grayscale(colors[i][j]);
            }
        }
        return colors;
    }

    private static Color grayscale(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int gray = (int) (.2126 * (double) r + .7152 * (double) g + .0722 * (double) b);
        return new Color(gray, gray, gray);
    }

    private static Color[][] sample(File f) {
        BufferedImage buffImg;
        try {
            buffImg = ImageIO.read(f);
        } catch (IOException e) {
            buffImg = null;
            e.printStackTrace();
        }
        int width = buffImg.getWidth();
        int height = buffImg.getHeight();
        int widthInc = width / COL_RES;
        int heightInc = height / ROW_RES;
        Color rgb;
        Color[][] colors = new Color[ROW_RES][COL_RES];
        for (int i = 0; i < ROW_RES; i++) {
            for (int j = 0; j < COL_RES; j++) {
                rgb = new Color(buffImg.getRGB(j * widthInc, i * heightInc));
                colors[i][j] = rgb;
            }
        }
        return colors;
    }

    private static Color[][] superSample(File f) {
        BufferedImage buffImg;
        try {
            buffImg = ImageIO.read(f);
        } catch (IOException e) {
            buffImg = null;
            e.printStackTrace();
        }
        int width = buffImg.getWidth();
        int height = buffImg.getHeight();
        int widthInc = 1;
        int heightInc = 1;
        Color rgb;
        Color[][] colors = new Color[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                rgb = new Color(buffImg.getRGB(j * widthInc, i * heightInc));
                colors[i][j] = rgb;
            }
        }
        return colors;
    }

    private static Color[][] reduce(Color[][] colors, ArrayList<Color> colList) {
        for (int i = 0; i < colors.length; i++) {
            for (int j = 0; j < colors[0].length; j++) {
                Color rgb = reduce(colors[i][j], colList);
                colors[i][j] = rgb;
            }
        }
        return colors;
    }

    private static Color reduce(Color toReduce, ArrayList<Color> colList) {
        Color closest = null;
        double closestDistance = 0;
        double dist = 0;
        for (Color c : colList) {
            if (closest == null) {
                closest = c;
                closestDistance = colDist(toReduce, c);
                continue;
            }
            dist = colDist(toReduce, c);
            if (dist < closestDistance) {
                closest = c;
                closestDistance = dist;
            }
        }
        return closest;
    }

    private static double colDist(Color c1, Color c2) {
        int r1 = c1.getRed();
        int g1 = c1.getGreen();
        int b1 = c1.getBlue();
        int r2 = c2.getRed();
        int g2 = c2.getGreen();
        int b2 = c2.getBlue();
        return Math.sqrt(Math.pow((r1 - r2), 2.0) + Math.pow((g1 - g2), 2.0) + Math.pow((b1 - b2), 2.0));
    }
}
