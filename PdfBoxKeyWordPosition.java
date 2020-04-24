package com.jzy.edu.cloud.common.pdf;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.tools.ant.types.resources.selectors.Date;

import com.alibaba.fastjson.JSONObject;
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
import java.util.regex.Pattern;


/**
 * 直接读取pdf文件
 * @author Administrator
 *
 */
public class PdfBoxKeyWordPosition extends PDFTextStripper {
	// the_pdf_dpi/72    200dpi= 2.7778  300dpi= 4.166    pdf默认dpi为72
    private final String the_pdf_check_num = 100;
    private final float dpi_xy = 2.7778;
    private final float dpi_x_offset = 2.5;
    private final float dpi_y_offset = 9.5;
    private final float width = 1.1;
    private final float 0.85;
	// 第几题
	private int countQuestions = 0;
	// PDF文件路径
	private String pdfPath;
	// PDF文件页面宽
	private int width;
	// PDF文件页面高
	private int high;
	// 坐标信息集合
	private List<JSONObject> list = new ArrayList<JSONObject>();
	// 当前页信息集合
	private List<int[]> pagelist = new ArrayList<int[]>();
	// 客观题集合
	private List<int[]> objsignlist = new ArrayList<int[]>();
	// 主观题集合
	private List<int[]> slist = new ArrayList<int[]>();
	private List<int[]> subsignlist = new ArrayList<int[]>();
	// 有参构造方法
	public PdfBoxKeyWordPosition(String pdfPath) throws IOException {
		super();
		super.setSortByPosition(true);
		this.pdfPath = pdfPath;
	}
	public String getPdfPath() {
		return pdfPath;
	}
	public void setPdfPath(String pdfPath) {
		this.pdfPath = pdfPath;
	}
	// 获取坐标信息
	public JSONObject getCoordinate() throws IOException {
		JSONObject jsonObject = new JSONObject();
		try {
			document = PDDocument.load(new File(pdfPath));
			int pages = document.getNumberOfPages();
			
			for (int i = 1; i <= pages; i++) {
				pagelist.clear();
				subsignlist.clear();
				super.setSortByPosition(true);
				super.setStartPage(i);
				super.setEndPage(i);
				Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
				super.writeText(document, dummy);
				for (int[] li : pagelist) {
					li[5] = i;
					if (list.size()<=li[4]-1) { //新的一题
						List<int[]> newList = new ArrayList<int[]>();
						newList.add(li);
						JSONObject object = new JSONObject();
						object.put("opts", newList);
						list.add(object);
						
					}else {//已经有的题里添加选项
						List<Object> box = new ArrayList<>();
						JSONObject object = list.get(li[4]-1);
						box.addAll(object.getJSONArray("opts"));
						box.add(li);
						object.put("opts", box);
						list.set(li[4]-1, object);
						
					}
				}
				for (int[] li : subsignlist) {
					li[5] = i;
				}
				for (int[] li : objsignlist) {
					li[5] = i;
				}
				slist.addAll(subsignlist);
			}
			
			// 客观题
			JSONObject objectives = new JSONObject();
			objectives.put("signAdd", objsignlist);
			
			objectives.put("boxs", list);
			jsonObject.put("objectives", objectives);
			// 页面信息
			JSONObject area = new JSONObject();
			area.put("pages", pages);
			area.put("width", width);
			area.put("high", high);
			jsonObject.put("area", area);
			// 主观题
			jsonObject.put("subjectives", slist);
			return jsonObject;
		} catch (Exception e) {
			e.printStackTrace();
			return jsonObject;
		} finally {
			if (document != null) {
				document.close();
			}
		}
	}

