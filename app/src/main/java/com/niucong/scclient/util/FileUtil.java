package com.niucong.scclient.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by think on 2016/10/31.
 */

public class FileUtil {

    public static SimpleDateFormat ymdhms = new SimpleDateFormat("yyyyMMddHHmm");

    /**
     * 获取sd卡的路径
     *
     * @return
     */
    public static String getSdcardDir() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }

    /**
     * 导出数据
     */
    public boolean copyDBToSDcrad(Context c, String SDCardPath) {
        String DATABASE_NAME = "shunchang";
        String oldPath = c.getDatabasePath(DATABASE_NAME).getPath();
        String newPath = SDCardPath + DATABASE_NAME;
        Log.i("mainactivity", "copyDBToSDcrad oldPath=" + oldPath);
        Log.i("mainactivity", "copyDBToSDcrad newPath=" + newPath);
//        File proFile = new File(newPath);
//        if (proFile.exists()) {
//            File myFile = new File(newPath + ymdhms.format(new Date(proFile.lastModified())));
//            proFile.renameTo(myFile);
//        }
        return copyFile(oldPath, newPath + ymdhms.format(new Date()));
    }

    /**
     * 导入数据
     */
    public boolean copySDcradToDB(Context c, String SDCardPath) {
        String DATABASE_NAME = "shunchang";
        String oldPath = SDCardPath + DATABASE_NAME;// "file:///android_asset/"
        String newPath = c.getDatabasePath(DATABASE_NAME).getPath();
        Log.i("mainactivity", "copySDcradToDB oldPath=" + oldPath);
        Log.i("mainactivity", "copySDcradToDB newPath=" + newPath);
//        return copyAssetsFile(c, DATABASE_NAME, newPath);
        return copyFile(oldPath, newPath);
//        File proFile = new File(oldPath);
//        File myFile = new File(oldPath + ymdhms.format(new Date()));
//        return proFile.renameTo(myFile);
    }

    /**
     * 复制单个文件
     *
     * @param oldFileName             String 原文件路径
     * @param newPath                 String 复制后路径
     * @return boolean
     *  
     */
    public static boolean copyAssetsFile(Context c, String oldFileName, String newPath) {
        try {
            int byteread = 0;
            File newfile = new File(newPath);
            if (!newfile.exists()) {
                newfile.createNewFile();
            }
            InputStream inStream = c.getAssets().open(oldFileName);
            FileOutputStream fs = new FileOutputStream(newPath);
            byte[] buffer = new byte[1444];
            while ((byteread = inStream.read(buffer)) != -1) {
                fs.write(buffer, 0, byteread);
            }
            inStream.close();
            Log.i("mainactivity", "复制文件成功");
            return true;
        } catch (Exception e) {
            Log.i("mainactivity", "复制文件操作出错");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 复制单个文件
     *  
     *
     * @param oldPath              String 原文件路径
     * @param newPath              String 复制后路径
     * @return boolean
     *  
     */
    public static boolean copyFile(String oldPath, String newPath) {
        try {
            int byteread = 0;
            File oldfile = new File(oldPath);
            File newfile = new File(newPath);
            if (!newfile.exists()) {
                newfile.createNewFile();
            }
            if (oldfile.exists()) { // 文件存在时
                InputStream inStream = new FileInputStream(oldPath); // 读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                while ((byteread = inStream.read(buffer)) != -1) {
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
                Log.i("mainactivity", "复制文件成功");
                return true;
            } else {
                Log.i("mainactivity", "文件不存在");
            }
            return false;
        } catch (Exception e) {
            Log.i("mainactivity", "复制文件操作出错");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Activity 6.0运行权限设置
     *
     * @param context
     * @param activity
     * @param permission 权限  Manifest.permission.
     * @param type
     */
    public static boolean setPermission(Context context, Activity activity, String permission,
                                        int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager
                    .PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{permission}, type);
                return true;
            }
        }
        return false;
    }

    /**
     * 生成xlsx格式的表格，生成xls格式的表格只需把HSSF替换成XSSF
     *
     * @param workbook
     * @param sheetNum
     * @param sheetTitle
     * @param headers
     * @param result
     * @throws Exception
     */
    public static void exportExcel(HSSFWorkbook workbook, int sheetNum, String sheetTitle,
                                   String[] headers, List<List<String>> result) throws Exception {
// 第一步，创建一个webbook，对应一个Excel以xsl为扩展名文件
        HSSFSheet sheet = workbook.createSheet();
        workbook.setSheetName(sheetNum, sheetTitle);
//设置列宽度大小
//        sheet.setDefaultColumnWidth((short) 20);
//第二步， 生成表格第一行的样式和字体
//        HSSFCellStyle style = workbook.createCellStyle();
// 设置这些样式
//        style.setFillForegroundColor(XSSFColor.PALE_BLUE.index);
//        style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
//        style.setBorderBottom(XSSFCellStyle.BORDER_THIN);
//        style.setBorderLeft(XSSFCellStyle.BORDER_THIN);
//        style.setBorderRight(XSSFCellStyle.BORDER_THIN);
//        style.setBorderTop(XSSFCellStyle.BORDER_THIN);
//        style.setAlignment(XSSFCellStyle.ALIGN_CENTER);
// 生成一个字体
//        HSSFFont font = workbook.createFont();
//        font.setColor(XSSFColor.BLACK.index);
//设置字体所在的行高度
//        font.setFontHeightInPoints((short) 20);
//        font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
// 把字体应用到当前的样式
//        style.setFont(font);
// 指定当单元格内容显示不下时自动换行
//        style.setWrapText(true);
// 产生表格标题行
        HSSFRow row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            HSSFCell cell = row.createCell((short) i);
//            cell.setCellStyle(style);
            HSSFRichTextString text = new HSSFRichTextString(headers[i]);
            cell.setCellValue(text.toString());
        }
// 第三步：遍历集合数据，产生数据行，开始插入数据
        if (result != null) {
            int index = 1;
            for (List<String> m : result) {
                row = sheet.createRow(index);
                int cellIndex = 0;
                for (String str : m) {
                    HSSFCell cell = row.createCell((int) cellIndex);
                    cell.setCellValue(str.toString());
                    cellIndex++;
                }
                index++;
            }
        }
    }

}
