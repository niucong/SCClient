package com.niucong.scclient.util;

import android.os.RemoteException;
import android.util.Base64;
import android.widget.Toast;

import com.gprinter.aidl.GpService;
import com.gprinter.command.EscCommand;
import com.gprinter.command.GpCom;
import com.gprinter.io.utils.GpUtils;
import com.niucong.scclient.App;
import com.niucong.scclient.db.SellRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

/**
 * Created by think on 2017/3/2.
 */

public class PrintUtil {

    /**
     * 打印小票
     *
     * @param mGpService
     * @param sRecords
     */
    public static void printStick(GpService mGpService, List<SellRecord> sRecords) {
        EscCommand esc = new EscCommand();
        esc.addPrintAndFeedLines((byte) 3);
        // 设置打印居中
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);

        // 设置为倍高倍宽
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);
        // 打印文字
        esc.addText("顺昌诊所\n");
        esc.addPrintAndLineFeed();

        // 取消倍高倍宽
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);// 设置打印左对齐

        int size = sRecords.size();
        int allPrice = 0;
        int payType = 0;
        for (int i = 0; i < size; i++) {
            SellRecord sr = sRecords.get(i);
            allPrice += sr.getPrice() * sr.getNumber();
            esc.addText((i + 1) + "\t" + sr.getName() + "\n");
            esc.addText(sr.getBarCode() + "\n" + sr.getNumber() + "*" + App.app.showPrice(sr.getPrice()) + "\t\t" +
                    App.app.showPrice(sr.getPrice() * sr.getNumber()) + "\n");
        }
        esc.addPrintAndLineFeed();

        long time = System.currentTimeMillis();
        sRecords.get(0).getId();
        esc.addText("订单号：" + time + "\n");
        esc.addText("销售时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time)) + "\n");
        esc.addText("销售种类：" + size + "种\n");
        esc.addText("销售金额：" + App.app.showPrice(allPrice) + "\n");

        esc.addPrintAndLineFeed();
        // 设置条码可识别字符位置在条码下方
        esc.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.BELOW);
        // 设置条码高度为60点
        esc.addSetBarcodeHeight((byte) 60);
        // 设置条码单元宽度为1点
        esc.addSetBarcodeWidth((byte) 2);
        // 打印Code128码
        esc.addCODE128("" + time);
        esc.addCODE128(esc.genCodeB("" + time));
        esc.addPrintAndLineFeed();
        esc.addText("\n\n");

        Vector<Byte> datas = esc.getCommand();
        // 发送数据
        Byte[] Bytes = datas.toArray(new Byte[datas.size()]);
        byte[] bytes = GpUtils.ByteTo_byte(Bytes);
        String str = Base64.encodeToString(bytes, Base64.DEFAULT);
        int rel;
        try {
            rel = mGpService.sendEscCommand(0, str);
            GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rel];
            if (r != GpCom.ERROR_CODE.SUCCESS) {
                Toast.makeText(App.app, GpCom.getErrorText(r), Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