	// 获取坐标信息
	@Override
    protected void writeString(String string, List<TextPosition> textPositions) {
		// 正则 选项\[[A-Z]]   题号 \[[0-9]{1,3}]
    	String marchzimu = "[A-Z]";
    	String marchshuzi = "[0-9]";
    	
    	// 初始题号字符长度
    	int len = 2;
    	for (int i = 0; i < textPositions.size(); i++) {
    		TextPosition textPosition = textPositions.get(i);
			String str = textPosition.getUnicode();
			if ("❤".equals(str)) {
				int[] idx = getPosition(textPosition);
				objsignlist.add(idx);
			}
			// 识别主观题标志坐标
			if ("❥".equals(str)) {
				int[] idx = getPosition(textPosition);
				subsignlist.add(idx);
			}
    	}
    	width = (int) Math.ceil(textPositions.get(0).getPageWidth()*2.7778);
    	high = (int) Math.ceil(textPositions.get(0).getPageHeight()*2.7778);
    	// 识别客观题选项坐标
		for (int i = 0; i < textPositions.size(); i++) {
			// text得到pdf这一行中的汉字，同时下面有判断这一行字的长度，防止关键字在文中多次出现
//			String text = textPositions.toString().replaceAll("[^\u4E00-\u9FA5]", "");	
			TextPosition textPosition = textPositions.get(i);
			String str = textPosition.getUnicode();
			// 发现主观题标志结束
			if ("❥".equals(str)) {
				break;
			}
			
			if (str.equals("[")) {
				int countxx = 0;
				// 计算题号字符长度[1][10][100]
				if (countQuestions>=10) {
					len=3;
				}
				if (countQuestions>=100) {
					len=4;
				}
				for (int j = 0; j <= len; j++) {
					String s = "";
					try {
						s = textPositions.get(i + j).getUnicode();
					} catch (Exception e) {
						s = "";
					}
					if (Pattern.matches(marchzimu,s)||s.equals("[")||s.equals("]")) {
						countxx++;
					}
					if (Pattern.matches(marchshuzi,s)) {
						countQuestions++;
					}
					
				}
				// 选项位置
				if (countxx == 3) {
					int[] idx = getPosition(textPosition);
					pagelist.add(idx);
				}
			}
		}
	}
	//获取某个点的坐标
//	private int[] getPosition(TextPosition textPosition) {
//		int[] idx = new int[6];
//		// 需要进行一些调整 使得章盖在字体上
//		// X坐标 在这里加上了字体的长度，也可以直接 idx[0] = textPositions.get(i).getX()
//		// Y坐标 在这里减去的字体的长度，也可以直接 idx[1] = textPositions.get(i).getPageHeight()-textPositions.get(i).getY()
////		idx[1] = textPositions.get(i).getPageHeight()-textPositions.get(i).getY()-4*textPositions.get(i).getFontSize();
//		float X = textPosition.getX()+dpi_x_offset;
//		float Y = textPosition.getY()-dpi_y_offset;
//		idx[0] = (int) Math.ceil(X*dpi_xy);
//		idx[1] = (int) Math.ceil(Y*dpi_xy);
//		idx[2] = (int) Math.ceil(textPosition.getFontSize()*dpi_xy*width);
//		idx[3] = (int) Math.ceil(textPosition.getFontSize()*dpi_xy*hign);
//		idx[4] = countQuestions;
////		System.out.println("x=" + idx[0] + ",y=" + idx[1] + ",w=" + idx[2] + ",h=" + idx[3] + ",m=" + idx[4]);
//		return idx;
//	}
	//获取某个点的坐标
	private int[] getPosition(TextPosition textPosition) {
		int[] idx = new int[6];
		// 需要进行一些调整 使得章盖在字体上
		// X坐标 在这里加上了字体的长度，也可以直接 idx[0] = textPositions.get(i).getX()
		idx[0] = (int) (textPosition.getX()*2.7778);
		// Y坐标 在这里减去的字体的长度，也可以直接 idx[1] = textPositions.get(i).getPageHeight()-textPositions.get(i).getY()
//			idx[1] = textPositions.get(i).getPageHeight()-textPositions.get(i).getY()-4*textPositions.get(i).getFontSize();
		int X = (int) (textPosition.getX()+2.5);
		int Y = (int) (textPosition.getY()+9.5);
		idx[0] = (int) Math.ceil(X*2.7778);
		idx[1] = (int) Math.ceil(Y*2.7778);
		idx[2] = (int) Math.ceil(textPosition.getFontSize()*2.7778*1.1);
		idx[3] = (int) Math.ceil(textPosition.getFontSize()*2.7778*0.8);
		idx[4] = countQuestions;
//			System.out.println("x=" + idx[0] + ",y=" + idx[1] + ",w=" + idx[2] + ",h=" + idx[3] + ",m=" + idx[4]);
		return idx;
	}
	public static void main(String[] args) {
		try {
			String filePath = "F:\\soft\\apache-tomcat-8.5.20\\webapps\\home\\1205_dyg.pdf";
			PdfBoxKeyWordPosition pdfBoxKeyWordPosition = new PdfBoxKeyWordPosition(filePath);
			System.out.println(System.currentTimeMillis());
			JSONObject coordinate = pdfBoxKeyWordPosition.getCoordinate();
			System.out.println(System.currentTimeMillis());
			System.out.println(coordinate);
			

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