package com.jzy.edu.cloud.common.pdf;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.text.TextPosition;

import com.itextpdf.text.log.SysoCounter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;



public class PdfBoxKeyWordPositionArea extends PDFTextStripper {
	// the_pdf_dpi/72    200dpi= 2.7778  300dpi= 4.166    pdf默认dpi为72
    private final String the_pdf_check_num = 100;
    private final float dpi_xy = 2.7778;
    private final float dpi_x_offset = 2.5;
    private final float dpi_y_offset = 9.5;
    private final float width = 1.1;
    private final float 0.85;
	// 关键字字符数组
	private char[] key;
	// PDF文件路径
	private String pdfPath;
	// 坐标信息集合
	private List<int[]> list = new ArrayList<int[]>();
	// 当前页信息集合
	private List<int[]> pagelist = new ArrayList<int[]>();
	// 有参构造方法
	public PdfBoxKeyWordPositionArea(String keyWords, String pdfPath) throws IOException {
		super();
		super.setSortByPosition(true);
		this.pdfPath = pdfPath;
		char[] key = new char[keyWords.length()];
		for (int i = 0; i < keyWords.length(); i++) {
			key[i] = keyWords.charAt(i);
		}
		this.key = key;
	}
	public char[] getKey() {
		return key;
	}
	public void setKey(char[] key) {
		this.key = key;
	}
	public String getPdfPath() {
		return pdfPath;
	}
	public void setPdfPath(String pdfPath) {
		this.pdfPath = pdfPath;
	}
	// 获取坐标信息
	public List<int[]> getCoordinate() throws IOException {
		try {
			document = PDDocument.load(new File(pdfPath));
			int pages = document.getNumberOfPages();
			for (int i = 1; i <= pages; i++) {
				pagelist.clear();
				super.setSortByPosition(true);
				super.setStartPage(i);
				super.setEndPage(i);
				Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
				super.writeText(document, dummy);
				for (int[] li : pagelist) {
					li[4] = i;
				}
				list.addAll(pagelist);
			}
			return list;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (document != null) {
				document.close();
			}
		}
		return list;
	}
	
	// 获取坐标信息
		public List<int[]> getCoordinateArea() throws IOException {
			try {
				document = PDDocument.load(new File(pdfPath));
				int pages = document.getNumberOfPages();
				for (int i = 1; i <= pages; i++) {
					pagelist.clear();
					super.setSortByPosition(true);
					super.setStartPage(i);
					super.setEndPage(i);
					Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
					super.writeText(document, dummy);
					for (int[] li : pagelist) {
						li[4] = i;
						li[7] = pages;
//						li[4] = String.valueOf(i);
					}
					list.addAll(pagelist);
				}
				return list;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (document != null) {
					document.close();
				}
			}
			return list;
		}

	// 获取坐标信息
	@Override
    protected void writeString(String string, List<TextPosition> textPositions) {
		for (int i = 0; i < textPositions.size(); i++) {
			// text得到pdf这一行中的汉字，同时下面有判断这一行字的长度，防止关键字在文中多次出现
			String text = textPositions.toString().replaceAll("[^\u4E00-\u9FA5]", "");				
			String str = textPositions.get(i).getUnicode();
//			String text = textPositions.toString();
//			System.out.println(str);
			if (str.equals(key[0] + "")&&text.length()<Integer.valueOf(the_pdf_check_num)) {
				int count = 0;
				for (int j = 0; j < key.length; j++) {
					String s = "";
					try {
						s = textPositions.get(i + j).getUnicode();
					} catch (Exception e) {
						s = "";
					}
					if (s.equals(key[j] + "")) {
						count++;
					}
				}
				if (count == key.length) {
					int[] idx = new int[8];
					// 需要进行一些调整 使得章盖在字体上
					// X坐标 在这里加上了字体的长度，也可以直接 idx[0] = textPositions.get(i).getX()
					float X = textPositions.get(i).getX()+dpi_x_offset;
					float Y = textPositions.get(i).getY()-dpi_y_offset;
					idx[0] = (int) Math.ceil(X*dpi_xy);
					idx[1] = (int) Math.ceil(Y*dpi_xy);
					idx[2] = (int) Math.ceil(textPositions.get(i).getFontSize()*dpi_xy*width);
					idx[3] = (int) Math.ceil(textPositions.get(i).getFontSize()*dpi_xy*hign);
					idx[5] = (int) Math.ceil(textPositions.get(i).getPageWidth()*dpi_xy);
					idx[6] = (int) Math.ceil(textPositions.get(i).getPageHeight()*dpi_xy);
					pagelist.add(idx);
				}
			}
		}
	}
	public static void main(String[] args) {
		try {
			String filePath = "F:\\doc\\java\\getPDFElementLocations\\getPDFElementLocations\\828_dyg.pdf";
			PdfBoxKeyWordPositionArea pdfBoxKeyWordPosition = new PdfBoxKeyWordPositionArea("**",filePath);
			List<int[]> coordinate = pdfBoxKeyWordPosition.getCoordinate();
			System.out.println(coordinate);
			System.out.println(coordinate.size());
//			PdfBoxKeyWordPosition pdfBoxKeyWordPosition1 = new PdfBoxKeyWordPosition("&*",filePath);
//			List<float[]> coordinate1 = pdfBoxKeyWordPosition1.getCoordinate();
//			System.out.println(coordinate1);
//			PdfBoxKeyWordPosition pdfBoxKeyWordPosition2 = new PdfBoxKeyWordPosition("**",filePath);
//			List<float[]> coordinate2 = pdfBoxKeyWordPosition2.getCoordinate();
//			System.out.println(coordinate2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e);
		}
	}
}